# Authentication Architecture

Date: 2026-11-19

## Decision: server-side sessions, not JWT

Daily AI Assistant now uses Spring Security **server-side sessions** delivered in an HttpOnly
cookie (`DAILYAI_SESSION`). This was chosen over JWT/refresh tokens because:

- The primary client is a same-site React app; an HttpOnly session cookie requires no token
  storage in JavaScript and is immune to token theft via `localStorage`.
- Logout, account disablement and session expiry are immediate server-side decisions. There is
  no JWT revocation problem to solve.
- The application already reads and writes PostgreSQL on every personal request, so a session
  lookup adds no meaningful architectural cost.
- A JWT design would still need a refresh-token store, rotation and revocation to support
  logout; that is more moving parts without a demonstrated benefit here.

No long-lived credential is stored in `localStorage`. The session cookie is HttpOnly. The
frontend keeps only a short-lived CSRF token in memory.

### Browser and Capacitor compatibility

The browser calls the API same-site (Vite proxy or same-origin Nginx), so `SameSite=Lax` is the
default. A Capacitor release build runs its bundled assets at `https://localhost` and calls a
separate HTTPS API origin; that is cross-site. Production Android deployments must therefore
set:

```bash
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=none
```

`MainActivity` enables WebView cookie handling (including cross-site cookies for the trusted
API origin) and flushes the cookie jar. Cookies remain HttpOnly/Secure; no authentication or
CSRF check is disabled. A plain-HTTP emulator build cannot use `SameSite=None` cookies; testing
authenticated Android flows requires an HTTPS tunnel or a same-origin deployment.

## Request flow

1. `GET /api/auth/csrf` creates a session if needed and returns a CSRF token.
2. `POST /api/auth/register` or `/login` validates credentials, rotates the session id, stores a
   `SecurityContext` in the session and returns the safe user DTO.
3. The browser sends the HttpOnly session cookie automatically (`withCredentials`).
4. Mutating requests also send `X-XSRF-TOKEN`, read from the CSRF endpoint and held in memory.
5. `GET /api/auth/me` restores the user after a page reload.
6. `POST /api/auth/logout` invalidates the session and expires the cookie.

## Components

| Component | Responsibility |
|---|---|
| `SecurityConfig` | Filter chain, URL rules, CSRF, CORS, session fixation, password encoder |
| `AppUserDetailsService` | Loads a user by normalized email; refuses accounts without a password |
| `AppUserPrincipal` | Carries the database user id and `ROLE_USER`/`ROLE_ADMIN` |
| `CurrentUserService` | Resolves the owner from `SecurityContextHolder`; no cached/shared user |
| `AuthController` | CSRF, register, login, logout, me |
| `AuthService` | Email normalization, duplicate checks, BCrypt hashing |
| `LoginAttemptService` | In-memory failure throttling |
| `AdminKeyAuthenticationFilter` | Maps a valid `X-Admin-Key` to `ROLE_ADMIN` |
| `LegacyAccountBootstrap` | Environment-only activation of the preserved single profile |

## Password security

- BCrypt with cost 12 (`BCryptPasswordEncoder(12)`).
- Password requirements: 10–72 characters, at least one lower-case letter, upper-case letter,
  digit and non-whitespace symbol. The same policy is enforced for the bootstrap password.
- BCrypt's 72-byte input limit is respected by the 72-character validation bound.
- Password hashes are stored only in `users.password_hash`, are never included in a DTO, and
  are never logged. Test verification found zero plaintext password values in the database.
- Registration compares `password` and `confirmPassword`; the confirmation is never persisted.
- Login failures use one generic message, “Invalid email or password”, for unknown email, wrong
  password and disabled accounts.

## Session protections

| Control | Implementation |
|---|---|
| Cookie theft/XSS | HttpOnly cookie; no token in localStorage |
| Transport | `AUTH_COOKIE_SECURE=true` for HTTPS production |
| CSRF | Session CSRF token + `X-XSRF-TOKEN`; same-site cookie as defense in depth |
| Session fixation | `ChangeSessionIdAuthenticationStrategy` on register/login |
| Logout | Session invalidated; context cleared; cookie expired with `Max-Age=0` |
| Expiry | `SESSION_TIMEOUT` (default 8h); expired/missing session returns JSON 401 |
| Cookie scope | `Path=/`, HttpOnly, configurable SameSite/Secure |
| Session fixation/rotation | New session id on authentication, existing attributes retained |

`/api/admin/**` skips CSRF only when an `X-Admin-Key` header is present (a stateless custom
header). A cookie-authenticated admin account still needs a CSRF token.

## Login throttling

`LoginAttemptService` permits 5 failed attempts per normalized email + client address in a
15-minute window (both configurable). A blocked attempt returns `429` with a generic message.
The store is intentionally in-memory and bounded; a multi-instance deployment should move this
to a shared store such as Redis. Client address is `request.getRemoteAddr()`; behind a proxy,
forwarded-header trust must be configured explicitly rather than assumed.

## Authorization model

- Public read-only: `GET /api/news/**`, `/api/categories/**`, `/api/tools/**`,
  `/actuator/health`, `/actuator/info`, `/api/auth/csrf|login|register|logout`.
- Authenticated: dashboard, profile, bookmarks, learning endpoints and `/api/auth/me`.
- Admin: `ROLE_ADMIN` account **or** a valid `X-Admin-Key`; ordinary users receive 403.
- All personal queries are scoped by the authenticated `user_id`. There is no client-supplied
  owner id in any personal endpoint.

## Deferred security work

Email verification, password reset/change, MFA, remember-me, account deletion, shared rate
limiting, and audit logging are not implemented. Email uniqueness is normalized in the
application and enforced by the existing unique constraint. A duplicate registration returns
409 (registration necessarily reveals that an email is already registered); login remains
generic.
