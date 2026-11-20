# Frontend deployment (Vercel → Render)

The SPA is a static Vite build. `frontend/vercel.json` rewrites `/api/*` to the
Render backend so the browser only talks to the Vercel origin. That keeps the
`DAILYAI_SESSION` cookie first-party (no third-party cookie blocking) and makes
`SameSite=Lax; Secure` work as intended.

## Before deploying

1. Replace the placeholder host in `frontend/vercel.json`:

   ```text
   https://replace-with-render-service.onrender.com
   ```

   with the real Render service URL. Do not add a trailing slash.

2. In Vercel → Project → Settings:
   - Root Directory: `frontend`
   - Framework Preset: `Vite`
   - Build Command: `npm run build`
   - Output Directory: `dist`
   - Install Command: `npm ci`
   - Node.js Version: `22.x`

3. Environment variables (Production and Preview):

   | Variable | Value |
   | --- | --- |
   | `VITE_API_BASE_URL` | Leave unset (or empty) so requests use relative `/api` paths through the rewrite. |

   Set `VITE_API_BASE_URL` only for builds that cannot use the rewrite (for
   example a packaged Capacitor/Android build). Vite bakes the value in at build
   time, so changing it requires a redeploy.

4. On Render, set `AUTH_COOKIE_SECURE=true` and `AUTH_COOKIE_SAME_SITE=lax`.
   Because the API is proxied, requests are same-origin; keep the Vercel origin
   in `CORS_ALLOWED_ORIGINS` only as a fallback in case the proxy does not
   forward the original host.

## Why there is no SPA fallback rewrite

The app uses `HashRouter` (`src/main.jsx`), so client routes live after `#` and
never require a server-side fallback to `index.html`.
