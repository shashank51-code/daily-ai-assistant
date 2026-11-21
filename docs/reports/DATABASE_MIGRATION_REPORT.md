# Database Migration Report

Date: 2026-11-19

## Existing data inspected before migration

The live PostgreSQL database contained one pre-authentication owner:

| Field | Value |
|---|---|
| User id | `1` |
| Name | `Shashank` |
| Email | `fresher@dailyai.local` |
| Preferences | `JAVA_BACKEND` |
| Password hash | none (no password column existed) |

Owned records in the existing database:

| Table | Rows owned by user 1 | Foreign key |
|---|---:|---|
| `bookmarks` | 0 | `user_id → users(id) ON DELETE CASCADE` |
| `reading_history` | 4 | `user_id → users(id) ON DELETE CASCADE` |
| `learning_progress` | 1 | `user_id → users(id) ON DELETE CASCADE` |
| `daily_task_progress` | 4 | `user_id → users(id) ON DELETE CASCADE` |

The schema already scoped every personal table by `user_id`; no ownership columns needed to be
added or backfilled. Flyway history was at V8. Articles, categories, topics, tools and daily
tasks are shared reference/content data and remain global.

## Migration V9 (additive)

`backend/src/main/resources/db/migration/V9__add_authentication.sql`:

```sql
alter table users add column password_hash varchar(100);
alter table users add column role varchar(20) not null default 'USER';
alter table users add column enabled boolean not null default true;
alter table users add column password_changed_at timestamp with time zone;
```

Properties:

- Existing rows are not updated or deleted; owner id 1 remains owner of every existing child
  row.
- `password_hash` is nullable, so the preserved profile starts **unprovisioned**. No default,
  shared or public password is created by the migration.
- `role` defaults to `USER`; `enabled` defaults to `true`.
- No foreign keys are changed and no table is recreated.
- Email uniqueness uses the existing `uk_users_email` constraint. Registration normalizes every
  email to lower case before insert, so case-variant accounts cannot be created through the API.
  A functional index was deliberately avoided because it is not portable to the H2 migration
  test.

## Existing-account claim strategy

An operator can activate the preserved profile with environment variables only:

```bash
BOOTSTRAP_EMAIL=fresher@dailyai.local
BOOTSTRAP_PASSWORD=<strong password supplied outside source control>
BOOTSTRAP_ROLE=ADMIN
```

`LegacyAccountBootstrap`:

- Runs only when both email and password are non-blank.
- Requires the email to match an **existing** user; it never creates an account.
- Sets a BCrypt hash only when the existing `password_hash` is null.
- Never overwrites an existing password.
- Never logs the email/password or prints the credentials.
- Rejects a password that violates the application policy.

If the variables are absent, existing data is preserved but inaccessible until provisioning;
new users can still register normally. This is an additive, reversible identity choice: the
owner id and email are not reassigned.

## Verification on a clone of the real database

The original database was **not reset or modified**. To test real data safely:

1. `pg_dump` of the live database into a disposable clone.
2. A dedicated migration role was given ownership of the clone's public tables/sequences.
3. The current backend image started against the clone, letting Flyway apply V9 automatically.
4. The clone was queried before/after.

Result:

| Check | Result |
|---|---|
| Flyway versions 1–9 | All successful; V9 `add authentication` applied |
| Owner row | id 1, same name/email/preferences; `password_hash` null |
| Role / enabled | `USER` / `true` |
| Bookmarks | 0 (unchanged) |
| Reading history | 4 (unchanged) |
| Learning progress | 1 (unchanged) |
| Daily-task progress | 4 (unchanged) |
| Second account created | None |

The H2 `FlywayMigrationTest` also runs all nine migrations from an empty database and asserts
that the seeded owner remains at id 1 with no password. `LegacyAccountBootstrapTest` verifies
that environment bootstrap sets a BCrypt hash and `ADMIN` role without creating a second user.

## Deployment note

The original database remains at Flyway V8 in this workspace. On the next normal backend start,
Flyway applies V9 automatically. Configure `BOOTSTRAP_*` only when the owner is ready to claim
the preserved profile. Do not run the migration with a default password, and do not manually
drop/recreate the database.

If a rollback is required, take a backup first; V9 only adds nullable/defaulted columns, but a
safe reverse migration must be authored and reviewed rather than applied casually to a live
database.
