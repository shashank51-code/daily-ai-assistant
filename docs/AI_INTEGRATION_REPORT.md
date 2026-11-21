# AI Integration Report — Phase 8

Date: 19 November 2026
Provider approved: **Groq free tier (`openai/gpt-oss-20b`)**
Runtime state: `AI_ENABLED=false` by default. No account was created, no credits were purchased, and no real provider request was made during this phase.

## 1. What changed

The existing `ExplanationProvider` / `AiClient` abstraction, retry logic, cooldown and BASIC fallback were kept. Phase 8 extends them rather than replacing them.

### Explanation contract

`ExplanationDraft` now carries:

| Section | Field | Purpose |
| --- | --- | --- |
| What happened? | `whatHappened` | Publisher-grounded summary |
| Technical term | `technology` | One difficult term explained |
| Why does it matter? | `importance` | Practical significance |
| How does it work? | `howItWorks` | Beginner-friendly mechanism |
| Where is it used? | `whereUsed` | Real usage context |
| Learning next step | `fresherLearning` | Only shown when relevant |
| Practical example | `practicalExample` | Concrete, source-supported example |
| Related technologies | `relatedTechnologies` | Adjacent terms |
| Who benefits? | `whoBenefits` | Audience groups, never a named reader |
| How can I use it? | `useCases` | Realistic use supported by the source |
| Technical explanation | `technicalExplanation` | Beginner-friendly concept explanation |
| Evidence | `sourceEvidence` | One exact quote from the publisher text |
| Claims | `claims[]` | `{statement, evidence}` pairs |
| Telemetry | `metadata` | provider, model, tokens, latency |

### Evidence and factuality

- A draft is rejected unless every required section is present.
- `sourceEvidence` must be a literal substring of the article title or feed description.
- Each claim is kept only if its own quote is present in the publisher text. A single valid
  quote is **not** treated as proof for every generated statement.
- Unsupported claims are dropped. If no claim survives, the AI draft is rejected and the
  BASIC explanation remains.
- BASIC text never claims AI interpretation; `generatedBy` reports `AI` or `BASIC`.
- The original article URL is never modified.

### Personalisation and isolation

Article explanations are cached once per article because the source text is the same for
everyone. The `LEARN_NOW` / `EXPLORE_LATER` / `SKIP` verdict and reason are computed at
request time by `ArticleUsefulnessEngine` from the authenticated user's
`users.preferences` (`CurrentUserService` → Spring Security principal).

Consequences:

- No user's interests are sent to the provider, so personal data does not leave the backend.
- No user's preferences are persisted into a shared explanation row.
- One user's preferences cannot change another user's verdict; the engine is stateless and
  the existing `researchRemainsLaterForResearchInterestAndSkipForOtherTechnicalInterest`
  test exercises two preference sets against the same article.
- A user with no selected interests gets a neutral `EXPLORE_LATER` recommendation.

### Reliability

| Failure | Behaviour |
| --- | --- |
| No key / `AI_ENABLED=false` | Provider skipped; BASIC explanation used |
| HTTP 400 / 401 / 403 | No retry, failure recorded, cooldown counted |
| HTTP 429 | Retry with `Retry-After` or exponential backoff |
| HTTP 500 / 503 | Retried, then fallback |
| Network timeout | Retried, then fallback |
| Malformed JSON | One retry without `response_format`, then fallback |
| Incomplete response | Rejected; fallback |
| No grounded claim | Rejected; fallback |
| Retry exhaustion | Fallback, failure reason logged |
| Repeated failures | Provider paused for the cooldown window |

A failed article is not retried on every page view: `ai_attempted_at` makes the service skip
automatic retries for `AI_RETRY_AFTER_MINUTES` (default 360). An explicit regeneration can
bypass the window.

### Cost and performance controls

- `AI_MAX_CALLS_PER_DAY` (default 100) is a hard cap on provider requests, counted per
  attempt, reset at local midnight in `app.timezone`. `0` disables the cap.
- Explanations are generated only when an authenticated user opens an article, never during
  RSS ingestion.
- AI results are cached per article; BASIC rows are upgraded at most once per retry window.
- `400`/`401`/`403` are never retried, preventing duplicate spend on a bad key.
- Telemetry stored per explanation: provider, model, input tokens, output tokens, latency.
- Logs contain provider, model, article id, tokens, latency and a safe failure reason. They
  never contain the API key, the authorization header or the full prompt.

## 2. Database

Additive migration `V10__extend_ai_explanations.sql`:

`who_benefits`, `use_cases`, `technical_explanation`, `claims_json`, `source_evidence`,
`provider`, `model`, `input_tokens`, `output_tokens`, `latency_ms`, `ai_attempted_at`,
`ai_attempts`.

No table was dropped, no row was reset, and the existing unique constraint on `article_id`
is unchanged. `FlywayMigrationTest` now expects 10 successful migrations.

## 3. Configuration

```bash
AI_ENABLED=false
AI_PROVIDER=groq
AI_BASE_URL=https://api.groq.com/openai/v1
AI_API_KEY=
AI_MODEL=openai/gpt-oss-20b
AI_TIMEOUT_MS=30000
AI_MAX_ATTEMPTS=2
AI_RETRY_DELAY_MS=1000
AI_FAILURE_THRESHOLD=2
AI_COOLDOWN_MS=300000
AI_MAX_CALLS_PER_DAY=100
AI_RETRY_AFTER_MINUTES=360
```

The key is backend-only. No key exists in `frontend/src`, `localStorage`, Android assets or
the Docker frontend build. The React bundle never calls the provider.

Switching provider requires only `AI_PROVIDER`, `AI_BASE_URL`, `AI_MODEL` and `AI_API_KEY`.

## 4. API surface

`GET /api/news/{id}` returns the extended `explanation` object:

```json
{
  "generatedBy": "AI",
  "whatHappened": "...",
  "importance": "...",
  "whoBenefits": "...",
  "useCases": "...",
  "technicalExplanation": "...",
  "sourceEvidence": "...",
  "claims": [{ "statement": "...", "evidence": "..." }],
  "provider": "groq",
  "model": "openai/gpt-oss-20b",
  "inputTokens": 0,
  "outputTokens": 0,
  "latencyMs": 0
}
```

The response also keeps `usefulness.recommendation` and
`usefulness.recommendationReason`, which are the authenticated user's personalised verdict.

## 5. UI

The article page now renders **What happened?**, **Why does it matter?**, **Who benefits?**,
**How can I use it?**, **Technical explanation**, and **Should I learn it?**, followed by a
source-grounded claim list that pairs each statement with its publisher quote. AI output is
labelled `AI interpretation`; the model/provider/latency are available in the badge tooltip.

## 6. Verification

- Backend: 83 tests, 0 failures (`mvn test`).
- Frontend: `npm run build` succeeded (103 modules).
- Mock provider: 14 `AiClientTest` scenarios, including 400/401/429/500/503, timeout,
  malformed JSON, unsupported claims, retry exhaustion and daily budget.
- Docker image build: **not run** — Docker daemon is not running in this environment.
- Live API/browser run: **not run** — no Postgres/backend service was started for this phase.
- Real Groq call: **not run** — no `AI_API_KEY` was supplied.

## 7. Rollout

1. Create a Groq account and API key (free tier, no card).
2. Set `AI_API_KEY` in the backend environment and `AI_ENABLED=true`.
3. Open one article and confirm `generatedBy=AI`, `provider=groq`, and a non-zero token count.
4. Keep `AI_MAX_CALLS_PER_DAY=100` until the observed quality and cost are acceptable.
