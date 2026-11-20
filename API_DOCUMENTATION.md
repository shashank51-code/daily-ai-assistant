# API Documentation

Base URL: `http://localhost:8080`. All responses are JSON. Null fields are omitted from
responses (`spring.jackson.default-property-inclusion: non_null`), for example an unset
`completedAt` or `publishedAt` is absent rather than `null`. Errors use one shape:

```json
{
  "timestamp": "2026-11-17T04:55:44Z",
  "status": 404,
  "error": "Not Found",
  "message": "Article 99 was not found",
  "path": "/api/news/99",
  "details": null
}
```

`details` is a list of field validation messages for 400 responses. Status codes used:
200, 201, 204, 400, 401, 403, 404, 409, 429, 500.

## Authentication and access

Authentication uses an HttpOnly session cookie. Browser and Capacitor clients must send
credentials (Axios `withCredentials: true`). Mutating cookie-authenticated requests require
the `X-XSRF-TOKEN` header from `GET /api/auth/csrf`.

| Endpoint | Purpose |
|----------|---------|
| `GET /api/auth/csrf` | Token + header name for CSRF protection |
| `POST /api/auth/register` | `{ name, email, password, confirmPassword }`; creates and signs in |
| `POST /api/auth/login` | `{ email, password }`; generic 401 on failure, 429 when throttled |
| `POST /api/auth/logout` | Invalidates the session and expires the cookie |
| `GET /api/auth/me` | Current `{ id, name, email, role }`; 401 when signed out |

Public read-only endpoints: `GET /api/news/**`, `/api/categories/**`, `/api/tools/**`.
`GET /api/dashboard`, `/api/profile/**`, `/api/bookmarks/**` and `/api/learning/**` require a
session and are scoped to its user. Admin endpoints require `ROLE_ADMIN` or a valid
`X-Admin-Key` header and return 401/403 otherwise.

## News

### `GET /api/news`
| Param | Default | Notes |
|-------|---------|-------|
| `category` | all | category slug, e.g. `generative-ai` |
| `sort` | `newest` | `newest`, `oldest`, `title` |
| `page` | 0 | zero-based |
| `size` | 12 | max 50 |

Returns `PageResponse<ArticleDto>`:

```json
{
  "content": [{
    "id": 81, "title": "...", "url": "https://...", "description": "...",
    "source": "...", "publishedAt": "2026-11-16T23:32:32Z",
    "category": "ai-startups", "categoryName": "AI Startups",
    "imageUrl": null, "bookmarked": false
  }],
  "page": 0, "size": 12, "totalElements": 160, "totalPages": 14, "last": false
}
```

`publishedAt` can be `null` when the feed supplied no date; dates are never fabricated.

### `GET /api/news/latest?limit=5`
Returns up to 50 newest `ArticleDto` objects.

### `GET /api/news/{id}`
Returns `ArticleDetailDto` including `explanation`. For a signed-in reader it records the read
in that user's reading history and personalizes usefulness using their interests. Anonymous
readers get the public article without a history row. 404 if missing.

### `GET /api/news/category/{category}`
Same as `/api/news` filtered to a slug. 404 for an unknown category.

### `GET /api/news/search?q=agent&category=ai-agents&sort=newest&page=0&size=12`
Case-insensitive search over title, description and source.

## Categories

`GET /api/categories` → `[{ "id": 6, "name": "AI Agents", "slug": "ai-agents", "description": "...", "articleCount": 4 }]`

## Learning

### `GET /api/learning/roadmap`
```json
{
  "levels": [{
    "level": 1, "levelName": "Fundamentals",
    "topics": [{
      "id": 1, "level": 1, "levelName": "Fundamentals",
      "title": "Introduction to AI", "slug": "introduction-to-ai",
      "explanation": "...",
      "prerequisites": ["..."], "resources": ["..."], "exercises": ["..."],
      "projectIdeas": ["..."], "interviewQuestions": ["..."],
      "orderIndex": 1, "status": "NOT_STARTED", "completedAt": null
    }],
    "completedTopics": 0, "totalTopics": 4
  }],
  "completedTopics": 0, "totalTopics": 26, "progressPercent": 0
}
```

### `GET /api/learning/topics?level=1`
List of `LearningTopicDto`; omit `level` for all.

### `GET /api/learning/topics/{id}`
Single topic with the user's status. 404 if missing.

### `POST /api/learning/progress`
```json
{ "topicId": 1, "status": "IN_PROGRESS" }
```
`status` is `NOT_STARTED`, `IN_PROGRESS` or `COMPLETED`. Completing sets `completedAt`;
any other status clears it. Returns:
```json
{ "topicId": 1, "topicTitle": "Introduction to AI", "status": "COMPLETED",
  "completedAt": "2026-11-17T04:55:44Z", "updatedAt": "2026-11-17T04:55:44Z" }
```

### `GET /api/learning/progress`
All progress rows for the current user, newest update first.

### `GET /api/learning/daily-plan`
`{ "days": [{ "dayNumber": 1, "tasks": [...], "completedTasks": 0, "totalTasks": 4 }],
   "completedTasks": 0, "totalTasks": 28, "progressPercent": 0 }`

### `POST /api/learning/daily-plan/{taskId}?completed=true`
Marks a task complete (`true`) or pending (`false`). Returns `DailyTaskDto`.

## Tools

- `GET /api/tools?category&q&page=0&size=20` → `PageResponse<ToolDto>`
- `GET /api/tools/{id}` → `ToolDto` (404 if missing)
- `GET /api/tools/categories` → `["AI Coding Assistants", ...]`

`ToolDto` includes `features` (array), `website`, `pricing`, `freeAlternatives`, `trending`,
`pricingVerifiedOn` and `bookmarked`.

## Bookmarks

- `GET /api/bookmarks?itemType=ARTICLE` — omit filter for all; returns `BookmarkDto[]`.
- `POST /api/bookmarks` — `{ "itemType": "ARTICLE", "itemId": 81 }` → 201 with `BookmarkDto`.
  Repeating the request returns the existing bookmark. Unknown items → 404.
- `DELETE /api/bookmarks/{id}` → 204.
- `DELETE /api/bookmarks?itemType=ARTICLE&itemId=81` → 204 (used by the UI toggle).

## Dashboard

`GET /api/dashboard` returns greeting, user name, date label, briefing, `topUpdates`,
`latestNews`, `trendingTools`, `recentModels`, `todayTask`, `continueLearning`, `streakDays`
and a `stats` object (`articlesToday`, `articlesTotal`, `completedTopics`, `totalTopics`,
`topicProgressPercent`, `planProgressPercent`, `bookmarks`).

## Profile

- `GET /api/profile` — name, email, streak, articles read, bookmarks, topic totals, plan
  percent and the 20 most recent history entries.
- `PUT /api/profile` — `{ "name": "New Name" }`, validated non-blank ≤120 characters.
- `GET /api/profile/history?limit=20` — `HistoryDto[]`.

## Admin

| Endpoint | Purpose |
|----------|---------|
| `POST /api/admin/news/refresh` | Fetch all feeds now |
| `POST /api/admin/articles/{id}/explain` | Regenerate an explanation with AI (400 if AI disabled) |
| `GET /api/admin/status` | Feed count, running flag, last run, AI flag, key requirement |

Every `/api/admin/**` endpoint requires `ROLE_ADMIN` (an authenticated admin account) or a
valid `X-Admin-Key: <key>` header. A normal user receives 403; missing or wrong credentials
receive 401. The header bypass is stateless and is exempt from CSRF; a cookie-authenticated
admin still needs the CSRF token for POST requests.

Refresh response:

```json
{
  "finishedAt": "2026-11-17T04:55:32Z",
  "feedsAttempted": 6, "feedsSucceeded": 6, "feedsFailed": 0,
  "itemsRead": 2720, "saved": 30, "duplicates": 130, "skippedInvalid": 0,
  "errors": []
}
```

A running refresh returns 400 rather than starting a second overlapping run.

## Operational

- `GET /actuator/health` — liveness/readiness.
- CORS origins come from `CORS_ALLOWED_ORIGINS`; methods GET/POST/PUT/PATCH/DELETE/OPTIONS.
