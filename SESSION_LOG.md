# Session Log

## 2026-11-17 — Recovery and continuation

**Starting point:** Existing repository contained only `backend/` with entities, repositories,
services, RSS integration, scheduler, DTOs and Flyway migrations. There was no git history, no
controller package, no tests, no frontend and no documentation.

**Recovered state**
- Backend compiled against the existing `pom.xml` (Spring Boot 3.5.6, Java 17, ROME, Flyway).
- The app could not start because `application.yml` was invalid YAML.
- Spring context failure prevented any database or HTTP verification.

**Work completed**
1. Fixed the YAML failure and confirmed the full Spring context loads.
2. Added the missing REST layer (news, categories, learning, tools, bookmarks, dashboard,
   profile, admin), CORS configuration, category service and profile update DTO.
3. Added 30 backend tests covering parsing, categorizer, ingestion and duplicates, article
   service, learning progress, bookmarks, repository queries, validation, 404/401 and admin keys.
4. Started PostgreSQL 16 in Docker, ran Flyway migrations and verified the live API.
5. Fixed gzip handling for the DeepMind RSS feed; all 6 feeds now succeed.
6. Fixed a 500 on `GET /api/news` caused by unsupported null-precedence sorting.
7. Built the complete React client: dashboard, discover/search/filter, article detail with
   explanations, roadmap with progress, daily plan, bookmarks, profile/history, bottom navigation,
   light/dark themes, loading/empty/error states.
8. Verified the production frontend build and rendered every main route in headless Chrome
   against the live backend.
9. Added Dockerfiles, Compose stack, environment examples, ignore rules and full documentation.
10. Added Capacitor 6, generated the Android project with app name, launcher icons and
    cleartext HTTP support, and synced the production web build into it.

**Tests run**
- `mvn test`: 30 passed, 0 failed.
- `npm run build`: succeeded.
- Live API smoke tests: dashboard/news/categories/tools/learning/profile/bookmarks/progress.
- RSS: 6 feeds succeeded, 160 unique articles stored, duplicates rejected.

**Environment used for verification**
- Java 25 runtime with Java 17 target, Maven 3.9.9.
- PostgreSQL 16 container on port 5433; backend on 8080; Vite preview on 4173.

**Full-stack verification (same day)**
- Backend: `mvn test` → 30/30 passed.
- Live API: 55/55 checks passed, including pagination, all sort modes, filters, search, article
  explanations, tools, roadmap/progress, daily plan toggles, bookmark create/duplicate/delete,
  profile update/restore, admin refresh (6/6 feeds), AI-disabled response and CORS preflight.
- Browser E2E: 14/14 flows passed on a mobile viewport, covering navigation, live dashboard,
  bookmark add/remove, theme persistence, search, tool details, topic status changes, daily
  task toggles, article explanation, profile rename/restore and the not-found route.
- No uncaught page errors or console errors; backend log has no ERROR/WARN lines.
- Database verified: 160 articles, 160 explanations, 10 categories, 26 topics, 28 daily tasks;
  Flyway V1–V5 successful.
- Added `frontend/scripts/e2e-smoke.mjs` and `npm run test:e2e`; added a favicon.

**Verification after Android addition**
- `npm run build` succeeded again; `npx cap sync android` copied assets successfully.
- `docker compose config -q` validated the Compose file.
- Backend recompiled after all changes (EXIT 0).

**Not completed (external constraints)**
- AI explanation path not executed because no API key is available.
- Android APK not built because no Android SDK/Gradle toolchain is available here.

**Next session:** build and verify the Android APK, then validate AI explanations with a key.

## 2026-11-17 — AI integration hardening and Android preparation

**Starting point:** Full stack verified earlier the same day (30/30 backend, 55/55 live API,
14/14 browser E2E, 6/6 RSS feeds). Existing AI code was a single-provider OpenAI-compatible
client with no retries, no failure control and no tests.

**Phase 1 — AI integration**
- Inspected `integration/AiClient`, `service/ExplanationService`, `AdminService` and the article
detailed path. Confirmed keys are backend-only and the offline explanation always exists first.
- Added `ExplanationProvider` so `ExplanationService` no longer depends on one vendor client.
- Added bounded retries with exponential backoff and `Retry-After` support for 429/5xx, retry of
timeouts/network errors, no retry for 400/401/403, one plain-mode retry when JSON mode returns
unusable content, and a cooldown after consecutive failures so article pages fail fast.
- Added configurable `AI_TIMEOUT_MS`, `AI_MAX_ATTEMPTS`, `AI_RETRY_DELAY_MS`,
`AI_FAILURE_THRESHOLD`, `AI_COOLDOWN_MS` to `AppProperties`, `application.yml`, `.env.example`
and the README.
- Added 10 tests using a local mock OpenAI-compatible HTTP server: success, 429 then success,
malformed payload fallback, cooldown after 503, timeout, and no-key short circuit; plus offline
fallback, AI upgrade and provider-failure behaviour in `ExplanationService`.

**Phase 1 verification (no fabricated results)**
- `mvn test`: **40 passed, 0 failed** (30 existing + 10 new).
- End-to-end mock provider run: packaged backend on 8081 with `AI_ENABLED=true` against a mock
HTTP provider that rejects wrong bearer tokens; `GET /api/news/{id}` produced
`generated_by = AI`. A temporary article was used and deleted afterwards (0 rows).
- `AI_ENABLED=false` run: `aiEnabled=false` and offline explanations; all E2E flows passed.
- Real provider not exercised — no `AI_API_KEY` in this environment.
- Checked `frontend/src` and `frontend/dist`: no API key or `AI_API_KEY` reference.

**Phase 2 — Android preparation**
- Verified Capacitor config: app id `com.dailyai.assistant`, app name `Daily AI Assistant`,
web dir `dist`, Android scheme `https`.
- Verified Gradle config: applicationId/namespace `com.dailyai.assistant`, minSdk 22,
compile/target SDK 34, version 1.0, Gradle wrapper 8.2.1.
- Added `frontend/.env.android.example`, `.env.android` ignore rules and `npm run android:build`
(`vite build --mode android` + `cap sync` + `gradlew assembleDebug`).
- Verified `vite build --mode android` succeeds and all web assets build.

**Phase 3 — Android verification (blocked)**
- No Android SDK is installed (`ANDROID_HOME`/`ANDROID_SDK_ROOT` unset; no
`AppData/Local/Android/Sdk`). Default JDK is 25; JDK 17 exists at `~/.jdks/ms-17.0.19`.
- Therefore **no APK was built and no on-device/emulator verification was possible**. The
mobile viewport E2E (390×844) against the live backend was re-run instead: 14/14 passed,
including all five navigation tabs, news, bookmarks, learning progress and profile.

**Phase 4 — Documentation**
- Updated README (AI configuration/resilience, mock tests, Android prerequisites and APK steps),
DEVELOPMENT_PROGRESS.md (status, files, tests, limitations) and this log.

**Commands used for verification**
- `cd backend && ../.tools/apache-maven-3.9.9/bin/mvn -o test` → 40/40.
- Backend jars run with `DB_URL=jdbc:postgresql://localhost:5433/dailyai` (Docker PostgreSQL 16).
- `cd frontend && npx vite build --mode android` → success.
- `cd frontend && npm run test:e2e` → 14/14.

**Not completed (external constraints)**
- Android SDK install and APK build/device verification.
- Real AI provider verification.

**Next session:** install SDK + JDK 17, build/install the debug APK, and verify the five tabs
and API connectivity on an emulator; optionally configure a real LLM key.

## 2026-11-17 — Android APK build

**Starting point:** 40/40 backend tests, 14/14 browser E2E, verified AI mock-provider path,
configured Capacitor project. No Android SDK, no `adb`, no device; default Java 25.

**Step 1 — Environment**
- Java 25 is the default (`JAVA_HOME` unset). JDK 17.0.19 confirmed at
  `~/.jdks/ms-17.0.19`; JDK 27 also present. Nothing was uninstalled or changed machine-wide.
- `ANDROID_HOME`/`ANDROID_SDK_ROOT` unset; no SDK in any standard location; no adb/Gradle on PATH.
- `adb devices` empty. Capacitor config (`com.dailyai.assistant`, `Daily AI Assistant`) and
  Gradle settings (AGP/Gradle 8.2.1, minSdk 22, compile/target 34) re-confirmed.

**Step 2 — Android SDK (project-local)**
- Downloaded command-line tools 11076708 and installed to `.tools/android-sdk`.
- Accepted licenses, then installed only `platform-tools`, `platforms;android-34` and
  `build-tools;34.0.0`. No emulator, system image or other component.
- adb 37.0.1 runs. Nothing was installed system-wide; `.tools/android-sdk` is gitignored.

**Step 3 — Backend URL**
- No production backend is deployed, so no production URL was invented.
- Added `frontend/.env.android` (gitignored) with the machine's real LAN address
  `http://192.168.27.184:8080`, explicitly marked as a debug/development value.
- `vite.config.js` now refuses Android builds when `VITE_API_BASE_URL` is missing or is
  `localhost`/`127.0.0.1`; verified it exits 1 with a clear message.
- Verified the LAN URL works: `/api/dashboard` 200, `/api/news` 200, CORS preflight from
  `https://localhost` 200 (the Capacitor WebView origin).

**Step 4 — Build**
- Added `frontend/scripts/android-build.mjs` and changed `npm run android:build` to use it. It
  auto-detects JDK 17 and the SDK, writes `android/local.properties`, then runs
  `vite build --mode android` → `npx cap sync android` → `gradlew assembleDebug`.
- First assembly was run step by step (vite build, cap sync, gradlew) to isolate failures:
  `BUILD SUCCESSFUL in 6m 51s`, 82 tasks, no compilation errors. Gradle 8.2.1 and Android
  dependencies downloaded on that first run.
- Then the documented command `npm run android:build` was executed end to end with the new
  wrapper: exit 0, `BUILD SUCCESSFUL in 11s` (cached), and it printed the JDK, SDK and APK path.
- APK: `frontend/android/app/build/outputs/apk/debug/app-debug.apk`, 3,780,981 bytes (3.7 MB).
- `aapt2 dump badging`: package `com.dailyai.assistant`, label `Daily AI Assistant`,
  versionCode 1, versionName 1.0, minSdk 22, target/compile 34, INTERNET permission.
- `apksigner verify`: verifies with v1 + v2 schemes, 1 debug signer.
- Bundle contains `http://192.168.27.184:8080` and no `localhost:8080`.

**Step 5 — Device testing (not possible)**
- `adb devices` reports no attached device; no emulator or system image is installed.
- Therefore the APK was **not installed or launched**, and the five tabs, news, search,
  bookmarks, learning progress, profile and in-app backend connectivity were **not** tested on
  Android. Manual install instructions were added to the README:
  `adb install -r frontend/android/app/build/outputs/apk/debug/app-debug.apk`, then
  `adb shell am start -n com.dailyai.assistant/.MainActivity`.

**Step 6 — Documentation**
- Updated README (SDK install commands, JDK 17 auto-selection, URL validation, APK path,
  adb install commands), DEVELOPMENT_PROGRESS.md and this log.

**Not completed (external constraints)**
- On-device/emulator verification of the APK.
- Production backend deployment and release-signed APK.

**Next session:** connect a phone on the same Wi-Fi with the backend running and execute the
manual install/launch commands, then verify all five tabs and API-backed actions on the device.
