# Android Authentication Test Report

Date: 2026-11-19

## What was implemented for Capacitor

The Android app uses the same session cookie architecture as the browser:

- `capacitor.config.json` keeps the app origin at `https://localhost` and the bundled web assets.
- `MainActivity` now enables WebView cookie acceptance, enables cross-site cookies for the
  trusted API origin, and flushes the cookie jar after startup.
- The API client sends `withCredentials: true` and the CSRF header on mutations.
- `VITE_API_BASE_URL` is compiled into the bundle at build time.

Relevant code:

```java
CookieManager cookieManager = CookieManager.getInstance();
cookieManager.setAcceptCookie(true);
cookieManager.setAcceptThirdPartyCookies(getBridge().getWebView(), true);
cookieManager.flush();
```

This does not make the session readable by JavaScript; the cookie remains HttpOnly, and all
authentication/CSRF checks remain enabled.

## Required production configuration

Because the WebView origin (`https://localhost`) and the API origin are different sites:

```bash
VITE_API_BASE_URL=https://api.example.com
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=none
CORS_ALLOWED_ORIGINS=https://localhost
```

The API must be HTTPS. `SameSite=None` cookies are rejected without `Secure`, and Android 12+
WebView follows modern Chromium SameSite behavior. A plain-HTTP emulator backend
(`http://10.0.2.2:8080`) cannot send the production session cookie; use an HTTPS tunnel or a
same-origin deployment for authenticated device testing. For a same-origin deployment, the
frontend can reverse-proxy `/api` (the Nginx image already does this), and the cookie can remain
`SameSite=Lax`.

The existing `android:usesCleartextTraffic="true"` and `allowMixedContent` settings are retained
only for local development. Release builds should use HTTPS and remove cleartext traffic.

## Build verification

```text
$ cd frontend
$ npm run android:build
==> Vite Android build          PASS
==> cap sync android            PASS
==> gradlew assembleDebug       PASS
BUILD SUCCESSFUL in 35s
```

APK:

- Path: `frontend/android/app/build/outputs/apk/debug/app-debug.apk`
- Size: 4,262,351 bytes
- SHA-256: `77568db8e65b00921d3f007435085f839f068aee97b47447ba475659a764c3e7`
- Package: `com.dailyai.assistant`, versionCode 1, versionName 1.0
- minSdk 24, targetSdk 36, compileSdk 36
- INTERNET permission present

The build verifies that the new auth UI, Axios credential/CSRF handling, and MainActivity
compile and package. It does not prove runtime WebView behavior.

## Runtime checks — blocked

No Android emulator, connected device, or Android Studio is available in this environment.
Therefore the following were **not** executed on-device:

- Opening the app and reaching the login screen.
- Registration and login with real cookies.
- Session persistence across app reload/restart.
- Logout cookie clearing.
- HTTPS API communication from the WebView.
- Cross-site cookie acceptance on Android 12+.

These are explicit limitations, not passes. The local emulator HTTP URL cannot exercise the
production `SameSite=None; Secure` cookie, so a meaningful runtime test requires an HTTPS API
(or same-origin deployment) and a device/emulator.

## Risks / follow-up

- Verify the cookie is accepted and sent in a real WebView with an HTTPS backend.
- Confirm the app restores a session after process restart; session cookies may not survive an
  app process kill depending on WebView persistence.
- Remove cleartext/mixed-content allowances for release.
- Consider same-origin hosting (`server.url` or reverse-proxied API) to avoid third-party cookie
  policy differences entirely.
