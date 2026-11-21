# Phase 5 Test Report

Date: 2026-11-17

## Results

| Check | Result | Evidence |
|---|---|---|
| PostgreSQL | PASS | Docker Compose reports healthy; data volume was not reset. |
| Spring Boot | PASS | Container healthy; live article-detail API returned the expected updated category and research summary. |
| React frontend | PASS | Frontend container running at `http://localhost:3000`; production build passed. |
| Backend clean tests | PASS | Maven 3.9 / Java 17 clean suite: 63 tests, 0 failures, 0 errors, 0 skipped. |
| Docker builds | PASS | Backend and frontend images built from current source; running services healthy. |
| Research article API | PASS | `/api/news/160` returned `ai-research`, BASIC explanation provenance, `beginnerSummary`, source sentences, and explicit interpretation labels. |
| Database classification | PASS | Read-only query found 30 arXiv abstracts in `ai-research`. No database reset or destructive query. |
| Browser article and layout | PASS | Live Chrome at 1440×960, 768×1024, 390×844; top/middle/bottom scroll positions checked for internal scrolling and header/nav overlap. |
| Five bottom navigation destinations | PASS | Home, Discover, Learn, Saved, Profile opened in each emulated viewport. |
| Browser console/page errors | PASS | None observed during the viewport/navigation run. |
| Playwright full legacy smoke flow | BLOCKED / NOT RUN | Existing smoke script mutates bookmarks, learning progress, tasks and profile name. This pass used read-only navigation checks to avoid changing the user’s persistent data. |
| Android build | BLOCKED | Android SDK platform 34 missing; JDK 21 unavailable for the Capacitor 7 candidate. No native build result is claimed. |
| Current npm audit | FAIL / REVIEW REQUIRED | 9 advisories: 1 critical, 2 high, 6 moderate. Major candidate trial and remediation plan are in `SECURITY_REMEDIATION_REPORT.md`. |
| Real AI provider | BLOCKED | `AI_ENABLED=false`; no provider was called. BASIC fallback was the live response. |

## Screenshots

See the nine `phase5-{desktop,tablet,mobile}-{top,middle,bottom}.png` files and [MOBILE_UX_TEST_REPORT.md](MOBILE_UX_TEST_REPORT.md).

## Regressions and fixes

- Corrected arXiv classification using source metadata, with a narrow migration; verified API and database state.
- Reworked article scrolling so content stays between the header and bottom navigation.
- Added concise research summary and separated publisher abstract from interpretations.
- A full test run exposed a research-summary fixture that did not include a required abstract term; the fixture was aligned with the paraphrase’s source conditions and the clean suite then passed.

No deployment, GitHub push, paid provider setup, database reset, or dependency major upgrade was performed.
