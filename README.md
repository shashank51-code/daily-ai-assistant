# Daily AI Updates & Learning Assistant

A mobile-first app for freshers and students: real AI news collected from RSS feeds, a
beginner-friendly five-level learning roadmap, AI tool discovery, a daily learning plan,
bookmarks and reading history. The backend is Spring Boot 3 with PostgreSQL; the client is
React (JavaScript/CSS/Axios) packaged for Android with Capacitor.

The app runs completely without a paid AI service. When an OpenAI-compatible endpoint is
configured, article explanations are upgraded by the model; otherwise a truthful offline
explanation is generated only from the feed text.

## Architecture

```
React + Vite + Axios (browser / Capacitor Android)
                │  JSON over HTTP
                ▼
Spring Boot 3 REST API  ──►  PostgreSQL (Flyway migrations)
      │        │
      │        └──► Optional OpenAI-compatible chat completions API
      └──► RSS feeds (OpenAI, DeepMind, Google AI, Hugging Face, TechCrunch, arXiv)
             fetched by Spring Scheduler every 3 hours (Asia/Kolkata)
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for package layout and design decisions.

## Technology stack

| Layer    | Technology |
|----------|------------|
| Backend  | Java 17, Spring Boot 3.5, Spring Web, Spring Data JPA, Validation, Scheduler, Actuator |
| Database | PostgreSQL 16, Flyway migrations |
| RSS      | ROME 2.1 (`com.rometools:rome`) over the JDK HTTP client |
| Frontend | React 18, JavaScript, custom CSS, Axios, React Router (hash routing) |
| Build    | Maven, Vite, npm |
| Android  | Capacitor 6 |

## Folder structure

```
daily-ai-assistant/
├── backend/
│   ├── src/main/java/com/dailyai/assistant/
│   │   ├── config/        AppProperties, CORS
│   │   ├── controller/    REST endpoints
│   │   ├── dto/           API records
│   │   ├── entity/        JPA entities
│   │   ├── exception/     API error handling
│   │   ├── integration/   RSS fetch/parse, categorizer, optional AI client
│   │   ├── repository/    Spring Data repositories
│   │   ├── scheduler/     @Scheduled refresh + startup loader
│   │   └── service/       business logic
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/  V1 schema, V2–V5 seed data
│   └── src/test/java/     unit, MVC and repository tests
├── frontend/
│   ├── src/api/           Axios client and endpoint functions
│   ├── src/components/    layout, cards, states, icons
│   ├── src/pages/         home, discover, article, learn, topic, bookmarks, profile
│   ├── src/hooks/         data loading hook
│   └── src/styles.css     custom mobile-first CSS with light/dark themes
├── docker-compose.yml
├── .env.example
└── docs: README, ARCHITECTURE, API_DOCUMENTATION, DEVELOPMENT_PROGRESS, SESSION_LOG
```

## Prerequisites

- Java 17+ and Maven 3.9+
- Node.js 20+ and npm
- PostgreSQL 16 (local or Docker)
- Optional: Docker Desktop, Android Studio + SDK for the APK

## 1. Database setup

### Option A: Docker (recommended)

```bash
cp .env.example .env          # set a strong DB_PASSWORD
docker compose up -d db
```

The database listens on `localhost:5432` with database/user `dailyai`.

### Option B: Existing PostgreSQL

```sql
CREATE DATABASE dailyai;
CREATE USER dailyai WITH PASSWORD 'your-password';
GRANT ALL PRIVILEGES ON DATABASE dailyai TO dailyai;
```

Flyway creates all tables and seed data on the first backend start. No manual SQL is needed.

## 2. Run the backend

```bash
cd backend
export DB_URL=jdbc:postgresql://localhost:5432/dailyai
export DB_USERNAME=dailyai
export DB_PASSWORD=your-password
mvn spring-boot:run
```

- API: http://localhost:8080
- Health: http://localhost:8080/actuator/health
- On an empty article table the app fetches all configured feeds once at startup.

Run the tests:

```bash
cd backend
mvn test
```

## 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. Vite proxies `/api` to `http://localhost:8080`.
For a standalone production build set `VITE_API_BASE_URL`:

```bash
# frontend/.env.local
VITE_API_BASE_URL=http://localhost:8080
npm run build
npm run preview
```

## 4. Run everything with Docker Compose

```bash
cp .env.example .env          # set DB_PASSWORD and optionally ADMIN_API_KEY
docker compose up --build
```

- Frontend: http://localhost:3000
- API: http://localhost:8080

## RSS configuration

Feeds live in `backend/src/main/resources/application.yml` under `app.rss.feeds`. Each entry
has a name, URL and fallback category. The scheduler runs at minute 0 every third hour in
`Asia/Kolkata`; the timestamp is configurable with `APP_TIMEZONE`.

Manual refresh:

```bash
curl -X POST http://localhost:8080/api/admin/news/refresh
# If ADMIN_API_KEY is set:
curl -X POST -H "X-Admin-Key: $ADMIN_API_KEY" http://localhost:8080/api/admin/news/refresh
```

Behaviour and limits:

- Articles are de-duplicated by the original URL (database unique constraint plus in-run set).
- A failing, slow or malformed feed is recorded in the refresh result and never stops other feeds.
- gzip/deflate responses are decompressed before parsing.
- Publication dates are never fabricated; a feed without a date stores `null`.
- Only the newest `max-items-per-feed` entries per feed are considered.
- The publisher is shown as the source; "Read more" opens the original URL.
- Please respect each publisher's terms and rate limits. Default scheduling is deliberately low frequency.

## Optional AI explanations

The AI layer is provider independent: it talks to any service that exposes an
OpenAI-compatible `POST /chat/completions` endpoint. Only the Spring Boot backend calls the
provider. API keys live in backend environment variables and are never sent to, or bundled
with, the frontend or the Android APK.

```bash
AI_ENABLED=true
AI_BASE_URL=https://api.openai.com/v1   # or another compatible endpoint
AI_API_KEY=sk-...                       # never commit this
AI_MODEL=gpt-4o-mini
```

Optional resilience settings (defaults shown):

```bash
AI_TIMEOUT_MS=30000        # per request, connect + read
AI_MAX_ATTEMPTS=2          # total attempts for transient failures
AI_RETRY_DELAY_MS=1000     # base delay, doubled per attempt (capped at 10s)
AI_FAILURE_THRESHOLD=2     # consecutive failures before pausing
AI_COOLDOWN_MS=300000      # pause length after the threshold is reached
```

Behaviour:

- With no key (or `AI_ENABLED=false`) every article gets the truthful offline explanation
  built only from the feed title and description; the app is fully functional.
- `429` and `5xx` responses are retried with bounded exponential backoff and an honoured
  `Retry-After` header. Timeouts and network errors are retried, then swallowed.
- After repeated failures the provider is skipped during the cooldown, so article pages keep
  loading quickly instead of waiting on a broken provider.
- `400`/`401`/`403` are treated as configuration errors and are not retried.
- The prompt forbids invented facts and requires `"Not available in the provided text."` for
  unknown details. `generatedBy` in the API reports `AI` or `BASIC`.
- A response that is not usable JSON triggers one retry without `response_format`, then the
  offline explanation is kept.

Tests use a local mock provider that implements the OpenAI protocol, so no key is needed:

```bash
cd backend
mvn test -Dtest=AiClientTest,ExplanationServiceTest
```

Real-provider generation has not been verified in this environment because no `AI_API_KEY`
is available; the complete request/response path was verified end to end against the mock
provider (see `DEVELOPMENT_PROGRESS.md`).

## Android build (Capacitor)

The Android project lives in `frontend/android` with application ID `com.dailyai.assistant`,
app name `Daily AI Assistant`, minSdk 22 and targetSdk 34.

Prerequisites:

- **JDK 17**. `npm run android:build` locates a JDK 17 automatically (checks `JAVA_HOME`, then
  `~/.jdks/*`) and passes it to Gradle only for this build, so a newer machine-wide Java is
  left untouched. Android Gradle Plugin 8.2 does not support newer JDKs.
- **Android SDK** with Platform 34, Build-Tools 34.0.0 and platform-tools. The script looks in
  `ANDROID_HOME`, `ANDROID_SDK_ROOT`, the project-local `.tools/android-sdk`, then the default
  per-user location. A project-local install used by this repository:

  ```bash
  # from the project root; downloads only the three required packages
  mkdir -p .tools/android-sdk/cmdline-tools
  curl -L -o /tmp/cmdline-tools.zip \
    https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip
  unzip -q /tmp/cmdline-tools.zip -d .tools/android-sdk/cmdline-tools
  mv .tools/android-sdk/cmdline-tools/cmdline-tools .tools/android-sdk/cmdline-tools/latest
  yes | .tools/android-sdk/cmdline-tools/latest/bin/sdkmanager.bat \
    --sdk_root="$(cygpath -m "$PWD/.tools/android-sdk")" --licenses
  yes | .tools/android-sdk/cmdline-tools/latest/bin/sdkmanager.bat \
    --sdk_root="$(cygpath -m "$PWD/.tools/android-sdk")" \
    "platform-tools" "platforms;android-34" "build-tools;34.0.0"
  ```

  (Use `sdkmanager` without `.bat` on macOS/Linux.)
- The Gradle wrapper downloads Gradle 8.2.1 on the first build (internet required).

The API URL is compiled into the web assets, so set it before building:

```bash
cd frontend
cp .env.android.example .env.android
# edit VITE_API_BASE_URL: emulator http://10.0.2.2:8080 | phone http://<LAN-IP>:8080
# production https://api.your-domain.example
```

The Android build **fails on purpose** when `VITE_API_BASE_URL` is missing or points at
`localhost`/`127.0.0.1`, because neither works on a phone. There is no default production URL:
a release build needs a real deployed backend over HTTPS.

Build the debug APK:

```bash
cd frontend
npm run android:build
# APK: frontend/android/app/build/outputs/apk/debug/app-debug.apk
```

The script builds the web assets with `vite build --mode android`, runs `npx cap sync android`,
writes the gitignored `android/local.properties` with the detected SDK, and calls
`gradlew assembleDebug` with JDK 17 + the SDK on `PATH`.

Install on a connected phone (USB debugging enabled):

```bash
# Windows (SDK in the project)
.tools/android-sdk/platform-tools/adb.exe install -r \
  frontend/android/app/build/outputs/apk/debug/app-debug.apk
.tools/android-sdk/platform-tools/adb.exe shell am start -n \
  com.dailyai.assistant/.MainActivity
# macOS/Linux, SDK on PATH
adb install -r frontend/android/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.dailyai.assistant/.MainActivity
```

Or copy the APK to the phone and open it (allow installing from unknown sources).

Notes for physical devices:

- The phone and computer must be on the same network; the backend must listen on all interfaces
  (Spring Boot's default `0.0.0.0:8080`, which was verified with the LAN URL).
- Windows Firewall may need to allow inbound TCP 8080 for the phone to reach the backend.
- Plain HTTP to a LAN IP requires cleartext traffic, enabled in
  `android/app/src/main/AndroidManifest.xml` (`android:usesCleartextTraffic="true"`).
- Release builds should use HTTPS and the correct `CORS_ALLOWED_ORIGINS`; remove cleartext
  traffic when the API is HTTPS-only.
- The APK is not committed (`*.apk` is ignored). A debug APK was built in this repo at
  `frontend/android/app/build/outputs/apk/debug/app-debug.apk` (3,780,981 bytes, v1+v2 signed).

## API documentation

Full request/response reference: [API_DOCUMENTATION.md](API_DOCUMENTATION.md).

| Group      | Endpoints |
|------------|-----------|
| News       | `GET /api/news`, `/latest`, `/search`, `/category/{slug}`, `/{id}` |
| Categories | `GET /api/categories` |
| Learning   | `GET /api/learning/roadmap`, `/topics`, `/topics/{id}`, `/progress`, `/daily-plan`; `POST /api/learning/progress`, `/daily-plan/{taskId}` |
| Tools      | `GET /api/tools`, `/tools/{id}`, `/tools/categories` |
| Bookmarks  | `GET/POST /api/bookmarks`, `DELETE /api/bookmarks/{id}`, `DELETE /api/bookmarks?itemType=&itemId=` |
| Dashboard  | `GET /api/dashboard` |
| Profile    | `GET/PUT /api/profile`, `GET /api/profile/history` |
| Admin      | `POST /api/admin/news/refresh`, `POST /api/admin/articles/{id}/explain`, `GET /api/admin/status` |

There is no authentication: the app is designed as a single-profile personal app and all
records belong to the default user seeded by Flyway. Do not expose the admin endpoints
publicly without setting `ADMIN_API_KEY`.

## Testing

```bash
cd backend && mvn test        # 30 unit/MVC/repository tests
cd frontend && npm run build  # production build check

# Browser E2E (needs the backend + `npm run preview` running, and Chrome installed)
cd frontend && npm run test:e2e   # 14 UI flows on a mobile viewport
```

Backend tests cover RSS parsing (including malformed XML and missing dates), gzip-safe
fetching is exercised live, duplicate detection, categorizer fallback, article listing and
sorting, repository queries against H2, learning progress, bookmarks, validation, 404/401
handling and the admin key check.

The end-to-end suite (`frontend/scripts/e2e-smoke.mjs`) drives the installed Chrome through
the real UI: navigation, dashboard data, bookmark add/remove, theme persistence, news search,
tool details, topic progress, daily task toggles, article explanations and profile updates.
It also fails if the browser reports uncaught exceptions or console errors.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `Cannot reach the backend` in the UI | Check the API URL (`VITE_API_BASE_URL`) and that port 8080 is open. |
| Startup fails with `Schema-validation` errors | Flyway did not run; check DB credentials and `spring.flyway.enabled`. |
| `401` on admin refresh | Set/enter the same `X-Admin-Key` value as `ADMIN_API_KEY`. |
| Empty news list | Run a manual refresh and check the response `errors` array. |
| DeepMind feed failed | It is gzip-compressed; the client now decompresses it automatically. |
| Android app shows network error | Use `10.0.2.2` (emulator) or LAN IP (phone), allow cleartext HTTP, restart backend after changing CORS. |
