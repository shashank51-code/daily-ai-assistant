# API Security Report

Date: 2026-11-19

## Endpoint authorization matrix

| Endpoint group | Authentication | Authorization / behavior |
|---|---|---|
| `GET /api/news/**`, `GET /api/categories/**`, `GET /api/tools/**` | No | Public read-only discovery |
| `GET /api/auth/csrf` | No | Returns session CSRF token |
| `POST /api/auth/register` | No | Validation, duplicate email 409, auto-login |
| `POST /api/auth/login` | No | Generic 401, throttled 429 |
| `POST /api/auth/logout` | Session (CSRF required) | Invalidates session + expires cookie |
| `GET /api/auth/me` | Session | Current safe user DTO |
| `GET /api/dashboard` | Session | Owner-scoped stats and recommendations |
| `GET/PUT /api/profile`, `PUT /api/profile/preferences`, `GET /api/profile/history` | Session | Only the authenticated user's profile/history |
| `/api/bookmarks/**` | Session | List/add/delete only rows with the authenticated `user_id` |
| `/api/learning/**` | Session | Roadmap read is shared; progress/task rows are owner-scoped |
| `/api/admin/**` | `ROLE_ADMIN` or valid `X-Admin-Key` | Normal users receive 403; missing/wrong credentials 401 |

Unauthenticated API requests receive a JSON `401`; forbidden requests and CSRF failures receive
a JSON `403`. Public article detail remains readable, but history is recorded only when a
session exists.

## Ownership enforcement

`CurrentUserService` resolves the owner from `SecurityContextHolder` on every request. It has no
shared cache. The previous `findFirstByOrderByIdAsc()` behavior was removed. Existing services
already used owner-scoped repository methods; the remaining public paths were made
anonymous-safe:

- `BookmarkService.bookmarkedIds` / `isBookmarked` return empty/false for anonymous readers.
- `BookmarkService.count` uses `countByUserId`.
- `ArticleService.recordRead` writes only for an authenticated user.
- `BookmarkRepository.findByIdAndUserId` protects `DELETE /api/bookmarks/{id}`.
- Learning progress and daily-task repositories always filter by `user_id`.

A client cannot supply an owner id on any personal endpoint. Changing ids in paths or request
bodies cannot cross the session's ownership boundary; cross-user DELETE returns 404.

## CSRF and CORS

- CSRF is enabled for all cookie-authenticated state-changing requests.
- The SPA obtains the raw token from `GET /api/auth/csrf` and sends `X-XSRF-TOKEN`.
- CSRF is skipped only for `/api/admin/**` requests that carry an `X-Admin-Key` header; those
  requests are not cookie-authenticated. A logged-in ADMIN account still needs CSRF.
- CORS uses explicit allowed origins from `CORS_ALLOWED_ORIGINS` and
  `allowCredentials(true)`. Wildcard origins are not used.
- Allowed headers include `Content-Type`, `Authorization`, `X-XSRF-TOKEN` and `X-Admin-Key`.

## Session and password controls

| Control | Result |
|---|---|
| Password storage | BCrypt cost 12; no plaintext or reversible storage |
| Password exposure | Not in DTOs, logs or error messages |
| Validation | 10–72 chars, upper/lower/digit/symbol; confirmation match |
| Login errors | Generic “Invalid email or password” |
| Login throttling | 5 failures / 15 min per email+address; 429 |
| Session fixation | Session id changed on login/register |
| Cookie | HttpOnly, Path=/, SameSite/Secure configurable |
| Logout | Session invalidated, context cleared, cookie expired |
| Expiry | 8h default; missing/expired session returns 401 |
| Admin auth | Role or constant-time compared admin key; key never logged |

## Admin protection

The old controller-level optional key check was removed. `AdminKeyAuthenticationFilter` maps a
valid configured key to `ROLE_ADMIN`; Spring Security then enforces `hasRole("ADMIN")`. A blank
configured key is never accepted. The Discover refresh button is shown only to admin accounts,
and normal users receive 403 if they call the endpoint directly. `GET /api/admin/status` is
admin-only as well.

## Security test evidence

- Successful registration, duplicate case-insensitive email, invalid email, weak password.
- Successful login, generic invalid-credential response, logout, session restoration.
- CSRF missing on a cookie request → 403.
- Unauthenticated profile/bookmarks/learning/dashboard → 401; public news → 200.
- User A vs User B bookmarks, history, learning progress and daily tasks.
- User B deleting User A's bookmark by id → 404.
- Admin endpoint: normal user 403; valid key 200; wrong key 401.
- Password records: 60-character `$2a$` BCrypt hashes; zero plaintext test passwords.

## Remaining security limitations

- Login throttling and the attempt map are per-instance; use a shared store in a multi-instance
  deployment.
- Client IP uses the direct socket; trusted proxy/forwarded-header handling is not configured.
- Email verification, password reset/change, MFA, remember-me, account deletion and audit
  logging are not implemented.
- The admin key is a shared secret with no rotation UI; keep it in runtime environment variables.
- `SameSite=None` requires HTTPS and a WebView that accepts the trusted API cookies; Android
  runtime verification is blocked by the absence of an emulator/device (see Android report).
- `AUTH_COOKIE_SECURE` defaults to false for local HTTP. Production must set it to true.
