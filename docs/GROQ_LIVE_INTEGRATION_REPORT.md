# Groq Live Integration Report — Phase 8.1

Date: 19 November 2026
Status: **Live integration PASS; explanation quality PARTIAL (see
`REAL_AI_QUALITY_REPORT.md`).**

## 1. Configuration used

| Setting | Value |
| --- | --- |
| Provider | Groq |
| API shape | OpenAI-compatible chat completions |
| Base URL | `https://api.groq.com/openai/v1` |
| Model | `openai/gpt-oss-20b` |
| Temperature | 0.1 |
| Response format | `json_object` |
| Daily attempt cap | 10 |
| Retry window after failed attempt | 360 minutes |
| Key handling | loaded from root `.env`; never printed or sent to the browser |

No source code changed for this run. The backend was recreated so the container picked up
`AI_ENABLED=true` and the key.

## 2. Live request results

All three approved articles were requested through the authenticated article-detail endpoint,
which is the normal user path that creates or upgrades an explanation.

| Article | Category | HTTP | Result | Claims kept | Input tokens | Output tokens | Provider latency |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 16 — How Amplitude ships product improvements... | ai-tools | 200 | AI | 5 | 525 | 1,653 | 2,642 ms |
| 131 — Agents Cannot Hand Over... | ai-research | 200 | AI | 2 | 824 | 1,297 | 1,884 ms |
| 44 — DeepMind and Singapore... scam detection | tutorials | 200 | AI | 4 | 527 | 1,370 | 2,119 ms |
| **Total** | | | **3/3 success** | **11** | **1,876** | **4,320** | **6,645 ms** |

- Every article succeeded on the **first provider attempt**; no retry, no 429, no 5xx and no
  timeout occurred.
- Persisted `generatedBy` is `AI`, `provider` is `groq`, and `model` is
  `openai/gpt-oss-20b` for all three.
- `ai_attempts` is `1` for each row, confirming one billable attempt per article.
- The BASIC fallback was not used for these three articles.
- Only three provider requests were made. No article outside 16, 131 and 44 was touched.

## 3. Validation path exercised

`AiClient` received a JSON object, parsed it, required all explanation sections, required a
literal `sourceEvidence` quote and required at least one claim with literal evidence.
`ExplanationService` then persisted only `supportedClaims(article)`.

Observed:

- **11 of 11 persisted claims** have evidence that is a literal substring of the article title
  plus feed description.
- `sourceEvidence` is literal for all three articles.
- The claim-level filter therefore did not reject any of the claims that reached storage.
- No malformed JSON, incomplete payload or empty-claim response occurred, so the rejection path
  itself was not triggered by Groq in this run.

## 4. Cached reuse and two-user isolation

Two temporary accounts were created and given different preferences:

- User A: `JAVA_BACKEND`
- User B: `AI_RESEARCH`

Each user fetched the same three articles.

| Article | User A recommendation | User B recommendation | Same cached AI text | Generated at unchanged |
| --- | --- | --- | --- | --- |
| 16 | LEARN_NOW | SKIP | Yes | Yes |
| 131 | SKIP | EXPLORE_LATER | Yes | Yes |
| 44 | EXPLORE_LATER | SKIP | Yes | Yes |

- User B's three requests added **no provider calls**: the success-log count stayed at 3 and
  `ai_attempts` stayed at 1 per article.
- Cached responses returned in 40–62 ms, versus 1.9–2.8 s for generation.
- `generatedAt` was the same stored timestamp for both users (JSON serialisation shows a
  different number of fractional-second digits, but the database row is identical).
- Reading history was separate: user A had 3 rows, user B had 3 rows, both over articles
  16–131. No row was shared.
- A scan of both users' responses found no test email, display name, `JAVA_BACKEND` or
  `AI_RESEARCH` string in the explanation content. Preferences affected only the
  `usefulness` block, which is computed per request.

The temporary accounts and their reading-history rows were removed after verification. The
three AI explanations remain as the global cache for those articles.

## 5. Failure handling

Live failure modes (invalid key, 429, timeout, 5xx, malformed response, exhausted budget) were
**not** exercised in this run, because the approved scope was three successful article calls.
The mock-provider suite covers those paths:

- `AiClientTest`: 14 tests, including 400/401/429/5xx/timeout, malformed JSON, unsupported
  claims, retry exhaustion, cooldown and budget exhaustion.
- `ExplanationServiceTest`: 9 tests covering BASIC fallback, retry window and claim persistence.

No claim is made that Groq's live failure behavior was observed.

## 6. Data and security

- Existing data was not reset. Final counts: **1 user, 160 articles, 160 explanations,
  3 AI explanations**, schema V1–V10.
- No existing user, article, bookmark, progress or explanation row was modified.
- The API key was read only by the backend container; it was not printed, logged, bundled into
  React, written to Android assets or committed.
- Temporary test credentials and accounts were deleted.
- No deployment, push or commit was performed.

## 7. Final state

| Item | Value |
| --- | --- |
| `AI_ENABLED` | `false` |
| `AI_API_KEY` | present in container (not displayed) |
| `AI_PROVIDER` | `groq` |
| `AI_MODEL` | `openai/gpt-oss-20b` |
| `AI_MAX_CALLS_PER_DAY` | `10` |
| Backend health | `{"status":"UP"}` |
| Backend container | healthy |

The three AI rows remain readable while AI is disabled, because an existing AI explanation is
returned from cache and is not regenerated.

## 8. Verdict

The integration works end to end: authentication, provider call, structured parsing,
claim-level evidence, persistence, cache reuse and per-user recommendation isolation all
passed. Quality is not uniformly production-ready; the research article was badly
under-explained. Details and severity are in `REAL_AI_QUALITY_REPORT.md`.
