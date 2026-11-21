# AI Cost and Usage Report — Phase 8

Date: 19 November 2026
Real provider spend in this phase: **$0.00**. No account, card or API key was configured.

## 1. What is recorded

| Signal | Where |
| --- | --- |
| Provider name | `article_explanations.provider`, success log line |
| Model | `article_explanations.model`, success log line |
| Outcome | Log: `outcome=SUCCESS`; failures log a safe reason |
| Processing time | `article_explanations.latency_ms`, success log line |
| Input tokens | `article_explanations.input_tokens` (from provider `usage.prompt_tokens`) |
| Output tokens | `article_explanations.output_tokens` (from `usage.completion_tokens`) |
| Failure reason | Log only: `HTTP 400`, `HTTP 401`, `HTTP 429`, `HTTP 5xx`, `timeout or network error`, `unusable JSON payload`, `daily call budget reached` |
| Retry count | `article_explanations.ai_attempts` |
| Last attempt | `article_explanations.ai_attempted_at` |

Logs never include the API key, the authorization header or the full prompt. Token counts are
`0` when a provider omits the `usage` object.

## 2. Limits in force

| Limit | Default | Effect |
| --- | --- | --- |
| `AI_MAX_CALLS_PER_DAY` | 100 | Hard cap on provider attempts per local day; `0` disables |
| `AI_MAX_ATTEMPTS` | 2 | Bounds retry spend on 429/5xx/timeout |
| `AI_FAILURE_THRESHOLD` | 2 | Opens cooldown and stops calls during an outage |
| `AI_COOLDOWN_MS` | 300000 | Five-minute pause after repeated failures |
| `AI_RETRY_AFTER_MINUTES` | 360 | A failed article is not re-attempted for six hours |
| Generation trigger | Article view only | RSS ingestion never calls the provider |
| Cache | One row per article | A successful explanation is reused; no repeat spend |
| 4xx handling | No retry | A bad key or bad request cannot burn the budget |

At the cap, expected maximum spend on Groq's paid rate is about **$0.03/day**
(100 × $0.00028) — and on the free tier it is $0.00.

## 3. Planning estimate

Assumption per article: **900 input + 700 output tokens** (system prompt, title/description,
JSON answer). Planning volume: **100 articles/day → 3,000/month**.

| Provider / model | $/article | $/month | ₹/month (≈₹88/$) |
| --- | --- | --- | --- |
| Groq `openai/gpt-oss-20b` | $0.00028 | $0.83 | ≈ ₹73 |
| Groq `openai/gpt-oss-120b` | $0.00056 | $1.67 | ≈ ₹147 |
| OpenAI `gpt-4o-mini` | $0.00056 | $1.67 | ≈ ₹147 |
| Z.ai `glm-5.3-flash` | $0.00049 | $1.46 | ≈ ₹129 |
| Gemini `gemini-3.1-flash-lite` | $0.00128 | $3.83 | ≈ ₹337 |
| DeepSeek `deepseek-flash` (off-peak/peak) | $0.00056–$0.00111 | $1.67–$3.33 | ≈ ₹147–₹293 |

Prices were checked in November 2026 and change frequently; re-check before purchase. These
figures include normal (non-batch) list prices and exclude taxes.

## 4. Why the default is 100 calls/day

- The expected feed volume is below 100 new articles/day.
- A user opening 20 articles costs at most 20 calls, and cached articles cost zero.
- The retry window prevents a failing provider from being hit repeatedly by repeat views.
- The budget counts attempts, so a 429 retry cannot silently double spend.

Raise the cap deliberately after checking `provider`, `model`, `input_tokens`,
`output_tokens` and `latency_ms` for real rows.

## 5. Usage observed

| Metric | Value |
| --- | --- |
| Real provider requests | 0 |
| Real tokens consumed | 0 |
| Real spend | $0.00 |
| Mock-provider HTTP requests (tests) | 16 across 14 tests |
| Explanations generated in tests | mock only |

No cost claim is made about a real provider run because none occurred.

## 6. Recommended operating policy

1. Keep `AI_ENABLED=false` in committed configuration.
2. Enable AI only on the deployment that has the key, not in local or CI environments.
3. Start with `AI_MAX_CALLS_PER_DAY=50`, review a week of telemetry, then raise it.
4. Generate only for articles a user opens; do not pre-generate the whole feed.
5. If a provider starts returning 429 frequently, lower the cap rather than raising retries.
6. Re-check the provider price page monthly; a model rename is a configuration change, not a
   code change.
