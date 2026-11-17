# Architecture

## Overview

```
┌──────────────────────────── React client ────────────────────────────┐
│ HashRouter pages: Home, Discover, Article, Learn, Topic, Bookmarks, Profile
│ Axios client (VITE_API_BASE_URL)  ·  custom CSS light/dark themes
└───────────────────────────────┬──────────────────────────────────────┘
                                │ REST/JSON
┌───────────────────────────────▼──────────────────────────────────────┐
│ Spring Boot 3.5 (Java 17)                                            │
│ Controller → Service → Repository → PostgreSQL                       │
│                                                                     │
│ integration/                     scheduler/                          │
│  RssFeedClient (HTTP, gzip)      NewsFetchScheduler (every 3h)       │
│  RssParser (ROME)                StartupNewsLoader (empty DB only)   │
│  ArticleCategorizer              service/NewsIngestionService        │
│  AiClient (optional)             service/ExplanationService          │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                    ┌───────────▼───────────┐
                    │ PostgreSQL 16         │
                    │ Flyway V1–V5          │
                    └───────────────────────┘
```

## Backend packages

| Package      | Responsibility |
|--------------|----------------|
| `config`     | `AppProperties` for `app.*` settings, CORS configuration |
| `controller` | Thin REST layer, request validation, HTTP status codes |
| `dto`        | Immutable records returned to the client |
| `entity`     | JPA entities with constraints and indexes |
| `repository` | Spring Data JPA interfaces and JPQL search queries |
| `service`    | Transactions and business rules |
| `integration`| RSS HTTP client, ROME parser, categorizer, optional AI client |
| `scheduler`  | Cron feed refresh and first-run loader |
| `exception`  | Domain exceptions and a single `ApiError` response shape |

Controllers never contain business logic. Services own transactions; read methods use
`@Transactional(readOnly = true)`, writes use `@Transactional`.

## Data model

| Table | Purpose | Key constraints |
|-------|---------|-----------------|
| `users` | single local profile | unique email |
| `categories` | taxonomy used by feed fallback/categorizer | unique slug |
| `articles` | collected news | unique URL, indexes on published/fetched/category |
| `article_explanations` | 8-part beginner explanation | one row per article |
| `bookmarks` | saved article/tool/topic | unique (user, item_type, item_id) |
| `learning_topics` | 26 topics across 5 levels | unique slug |
| `learning_progress` | per-user topic status | unique (user, topic) |
| `ai_resources` | AI tool catalogue | unique name |
| `reading_history` | per-user read timestamps | unique (user, article) |
| `daily_tasks` | seven-day learning plan | indexed by day/order |
| `daily_task_progress` | completed daily tasks | unique (user, task) |

All timestamps are `timestamptz`; the app stores UTC and renders in the configured time zone
(`Asia/Kolkata` by default). Flyway owns the schema; Hibernate runs with `ddl-auto: validate`.

## Ingestion flow

1. `NewsFetchScheduler` triggers every third hour; `StartupNewsLoader` triggers once when the
   article table is empty.
2. `NewsIngestionService.refreshAll` guards against overlapping runs with an `AtomicBoolean`.
3. For each configured feed:
   - `RssFeedClient` performs a GET with timeouts and a descriptive User-Agent, then
     decompresses gzip/deflate (some publishers, e.g. DeepMind, compress XML without being asked).
   - `RssParser` uses ROME, strips HTML, deduplicates within the feed and never invents dates.
   - Each entry is validated (title, http/https URL, length), deduplicated in-run and against
     the unique `articles.url` index, categorized and saved.
   - A basic explanation row is created for each new article.
4. Errors are captured per feed in `RefreshResultDto.errors`; the run continues.
5. The refresh result is available in the UI through the Discover refresh button.

## Explanation strategy

`ExplanationService` always creates an offline explanation using only the article title,
description, source and category. Fields the feed cannot support explicitly say
"Not available in the provided text." If AI is enabled, `AiClient` asks an OpenAI-compatible
model for the eight sections as JSON, with a strict no-invention system prompt. On failure,
timeout or malformed JSON the stored offline version is kept. `generatedBy` tells the client
whether the text is `AI` or `BASIC`.

## Frontend structure

- `HashRouter` is used so deep links work when the app is served from the Android WebView.
- `useAsync` centralises loading/error/reload handling; every screen renders skeletons,
  an error state with retry, and an empty state.
- `BookmarkToggle` performs optimistic add/remove and reverts on failure.
- Theme is stored in `localStorage` and applied with `data-theme`; CSS variables power both themes.
- No UI framework or Tailwind: a single custom stylesheet defines tokens, mobile-first layout,
  cards, bottom navigation and safe-area insets.

## Security and data handling

- No secrets are committed; `.env.example` documents required variables.
- The admin API is open in local development but requires `X-Admin-Key` when `ADMIN_API_KEY` is set.
- Feed HTML is stripped before storage and rendering; all original links open externally.
- There is intentionally no authentication in this version. It is a single-profile app, and
  any public deployment must add authentication before exposing personal progress.

## Extension points

- Add feeds in `application.yml`; no code changes required.
- Add another news API by implementing a client that produces `ParsedArticle` objects.
- Swap the AI provider by changing `AI_BASE_URL`/`AI_MODEL` (any compatible API).
- Add authentication by replacing `CurrentUserService` with a request-scoped principal.
