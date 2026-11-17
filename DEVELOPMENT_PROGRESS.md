# Development Progress

Last updated: 2026-11-17 (Asia/Kolkata)

## Last completed task

Android APK build. The Android SDK (platform 34, build-tools 34.0.0, platform-tools) was
installed project-locally under `.tools/android-sdk`, a JDK 17-only build wrapper was added, and
the debug APK was produced: `frontend/android/app/build/outputs/apk/debug/app-debug.apk`
(3,780,981 bytes), verified with `aapt2` and `apksigner`. No Android device or emulator is
connected, so installation and on-device UI testing were **not** performed. Backend suite is
**40/40 passing**; browser E2E is **14/14 passing**.

## Current implementation status

| Phase | Scope | Status |
|-------|-------|--------|
| 1 | Spring Boot setup, entities, Flyway schema and seed data | Verified |
| 2 | RSS integration, parsing, de-duplication, scheduler | Verified live (6/6 feeds succeed) |
| 3 | News/category/learning/tools/bookmark/dashboard/profile/admin APIs | Verified live |
| 4 | React dashboard and mobile UI | Verified (production build + headless browser) |
| 5 | Bookmarks, reading history, roadmap, daily plan | Verified live |
| 6 | Optional AI explanations | Verified against a mock provider end to end; real provider not executed (no AI key available) |
| 7 | Capacitor Android packaging | **Debug APK built and verified**; not device-tested (no adb device/emulator) |
| 8 | Tests, bug fixing, deployment preparation | Backend 40/40 tests pass; browser E2E 14/14; Docker files added, compose not started |

## Verified working

- Backend compiles and packages (`mvn package`).
- Flyway applied V1–V5 to PostgreSQL 16; Hibernate `validate` passes.
- Startup fetch stored 130 articles from 5 feeds; a second manual refresh stored 30 more from
  the previously failing DeepMind feed. Latest result: 6 feeds attempted/succeeded, 2720 items
  read, 130 duplicates skipped, 0 failures.
- `GET /api/dashboard`, `/api/news`, `/api/news/{id}`, `/api/categories`, `/api/tools`,
  `/api/learning/roadmap`, `/api/profile` return real data.
- Bookmark add/list/delete-by-item, topic progress completion, reading history and streak all
  persisted and verified through HTTP.
- React production build succeeds (259 kB JS, 14.3 kB CSS).
- Browser check at desktop viewport rendered live dashboard, Discover, Learn, Bookmarks,
  Profile and an article detail with the eight-section explanation.
- Android/phone navigation uses a fixed bottom bar; light/dark themes persist.

## Bugs found and fixed during recovery

1. **Invalid `application.yml`**: the unquoted RSS User-Agent default contained
   `contact: student@...`, which made the YAML invalid and prevented the Spring context from
   starting at all. Quoted the default value.
2. **Missing API layer**: services and DTOs existed but no `controller` package, so the app had
   no usable HTTP endpoints. Added eight controllers, CORS config, category service and a
   profile update DTO.
3. **DeepMind feed failed**: the feed is gzip-compressed despite `Content-Type: application/xml`,
   causing "Content is not allowed in prolog". Added gzip/deflate decompression with a size cap.
4. **`GET /api/news` returned 500**: `Sort.NullHandling.NULLS_LAST` is unsupported by Spring Data
   JPA criteria sorting. Removed null precedence and added a paged-sorting repository test.
5. `BookmarkService.deleteByItem` was annotated `readOnly = true` although it deletes; corrected.

## Files created in this session

Backend:
- `controller/NewsController.java`, `CategoryController.java`, `LearningController.java`,
  `ToolController.java`, `BookmarkController.java`, `DashboardController.java`,
  `ProfileController.java`, `AdminController.java`
- `config/WebConfig.java`, `service/CategoryService.java`, `dto/ProfileUpdateRequest.java`
- Tests: `integration/RssParserTest`, `ArticleCategorizerTest`, `TextUtilsTest`;
  `service/NewsIngestionServiceTest`, `ArticleServiceTest`, `LearningServiceTest`,
  `BookmarkServiceTest`; `controller/NewsControllerTest`, `BookmarkControllerTest`,
  `AdminControllerTest`; `repository/ArticleRepositoryTest`

Frontend (new project):
- `package.json`, `vite.config.js`, `index.html`, `.env.example`, `capacitor.config.json`
- `android/` Capacitor project (app id `com.dailyai.assistant`, generated icons, cleartext HTTP)
- `src/main.jsx`, `App.jsx`, `styles.css`
- `src/api/client.js`, `api.js`; `src/hooks/useAsync.js`; `src/utils/format.js`
- `src/components/`: Layout, BottomNav, Icons, States, ArticleCard, ToolCard, BookmarkToggle
- `src/pages/`: Home, Discover, Article, Learn, Topic, Bookmarks, Profile, NotFound

Project files:
- `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile`, `frontend/nginx.conf`,
  `.env.example`, `.gitignore`, `README.md`, `ARCHITECTURE.md`, `API_DOCUMENTATION.md`,
  `DEVELOPMENT_PROGRESS.md`, `SESSION_LOG.md`

## AI integration (this phase)

- `integration/ExplanationProvider.java` — provider-independent contract (`isEnabled`,
  `isAvailable`, `generateExplanation`). `ExplanationService` now depends on the interface, so
  another provider can replace `AiClient` without touching business logic.
- `integration/AiClient.java` — OpenAI-compatible implementation with:
  - no-key/no-enabled short circuit (app stays functional with offline explanations);
  - bounded retries with exponential backoff and `Retry-After` support for 429/5xx;
  - timeout/network retry then graceful fallback;
  - no retry for 400/401/403 configuration errors;
  - one plain-mode retry when JSON mode returns unusable content;
  - cooldown after consecutive failures so article pages do not block on a broken provider.
- `config/AppProperties.java` + `application.yml` + `.env.example` — new settings
  `AI_MAX_ATTEMPTS`, `AI_RETRY_DELAY_MS`, `AI_FAILURE_THRESHOLD`, `AI_COOLDOWN_MS`.
- Tests `integration/AiClientTest` (6) and `service/ExplanationServiceTest` (4) exercise a local
  mock OpenAI-compatible HTTP provider: success, 429 retry, malformed payload, cooldown after
  5xx, timeout and no-key behaviour, plus offline fallback and AI upgrade in the service.
- Keys remain backend-only; a search of `frontend/src` and `frontend/dist` found no key or
  `AI_API_KEY` reference.

### AI verification performed

- Mock provider (local HTTP server, rejects a wrong bearer token) with the packaged backend on
  port 8081 and a temporary article: `GET /api/news/{id}` stored `generated_by = AI` with the
  mock draft, and the log recorded "Generated AI explanation for article …". The temporary
  article was then deleted (0 rows remain).
- AI disabled (`AI_ENABLED=false`): `GET /api/admin/status` reports `aiEnabled=false`, article
  detail returns the offline explanation, and all E2E flows pass.
- Real provider: **not executed** — no key available. Do not read the mock run as proof that a
  specific vendor works.

## Android build (this phase)

- Verified `capacitor.config.json`: `appId com.dailyai.assistant`, `appName Daily AI Assistant`,
  `webDir dist`, Android scheme `https`.
- Verified `android/app/build.gradle`: `namespace`/`applicationId` `com.dailyai.assistant`,
  minSdk 22, compile/target SDK 34, versionCode 1, versionName 1.0.
- Android project already generated (`frontend/android`, Gradle wrapper 8.2.1).
- Added `frontend/.env.android.example`, a `.env.android` ignore rule and
  `npm run android:build`, which builds with `--mode android`, syncs Capacitor and runs
  `gradlew.bat assembleDebug`. Verified `vite build --mode android` succeeds.
- Android SDK installed project-locally at `.tools/android-sdk` (cmdline-tools, `platforms/android-34`,
  `build-tools/34.0.0`, `platform-tools` with adb 37.0.1). No emulator, system image or extra
  component was downloaded.
- `frontend/scripts/android-build.mjs` + `npm run android:build`: finds JDK 17 (default Java is
  25), finds the SDK, writes the gitignored `android/local.properties`, then builds, syncs and
  assembles. `vite.config.js` now fails Android builds when `VITE_API_BASE_URL` is missing or
  points at `localhost`/`127.0.0.1`.
- API URL for the debug APK: the machine's real LAN address `http://192.168.27.184:8080`, stored
  in the gitignored `frontend/.env.android`. This is a development value, not a production URL;
  no production backend is deployed. Release builds must use a deployed HTTPS API.

## Files modified

- `backend/src/main/java/.../integration/AiClient.java` (resilience, provider interface)
- `backend/src/main/java/.../integration/ExplanationProvider.java` (new)
- `backend/src/main/java/.../service/ExplanationService.java` (depends on the interface)
- `backend/src/main/java/.../config/AppProperties.java`, `application.yml` (AI settings)
- `backend/src/test/java/.../integration/AiClientTest.java` (new)
- `backend/src/test/java/.../service/ExplanationServiceTest.java` (new)
- `frontend/package.json`, `frontend/.env.android.example` (new), `.gitignore`, `.env.example`
- `README.md`, `DEVELOPMENT_PROGRESS.md`, `SESSION_LOG.md`

## Earlier files modified

- `backend/src/main/resources/application.yml` (YAML fix)
- `backend/src/main/java/.../integration/RssFeedClient.java` (gzip/deflate)
- `backend/src/main/java/.../service/ArticleService.java` (sort fix)
- `backend/src/main/java/.../service/BookmarkService.java` (transaction fix)
- `backend/src/test/java/.../repository/ArticleRepositoryTest.java` (sort regression test)

## Tests executed

| Command | Result |
|---------|--------|
| `cd backend && mvn test` | **40 tests, 0 failures, 0 errors** (30 previous + 10 AI) |
| Live API suite (55 assertions, earlier run) | **55 passed, 0 failed** — reads, writes, paging, sorting, search, validation, 404s, admin refresh, CORS |
| `cd frontend && npm run build` | Success (production bundle) |
| `cd frontend && npx vite build --mode android` | Success; Android-mode bundle produced |
| `cd frontend && npm run test:e2e` | **14 passed, 0 failed** on a 390×844 viewport (re-run after AI changes) |
| Live AI mock provider (backend + PostgreSQL) | Article detail stored `generated_by = AI`; temporary article removed afterwards |
| AI disabled (`AI_ENABLED=false`) | `aiEnabled=false`; offline explanation returned; E2E unaffected |
| `cd frontend && npm run android:build` | **BUILD SUCCESSFUL in 6m 51s**, 82 tasks; APK produced |
| Android guard: localhost URL | Build fails with a clear error (`exit 1`) as intended |
| `aapt2 dump badging app-debug.apk` | `com.dailyai.assistant`, label `Daily AI Assistant`, minSdk 22, target/compile 34, INTERNET permission |
| `apksigner verify --verbose` | Verifies; v1 (JAR) and v2 schemes true, 1 signer (debug key) |
| APK file | 3,780,981 bytes (3.7 MB) at `frontend/android/app/build/outputs/apk/debug/app-debug.apk` |
| LAN connectivity | `GET http://192.168.27.184:8080/api/dashboard` → 200; `/api/news` → 200; CORS preflight from `https://localhost` → 200 with `Access-Control-Allow-Origin: https://localhost` |
| Android device | `adb devices` empty — no phone/emulator attached, installation not attempted |
| `mvn -DskipTests package` | Success; runnable jar produced |
| Database state after tests | 160 articles, 160 explanations, 10 categories, 26 topics, 28 tasks; Flyway V1–V5 all successful |

## Database migration status

- PostgreSQL 16 container (`dailyai-postgres`, port 5433 for verification).
- Flyway history includes V1 schema and V2–V5 seed migrations; schema validated by Hibernate.
- Seed data: 1 profile, 10 categories, 26 learning topics, AI tools, 28 daily tasks.
- No migrations were edited after being applied.

## Errors remaining / limitations

No runtime errors in the verification runs.

- **Real AI provider unverified**: no `AI_API_KEY` is available. The full request/response path
  is proven against a mock OpenAI-compatible provider, including `generated_by = AI`; a specific
  vendor has not been exercised.
- **Android device testing not performed**: the debug APK builds and verifies, but `adb devices`
  reports no connected phone and no emulator/system image is installed. Tabs, search, bookmarks,
  learning, profile and backend connectivity inside the Android WebView remain untested on a
  device. The same UI and API paths pass in the 390×844 browser E2E, which is not a substitute.
- **Debug APK uses a LAN URL**: `frontend/.env.android` points at `http://192.168.27.184:8080`,
  reachable only on the same Wi-Fi with the backend running. No production backend is deployed,
  so a release APK still needs a deployed HTTPS `VITE_API_BASE_URL`.
- No authentication by design (single-profile app). Admin key is optional locally.
- Docker Compose files were written but not started end-to-end; the same services were verified
  with a manually started PostgreSQL container and `java -jar`.
- `DeepMind` and other feeds can change format or rate-limit at any time; failures are visible
  in the refresh response, not silently ignored.

## Pending tasks

1. Install the APK on a phone or emulator and verify the five tabs, news, search, bookmarks,
   learning progress, profile and backend connectivity on the device.
2. Enable a real AI provider (`AI_ENABLED=true`, `AI_API_KEY`) and confirm
   `generatedBy = AI` on a live article with production network access.
3. Deploy the backend over HTTPS and build a release APK with that URL (unsigned release needs a
   keystore); the debug APK currently points at a LAN address.
4. Optional: add authentication if the app will be publicly deployed.
5. Optional: CI workflow running `mvn test`, `npm run build` and `npm run android:build`.

## Exact next task to execute

Start the backend (`SERVER_PORT=8080`, listening on `0.0.0.0`), connect a phone with USB

debugging on the same Wi-Fi, then run
`adb install -r frontend/android/app/build/outputs/apk/debug/app-debug.apk` and
`adb shell am start -n com.dailyai.assistant/.MainActivity`. Verify Home, Discover, Learn, Saved
and Profile, article search, bookmark persistence and the reading/learning progress writes
against the live API, watching `adb logcat` for WebView or network errors.
