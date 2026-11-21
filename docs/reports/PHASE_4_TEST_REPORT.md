# Phase 4 Test Report

Date: 2026-11-17

## Initial state verified

- Existing Docker Compose stack was healthy before changes: PostgreSQL, Spring Boot, and React frontend.
- After rebuilding/restarting only the backend and frontend, `GET /actuator/health` returned `UP`; frontend returned HTTP 200; PostgreSQL stayed running and its volume was not reset.
- Container configuration confirmed `AI_ENABLED=false`. No AI provider was called.

## Implemented and tested

| Area | Result | Evidence |
|---|---|---|
| Backend unit/integration suite | PASS | Clean Maven run: 55 tests, 0 failures/errors/skips. |
| Frontend production build | PASS | Vite production build succeeded (111 modules). |
| Docker build | PASS | Backend and frontend images built successfully. |
| Docker runtime | PASS | Updated backend/frontend containers started; backend health is UP; database remained healthy. |
| Home dashboard in browser | PASS | Dashboard and article cards rendered. |
| Navigation | PASS | All five bottom navigation links present. |
| Discover | PASS | Category/search controls rendered. |
| Empty search | PASS | Search with a unique nonexistent term displayed “No matching articles”. |
| Article details | PASS | Recommendation, explanation, provenance warning and source action rendered in the expected order. |
| Feedback | PASS | Useful selection toggled and persisted across reload in an isolated browser context; no profile/bookmark/task data was changed by the browser script. |
| Profile interests | PASS | Profile displayed all eight interest controls. No profile mutation was made. |
| Tablet layout | PASS | At 768px, no horizontal overflow. |
| Mobile layout | PASS | At 390px, no horizontal overflow. |
| Browser errors | PASS | No console errors or uncaught page errors during the final browser journey. |
| API response | PASS | Live `/api/news/latest`, `/api/news/{id}`, and `/api/profile` returned data; article had usefulness recommendation/reason and BASIC provenance. Profile preference list was empty. |
| Database | PASS | Read-only PostgreSQL query found 160 BASIC explanation rows, of which 2 currently have the new unavailable `where_used` template after the tested article reads. No reset or destructive SQL was run. |
| Dependency audit | REVIEW REQUIRED | `npm audit` reported 9 vulnerable package entries: 1 critical, 2 high and 6 moderate. Risk and major-version upgrade paths are in `DEPENDENCY_SECURITY_REPORT.md`. No dependencies were changed. |

## Fixes and retests

- Added content-aware update typing and conservative recommendation rules; targeted cases cover practical tools, funding, research, missing descriptions, interest matches and neutral defaults.
- Tightened BASIC fallback wording and refreshed existing BASIC explanations when opened. The browser check confirmed a previously stored article now shows “Not available in the provided text” instead of a category-wide product-use claim.
- Moved the original-source action below the usefulness decision and detailed explanation.
- Added local-only feedback and confirmed it survives browser reload.
- The first backend run exposed four test expectation/rule mismatches; they were corrected, then the clean backend run passed all 55 tests.

## Screenshots

- `phase4-article-desktop.png`
- `phase4-article-tablet.png`
- `phase4-article-mobile.png`

## Remaining limitations

- No real AI provider is configured or tested. AI remains disabled by default; BASIC is the active fallback.
- Exact source-quote validation is not claim-level fact checking. The current explanation schema cannot attach source excerpts to individual claims; this is disclosed on the article page and detailed in `AI_EXPLANATION_QUALITY_REPORT.md`.
- Historic BASIC explanations are refreshed lazily when the article is opened; the database count shows that not every old explanation has been opened/refreshed. No bulk rewrite was run.
- Feedback remains in local browser storage and does not affect recommendations.
- Dependency fixes imply major upgrades to Capacitor CLI, Vite and React Router. They remain unchanged pending approval and coordinated regression testing.

## Deployment

No deployment or GitHub push was performed. Docker Compose services remain running locally.
