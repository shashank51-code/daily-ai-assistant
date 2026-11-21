# Project Audit Report

**Scope:** static inspection of the checked out repository and attempted local checks, 17 Nov 2026. No services were running. Findings below distinguish code inspection from executed tests.

## Summary

Daily AI Assistant is a React/Vite + Spring Boot/PostgreSQL single-profile app for RSS updates, beginner explanations, learning topics/tasks, tools, bookmarks, reading history and a profile. The architecture is coherent and has tests for key backend units and controller flows. It is **not ready for verified local user testing from this checkout**: dependencies/runtime setup were removed during cleanup, so build and behavioral checks did not run. There is intentionally no authentication; public deployment would expose shared profile and progress data.

## Feature status from code inspection

| Area | Implemented | Verified status |
|---|---|---|
| Dashboard, Discover, article detail | React pages, API services, RSS-backed article data | Present; runtime blocked |
| RSS ingest and duplicate prevention | Scheduled/startup refresh, feed parsing, URL uniqueness | Implemented; no live feed verification |
| Categories and search/filter | Backend query/controller and Discover UI | Present; runtime blocked |
| AI explanations | Optional backend OpenAI-compatible client, offline fallback | Implemented; provider quality and live fallback unverified |
| Bookmarks/history | Single seeded user and persistence | Present; runtime blocked |
| Roadmap, topics, daily tasks | Seeded topics/tasks and progress endpoints/UI | Present; runtime blocked |
| AI tools directory | Seeded catalog and UI | Present; runtime blocked |
| Profile | Shared single-profile data | Present; no authentication by design |
| Mobile/navigation/themes | Hash routing, responsive CSS, bottom nav, theme preference | Present; browser/device behavior unverified |
| Docker/Android | Compose and Capacitor files | Present; builds not run |

## Findings and recommendations

### P0 — Protect user data before public hosting

- **Problem:** architecture explicitly has no authentication; controllers and `CurrentUserService` use a shared seeded profile.
- **Why it matters:** anyone reaching a public deployment may read or alter the same profile, bookmarks and progress.
- **Solution:** require authentication and bind every data query to the authenticated principal before public exposure.
- **User benefit:** private, independent learning history and saved items.
- **Effort:** L (1–2 weeks). **Risk/dependency:** identity/session design and migration of single-user data.

### P1 — Restore reproducible local verification

- **Problem:** Maven is unavailable; `frontend/node_modules`, `.env`, `.tools`, and generated Android build assets are absent. Frontend build and E2E could not run.
- **Why it matters:** no evidence currently confirms this checkout builds or its flows work.
- **Solution:** install documented prerequisites, copy example env files, start PostgreSQL/backend, install npm dependencies, then run backend tests, frontend build and E2E.
- **User benefit:** predictable setup and confidence before local testing.
- **Effort:** S (hours). **Risk/dependency:** network access, database and browser installation.

### P1 — Validate AI explanation output at the boundary

- **Problem:** AI output is presented alongside source content; static inspection cannot prove the model obeys the no-invention instruction or that malformed/partial responses are always rejected.
- **Why it matters:** fabricated practical advice can mislead learners.
- **Solution:** validate required fields, length and enum-like recommendation values, preserve fallback on any invalid response, and visibly label generated interpretation and source facts.
- **User benefit:** useful explanations with clear provenance.
- **Effort:** M (2–4 days). **Risk/dependency:** provider-specific response variations.

### P1 — Make readiness and freshness explicit

- **Problem:** feeds are periodic and can fail independently; no live feed verification was possible in this audit.
- **Why it matters:** stale content can be mistaken for current developments.
- **Solution:** expose last successful refresh time/feed status and publication date; avoid “latest” wording when timestamps are missing or old.
- **User benefit:** quick understanding of how current the feed is.
- **Effort:** S–M (1–3 days). **Risk/dependency:** refresh-result API and UI changes.

### P2 — Personalize the usefulness decision

- **Problem:** app has a profile but does not yet evidence configurable interests/background affecting article relevance.
- **Why it matters:** generic usefulness advice recreates the information overload the product intends to reduce.
- **Solution:** optional topic/goal preferences and transparent relevance explanations; keep a neutral default.
- **User benefit:** more relevant updates without forcing a learning path.
- **Effort:** M (4–7 days). **Risk/dependency:** preference schema and explanation prompt/versioning.

### P2 — Reduce operational ambiguity

- **Problem:** admin refresh is protected only when `ADMIN_API_KEY` is configured; local mode is intentionally open. CORS defaults include local origins.
- **Why it matters:** deploying development defaults can expose refresh operations or allow unintended origins.
- **Solution:** document a production profile that requires admin key and explicit origins; validate configuration at startup for non-local profiles.
- **User benefit:** safer operation and clearer deployment setup.
- **Effort:** S (1–2 days). **Risk/dependency:** deployment environment conventions.

## Proposed dashboard and navigation

Keep the five existing primary destinations: **Home, Discover, Learn, Saved, Profile**. On Home, show a compact “Today’s AI updates” list with date/source, category, one-line summary, usefulness label and a clear “Why it matters” entry point. Follow with one optional daily learning task. Keep tools and broader learning content in their existing destinations rather than adding more dashboard cards.

## Audit limits

This is a source-level audit plus attempted commands, not a successful end-to-end audit. Live RSS, database migrations against PostgreSQL, AI provider behavior, API latency, real mobile layout, Android packaging and Docker startup remain unverified.
