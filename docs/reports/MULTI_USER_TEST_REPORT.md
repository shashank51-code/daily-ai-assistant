# Multi-User Test Report

Date: 2026-11-19

## Test environments

| Suite | Environment |
|---|---|
| Backend 75 tests | H2 in PostgreSQL mode, isolated test profile, Flyway disabled for API tests |
| Flyway + bootstrap | H2 PostgreSQL mode with Flyway enabled from empty schema |
| Real-data migration | Disposable clone of the live PostgreSQL database |
| Browser auth/regression | Rebuilt backend image + disposable PostgreSQL database + Vite preview + installed Chrome |
| Android | Gradle `assembleDebug` only; no emulator/device |

No existing user data was used as disposable test data. The live database was only dumped; all
writes occurred in separate test/clone databases.

## Backend results

```
mvn -B clean test
Tests run: 75, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

`AuthApiIntegrationTest` (9 tests) verifies:

| Requirement | Result |
|---|---|
| Successful registration + auto-login | Pass |
| Duplicate email, including different case | 409 |
| Invalid email | 400 with field details |
| Weak password | 400 with field details |
| Successful login / generic invalid login | Pass / 401 |
| Logout invalidates session and expires cookie | 204, `Max-Age=0` |
| Session persists across requests | `/api/auth/me` returns the same user |
| Session fixation | Session id changes between CSRF request and successful login |
| Unauthenticated profile/bookmarks/learning/dashboard | 401 |
| CSRF missing on cookie mutation | 403 |
| User A bookmark invisible to User B | Pass |
| User B deletes User A's bookmark by id | 404 |
| User A name change does not alter User B | Pass |
| Topic progress and daily tasks isolated | Pass |
| Reading history recorded only for signed-in reader | Pass |
| Admin by normal user / valid key / wrong key | 403 / 200 / 401 |
| Repeated failed logins | 401 ×5, then 429 |

`AdminControllerTest` (3 tests) verifies the admin-key filter grants `ROLE_ADMIN` only for a
valid, non-blank configured key. `FlywayMigrationTest` verifies all nine migrations and the
unprovisioned preserved owner. `LegacyAccountBootstrapTest` verifies env-only activation with a
BCrypt hash and no second account.

## Real database API checks

The disposable E2E database contained independent registered users. After both browser runs:

| Check | Result |
|---|---|
| Password hashes | All registered users: 60-char `$2a$` BCrypt; legacy profile null |
| Plaintext password values in `users.password_hash` | 0 |
| Alice bookmarks | 1 |
| Bob bookmarks (same run) | 0 |
| Profile isolation | Bob's profile contained Bob only |
| Second user's Saved screen | “Nothing saved yet” |
| Cross-user DELETE by id | 404 (backend test) |

## Browser results

### Authentication suite — 10/10

`frontend/scripts/auth-e2e.mjs`, 390×844 Chrome:

1. Unauthenticated visit redirects to login.
2. Unknown credentials show a generic error.
3. Registration creates and signs in the user.
4. Session survives a full page reload.
5. Authenticated bookmark is stored for the owner.
6. Logout invalidates the browser session.
7. Login restores the saved bookmark after logout.
8. Second user sees an empty private Saved list.
9. Second user's profile does not contain the first user.
10. No unexpected console errors.

### Existing feature regression — 15/15

`frontend/scripts/e2e-smoke.mjs` now authenticates first, then verifies:

- Five-tab navigation and dashboard.
- Bookmark add/remove.
- Theme persistence.
- Discover news search and tool details.
- Learning topic status update/restore.
- Seven-day/28-task daily plan toggle.
- Article detail explanation.
- Profile name save/restore.
- Not-found route.
- No uncaught page exceptions or unexpected console errors.

Two stale assertions in the older script were corrected: the profile Save selector now uses
`exact: true`, and the article assertion checks “Quick summary” instead of the
description-dependent “What happened?” section. No application assertion was weakened.

## Migration verification

Applying V9 to a clone of the real database preserved:

- Owner id 1, name, email, preferences.
- 0 bookmarks, 4 reading-history rows, 1 learning-progress row, 4 daily-task rows.
- No password hash (no default password) and no second account.

The original database remained at V8 and was not modified; Flyway will apply V9 on the next
normal start.

## Limitations

- Session expiry by elapsed wall-clock time was not simulated in a long-running test. Logout
  invalidation, missing-session 401 behavior and the configured 8-hour timeout are verified by
  code/configuration and request tests.
- The login throttle is in-memory and per-instance; only the single-instance behavior was tested.
- Android login/session persistence could not be exercised because no emulator or device is
  available. See `ANDROID_AUTH_TEST_REPORT.md`.
- CSRF and session behavior were tested over HTTP locally; production requires
  `AUTH_COOKIE_SECURE=true`.
