# Phase 7 Implementation Report

Date: 2026-11-19

## Outcome

Daily AI Assistant is now a session-authenticated multi-user application. Every profile,
interest list, bookmark, reading-history row, learning-progress row and daily-task row is
scoped to the authenticated user. The existing single-profile owner and all existing data were
preserved by an additive Flyway migration with no default password.

No deployment, GitHub push, database reset or secret commit was performed.

## Architecture delivered

- Spring Security filter chain with server-side sessions in an HttpOnly cookie.
- BCrypt cost-12 password hashing, backend password policy and confirmation validation.
- Registration, login, logout, current-user and CSRF endpoints.
- Generic invalid-credential response and in-memory login throttling.
- CSRF tokens required for cookie-authenticated mutations; CSRF exempt only for stateless
  admin-key requests.
- Role-based admin protection (`ROLE_ADMIN`) plus the preserved `X-Admin-Key` mechanism.
- Owner resolution from `SecurityContextHolder`; the old first-user cache was removed.
- Anonymous-safe public article discovery, with reading history recorded only when signed in.
- React login/register screens, protected routing, reload session restoration, expiry handling
  and Profile logout.
- Capacitor WebView cookie handling for the HTTPS API origin.

Detailed decisions: `AUTHENTICATION_ARCHITECTURE.md`.

## Database

V9 adds nullable `password_hash`, `role` (default `USER`), `enabled` (default true) and
`password_changed_at` to `users`. Existing owner id 1 remains the owner of its rows. The account
is unprovisioned until an operator supplies `BOOTSTRAP_EMAIL` + `BOOTSTRAP_PASSWORD`; bootstrap
never creates an account and never overwrites an existing password.

See `DATABASE_MIGRATION_REPORT.md`.

## Main files added

```
backend/src/main/java/com/dailyai/assistant/
  config/LegacyAccountBootstrap.java
  controller/AuthController.java
  dto/AuthUserDto.java, LoginRequest.java, RegisterRequest.java
  entity/Role.java
  exception/ConflictException.java, TooManyRequestsException.java
  security/AdminKeyAuthenticationFilter.java, AppUserDetailsService.java,
           AppUserPrincipal.java, LoginAttemptService.java,
           RestAccessDeniedHandler.java, RestAuthenticationEntryPoint.java,
           SecurityConfig.java
  service/AuthService.java
backend/src/main/resources/db/migration/V9__add_authentication.sql
backend/src/test/java/.../AuthApiIntegrationTest.java
backend/src/test/java/.../migration/FlywayMigrationTest.java
backend/src/test/java/.../migration/LegacyAccountBootstrapTest.java
backend/src/test/resources/application-test.yml
frontend/src/auth/AuthContext.jsx, ProtectedRoute.jsx
frontend/src/pages/LoginPage.jsx, RegisterPage.jsx
frontend/scripts/auth-e2e.mjs
AUTHENTICATION_ARCHITECTURE.md, DATABASE_MIGRATION_REPORT.md,
API_SECURITY_REPORT.md, MULTI_USER_TEST_REPORT.md,
ANDROID_AUTH_TEST_REPORT.md, PHASE_7_IMPLEMENTATION_REPORT.md
```

## Main files modified

```
backend/pom.xml                              Spring Security + security-test
backend/src/main/java/.../User.java          password hash, role, enabled, password change time
backend/src/main/java/.../CurrentUserService.java
backend/src/main/java/.../BookmarkService.java, ArticleService.java
backend/src/main/java/.../BookmarkRepository.java, UserRepository.java
backend/src/main/java/.../AdminController.java, GlobalExceptionHandler.java
backend/src/main/java/.../config/AppProperties.java, WebConfig.java
backend/src/main/resources/application.yml   session/cookie/auth settings
docker-compose.yml, .env.example             auth/session/bootstrap variables
frontend/src/App.jsx, main.jsx               protected routing + AuthProvider
frontend/src/api/client.js, api/api.js       credentials, CSRF, auth API
frontend/src/pages/ProfilePage.jsx           logout
frontend/src/pages/DiscoverPage.jsx          admin-only refresh control
frontend/src/styles.css                      auth screens
frontend/scripts/e2e-smoke.mjs               auth bootstrap + stale-selector fixes
frontend/package.json                        test:auth script
frontend/android/.../MainActivity.java       WebView cookie handling
README.md, API_DOCUMENTATION.md              authentication/API docs
```

## Verification results

| Check | Result |
|---|---|
| Backend `mvn -B clean test` | **75 tests, 0 failures/errors/skips** |
| Auth API tests | 9/9 (registration, login, logout, CSRF, isolation, admin) |
| Flyway migration test | 9/9 versions successful; preserved owner |
| Bootstrap test | BCrypt hash + ADMIN role; one user only |
| Frontend production build | Pass, 103 modules |
| Auth Playwright suite | **10/10** |
| Existing feature Playwright suite | **15/15** |
| Docker backend/frontend image builds | Pass |
| Backend container against isolated DB | Healthy, Flyway applied |
| Real-data V9 clone | Owner + 4 history + 1 learning + 4 task rows preserved |
| Android `assembleDebug` | BUILD SUCCESSFUL |
| Plaintext password values in test DB | 0 |
| Existing database modified | No (remained at V8) |

The real-data validation used a dump/clone, not the live database. The live database still has
the original owner and counts.

## Operational notes

- Set `BOOTSTRAP_EMAIL`, `BOOTSTRAP_PASSWORD` and optionally `BOOTSTRAP_ROLE=ADMIN`, then start
  the backend once to activate the preserved profile. Remove the password variable after use.
- Production browser: `AUTH_COOKIE_SECURE=true`, `AUTH_COOKIE_SAME_SITE=lax`.
- Production Capacitor: HTTPS API, `AUTH_COOKIE_SECURE=true`,
  `AUTH_COOKIE_SAME_SITE=none`, and `https://localhost` in `CORS_ALLOWED_ORIGINS`.
- The current Compose volume's database password did not match the current `.env` value in this
  workspace. Verification therefore used an isolated role/database. Reconcile `DB_PASSWORD` with
  the actual role password; do **not** reset the volume to work around it.
- Existing scheduler/admin key behavior remains. Normal users cannot call admin endpoints.

## Remaining limitations

- No Android emulator/device: APK login/session runtime is not verified.
- Session timeout by elapsed time was not simulated; logout/invalid-session behavior was.
- Login throttling is in-memory and per-instance.
- No email verification, password reset/change, MFA, remember-me, account deletion or audit log.
- Admin role assignment is environment/bootstrap or database administration only; there is no
  admin-user-management UI.
- CSRF uses a raw SPA header token; BREACH-specific masking was not added.
- Docker Compose full-stack startup against the original volume was not possible because of the
  pre-existing password drift described above. Images build and the backend runs against an
  isolated database.

See `MULTI_USER_TEST_REPORT.md`, `API_SECURITY_REPORT.md` and
`ANDROID_AUTH_TEST_REPORT.md` for evidence and detailed gaps.
