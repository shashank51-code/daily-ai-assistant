# Live Test Report — Phase 2

**Date:** 17 Nov 2026 (Asia/Kolkata). **Status:** interactive matrix completed with headless Chrome + Playwright against the running local application. All UI actions below were executed; no result is inferred from source alone.

## Services

| Service | Status | Evidence |
|---|---|---|
| Frontend | PASS | `http://localhost:3000` returned HTTP 200 and served the app. |
| Spring Boot | PASS | Docker healthy; `/actuator/health` returned `UP`. |
| PostgreSQL | PASS | Docker healthy; PostgreSQL 16 connected; Flyway applied through version 6. |
| Production frontend build | PASS | Vite 5.4.21 build completed in the frontend image. |
| Backend automated suite | PASS | `mvn test` in Maven 3.9 / Java 17 Docker image: 40 tests, 0 failures/errors/skips. |

The existing database was reused; it was not reset. The stack remains running locally.

## Browser journeys

The initial native Chrome control path could not continue, so testing switched to the installed Chrome executable through Playwright. The application was opened at the live local URL. Normal-flow browser errors were zero; the only console errors were expected network failures from deliberately simulated backend and RSS outages.

| Page / interaction | Result | Observed outcome |
|---|---|---|
| Home dashboard | PASS | Live greeting, stats, updates and tools rendered. |
| Navigation | PASS | Home, Discover, Learn, Saved and Profile destinations exercised; five tabs visible. |
| Article detail | PASS | Opened an update; publication/source and explanation sections rendered. BASIC fallback badge appeared. |
| Reading history | PASS | Opened article and observed persisted history data after reload. Test-created history was removed after verification. |
| Discover search | PASS | “agent” returned matching articles. |
| Empty search | PASS | Unmatched query displayed “No matching articles.” |
| Category filter | PASS | Generative AI filter showed matching category cards only. |
| Bookmarks | PASS | Added a bookmark, verified API entry, reloaded, saw it under Saved, removed it and confirmed API removal. |
| Empty Saved | PASS | No bookmarks remained; “Nothing saved yet” displayed. |
| Learning topic/progress | PASS | Opened a topic, changed status, verified API progress, restored original status. |
| Daily task | PASS | Toggled a task, verified API state and reload persistence, then restored original completion. |
| AI tools | PASS | Searched Cursor, filtered AI Coding Assistants, expanded details, checked external link uses HTTPS. |
| Profile | PASS | Saved temporary display name, reloaded to verify persistence, restored original name exactly. |
| Theme | PASS | Theme switched and persisted across reload; restored starting theme. |
| Back/forward | PASS | Browser history returned Home → Discover and forward restored Discover. |
| Mobile 390×844 | PASS | No horizontal page overflow; screenshot captured. |
| Tablet 768×1024 | PASS | No horizontal page overflow; screenshot captured. |
| Desktop 1280×900 | PASS | No horizontal page overflow; screenshot captured. |
| Slow dashboard API | PASS | Delayed response displayed the loading skeleton. |
| Backend unavailable | PASS | Aborted dashboard request displayed a recoverable backend message; app recovered after route restored. |
| RSS refresh unavailable | PASS | Simulated HTTP 503 displayed “Could not refresh the news feeds right now.” |
| Empty reading history | PASS | Simulated empty profile history showed “No reading history.” |
| Unknown route | PASS | Not-found state rendered. |

Screenshots captured:

- [Desktop Discover](C:/Users/sanja/Downloads/daily-ai-assistant/qa-screenshots-desktop.png)
- [Tablet Discover](C:/Users/sanja/Downloads/daily-ai-assistant/qa-screenshots-tablet.png)
- [Mobile Discover](C:/Users/sanja/Downloads/daily-ai-assistant/qa-screenshots-mobile.png)

Visual review found consistent card layout and readable text at these sizes. Category chips extend horizontally on mobile; no page-level overflow occurred. This pass did not include a formal contrast or screen-reader audit.

## API and database verification

- Dashboard, latest/paged news, bookmarks, roadmap, daily plan, tools and profile API endpoints returned HTTP 200.
- Missing article ID returned HTTP 404.
- Article detail returned `generatedBy: BASIC` and the UI showed “Basic offline version.” Unsupported detail was explicitly marked unavailable.
- Test writes were checked through APIs and reloaded UI. Profile, bookmark, progress, and task states were restored.
- A temporary reading-history row created by the article-open test was deleted after verification. Final database state: 160 articles, 0 bookmarks, 2 reading-history rows, 1 learning-progress row, and 4 daily-task progress rows. The latter three match the pre-test baseline; the extra 30 articles are current arXiv feed data from the successful feed fix below.
- Final profile display name remains “Shashank.” No existing bookmark/progress/profile values were left changed.

## RSS, classification and data quality

Live refresh before the arXiv fix: 6 feeds succeeded, 2,446 entries read, 130 duplicates, 0 failed.

Evidence-backed fixes and retest:

1. **arXiv empty feed:** `https://export.arxiv.org/rss/cs.AI` returned HTTP 200 but only an empty 892-byte channel. arXiv’s documented endpoint `https://rss.arxiv.org/rss/cs.AI` returned 717 items and a current build timestamp. Updated the configured URL. Subsequent real refresh saved 30 papers (the configured per-feed limit), with all 6 feeds successful and 0 errors. [arXiv RSS documentation](https://github.com/arXiv/arxiv-docs/blob/develop/source/help/rss.md)
2. **Nvidia category fallback:** the publisher page labels “All the questions Nvidia should answer…” as AI, so the story is AI-related, not wholly unrelated. The app had labeled it “AI Startups” because the TechCrunch feed used that fallback when keyword rules found no category. Changed the feed fallback to the existing general “Latest AI News” category and added migration V6 for this exact article URL. Rebuilt/restarted the backend; API now returns `latest-ai-news` / “Latest AI News.” [TechCrunch article](https://techcrunch.com/2026/11/16/all-the-questions-nvidia-should-answer-on-tuesday-but-probably-wont/)
3. **Corma/Corba text mismatch:** the stored item title and original TechCrunch article identify “Corma”; the stored feed description says “Corba.” The current publisher article also says Corma. This is a confirmed mismatch in stored article metadata, likely from feed text at ingestion. No source list or article text was changed because overwriting publisher content without a safe refresh policy could corrupt other records. [TechCrunch article](https://techcrunch.com/2026/11/16/cybersecurity-ai-startup-corma-reaches-1b-valuation-in-about-3-months/)

Duplicate prevention worked in the initial refresh: all 130 stored URLs were recognized as duplicates. The arXiv correction added current papers without deleting existing articles.

## AI provider

`AI_ENABLED=false`; no provider credentials were configured. The offline `BASIC` path was verified through both API and browser. Real model generation, provider outage/retries, rate limits and model-quality claims remain **BLOCKED**. No paid provider was configured.

## Bugs fixed

- Replaced the empty arXiv export host with arXiv’s active documented RSS host.
- Changed TechCrunch’s unmatched-item fallback from “AI Startups” to “Latest AI News.”
- Added targeted Flyway migration V6 to correct the already-stored Nvidia article’s category.
- Rebuilt backend, verified migration and API category, and reran browser journeys plus all 40 backend tests.

## Remaining issues / readiness

- Stored Corma description mismatch remains; safer handling needs a defined source-refresh/update policy.
- Real AI-provider path is untested because it is disabled and has no credentials.
- Formal accessibility/contrast audit, Android device testing and real external-source-link navigation for every item remain untested.
- `npm ci` reported 9 dependency advisories (6 moderate, 2 high, 1 critical); no dependency changes or forced upgrades were made.

**Readiness:** core local journeys are now verified and pass. The app is suitable for controlled local user testing of its current no-AI configuration. Do not treat AI-generated output or the stored Corma description as verified. No deployment or GitHub push occurred.
