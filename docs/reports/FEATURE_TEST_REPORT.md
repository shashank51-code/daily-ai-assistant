# Feature Test Report

**Date:** 17 Nov 2026. Checks were attempted from the repository as found. No successful runtime tests are claimed.

## Executed checks

| Check | Result | Evidence |
|---|---|---|
| Frontend production build (`npm run build`) | Blocked | `vite` command not found; frontend dependencies are absent. |
| Browser E2E (`npm run test:e2e`) | Blocked | Node could not import `playwright-core`; dependencies are absent. |
| Backend tests (`mvn test`) | Not runnable | Maven executable is not installed/available (`mvn` not recognized). |
| API/browser flows | Not run | Backend/database/browser are not running; E2E dependency absent. |

## Source-level coverage observed

Backend tests exist for admin, bookmark and news controllers; AI client, categorizer, RSS parser and text utilities; article repository; and article, bookmark, explanation, learning and ingestion services. The E2E script describes checks for navigation, dashboard content, bookmark add/remove, learning task toggles, article explanation, profile save and unknown routes, plus browser errors. Presence of tests is not a pass result.

## Major flow status

| Flow | Result |
|---|---|
| Dashboard and current updates | Not runtime tested |
| Live RSS, dates, categories, duplicate handling | Not integration tested |
| Article explanation and offline fallback | Unit tests exist; suite not executed |
| Bookmark add/remove and reading history | Tests/E2E exist; not executed |
| Search/filter | Not runtime tested |
| Roadmap and progress | Tests/E2E coverage partial; not executed |
| Daily task progress | E2E script coverage; not executed |
| Profile save | E2E script coverage; not executed |
| Navigation/mobile/loading/error states | E2E script partial; not executed |
| PostgreSQL migrations and Docker | Not run |
| Android package/device behavior | Not run |

## Reproduction / next verification

1. Install Java 17 and Maven 3.9+, Node 20+, PostgreSQL 16 (or Docker), and Chrome.
2. Copy `.env.example` to `.env`; use a local DB password. Start PostgreSQL and the backend.
3. In `frontend`, run `npm ci`, `npm run build`, start preview, then `npm run test:e2e` with the backend and Chrome available.
4. In `backend`, run `mvn test`; separately verify Flyway against PostgreSQL and trigger RSS refresh.

No feature is marked passed because no test command completed successfully.
