-- Phase 7: authentication and multi-user ownership.
--
-- This migration is additive and keeps every existing row and owner id intact.
-- The pre-authentication profile (id 1) stays the owner of its bookmarks, history,
-- learning progress and daily-task progress. Its password_hash remains NULL until
-- an operator explicitly provisions it through the environment-only bootstrap
-- (BOOTSTRAP_EMAIL / BOOTSTRAP_PASSWORD). No default password is created here.

alter table users add column password_hash varchar(100);
alter table users add column role varchar(20) not null default 'USER';
alter table users add column enabled boolean not null default true;
alter table users add column password_changed_at timestamp with time zone;

-- Registration normalizes every email to lower case before inserting, so the
-- existing unique constraint (uk_users_email) is sufficient for case-insensitive
-- uniqueness. No functional index is added, which keeps the migration portable
-- across PostgreSQL and the H2 test database.
