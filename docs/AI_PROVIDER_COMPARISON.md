# AI Provider Comparison — Phase 8

Date: 19 November 2026
Status: **Awaiting user approval.** No provider has been configured and no billable call has been made.

## 1. Current implementation (inspected, not recreated)

| Concern | Current state |
| --- | --- |
| Contract | `ExplanationProvider` — `isEnabled()`, `isAvailable()`, `generateExplanation(Article)` returning `Optional<ExplanationDraft>`; implementations never throw. |
| Provider | `AiClient` — one OpenAI-compatible `/chat/completions` client. Configurable `AI_BASE_URL`, `AI_MODEL`, `AI_API_KEY`. |
| Prompting | Single system prompt + title/source/category/date/feed description. Plain `response_format: {"type":"json_object"}`, `temperature 0.2`. |
| Output fields | `whatHappened`, `technology`, `importance`, `howItWorks`, `whereUsed`, `fresherLearning`, `practicalExample`, `relatedTechnologies`, `sourceEvidence`. |
| Grounding | Every field must be non-blank; `sourceEvidence` must be a literal substring of title + description. Otherwise the draft is rejected. |
| Resilience | 429/5xx/timeouts retried with bounded exponential backoff and `Retry-After`; 400/401/403 fail fast; consecutive failures trigger a cooldown; retries capped at 2 by default. |
| Fallback | `ExplanationService` always stores a source-grounded BASIC explanation first; AI only upgrades an existing row. |
| Caching | One `article_explanations` row per article (`uk_explanations_article`). AI rows are not regenerated on every view. |
| Personalization | `ArticleUsefulnessEngine` computes `LEARN_NOW` / `EXPLORE_LATER` / `SKIP` at request time from the authenticated user's `users.preferences`. |
| Auth isolation | `/api/news/**` is authenticated; preferences come from `CurrentUserService`, never from a request parameter. |
| Configuration safety | `AI_ENABLED=false` by default, key empty by default, secrets only via backend env vars. No key is present in `frontend/src` or Android assets. |
| Tests | `AiClientTest` uses a local mock HTTP provider (success, 429 retry, missing fields, fabricated evidence, unusable JSON, 503 cooldown, timeout). `ExplanationServiceTest` covers fallback and draft rejection. |

### Gaps against Phase 8

1. No structured **claim-level** evidence — only one quote for the whole explanation.
2. No explicit **Who benefits?**, **How can I use it?**, **technical explanation**, or persisted **LEARN_NOW / EXPLORE_LATER / SKIP** verdict with reason.
3. AI output is **global per article**, while Phase 8 asks for a recommendation based on each user's interests. Personalizing the cached AI text would leak one user's preferences to another.
4. No usage/cost telemetry (provider, model, outcome, latency, tokens, failure reason).
5. No generation budget/priority policy; every article view can trigger at most one call per article, but there is no daily cap.
6. `sourceEvidence` is validated but not persisted.

## 2. Requirements used for the comparison

- OpenAI-compatible chat completions so `AiClient` stays provider-independent.
- Reliable JSON / structured output for a fixed schema.
- Transparent per-token pricing; no assumption that anything is free.
- Usable from India without a proxy.
- A no-card development path, because Phase 8 forbids billable calls without approval.
- Low cost at the expected volume: **900 input + 700 output tokens per article** (system prompt, article title/description, interests, JSON answer), **3,000 articles/month** (100/day) as the planning figure.

## 3. Options

Prices are USD per 1M tokens, from provider pricing pages and current (Nov 2026) price trackers. Verify again before purchase.

| Provider / model | Input | Output | Free tier | JSON / structured output | Dev without card | India access | Est. $/article | Est. $/3,000 mo |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **OpenAI `gpt-4o-mini`** | $0.15 | $0.60 | No | `json_object`, `json_schema` (strongest) | No | Yes | $0.00056 | **≈ $1.67** |
| OpenAI `gpt-5.4-nano` | $0.20 | $1.25 | No | Strong | No | Yes | $0.00106 | ≈ $3.17 |
| **Google `gemini-3.1-flash-lite`** | $0.25 | $1.50 | Yes, rate-limited; free-tier prompts may be used to improve Google products | Yes on native API; OpenAI-compat layer is partial | Yes | Yes | $0.00128 | ≈ $3.83 |
| Google `gemini-2.5-flash-lite` | $0.10 | $0.40 | Yes | Yes | Yes | Yes | $0.00037 | ≈ $1.11 — **gated to existing users, not viable for a new project** |
| **Groq `openai/gpt-oss-20b`** | $0.075 | $0.30 | Yes: 30 RPM, 1,000 req/day, 8k TPM, 200k tokens/day | `json_object` | Yes | Yes | $0.00028 | ≈ $0.83 (free tier covers ~125 articles/day) |
| Groq `openai/gpt-oss-120b` | $0.15 | $0.60 | Same shared free limits | `json_object` | Yes | Yes | $0.00056 | ≈ $1.67 |
| **DeepSeek `deepseek-flash`** | $0.15 off-peak / $0.30 peak | $0.60 / $1.20 | No | JSON output supported | No | Yes, but data is processed in China | $0.00056–$0.00111 | ≈ $1.67–$3.33 |
| **OpenRouter** (`:free` models) | $0 | $0 | Yes: 20 RPM, **50 req/day** (1,000/day after a one-time $10 credit purchase) | Per-endpoint; varies by model | Yes | Yes | $0 | $0 (hard cap ≈ 1,500 articles/mo) |
| OpenRouter (paid models) | provider price | provider price | — | Per-endpoint | Needs balance | Yes | provider + 5.5% fee | provider + 5.5% |
| **Z.ai `glm-4.7-flash`** | $0 | $0 | Free model | JSON mode; validate per model | Yes | Yes, but data is processed in China | $0 | ≈ $0 (rate limits apply) |
| Z.ai `glm-5.3-flash` | $0.15 | $0.50 | No | JSON mode | No | Yes | $0.00049 | ≈ $1.46 |

At an indicative ₹88/USD, the recommended paid option costs roughly **₹150 per month** at 100 articles/day. That is the entire cost model; there are no subscriptions or minimums on these pay-as-you-go APIs.

## 4. Trade-offs

**OpenAI `gpt-4o-mini`**
- Pros: already the configured default and already exercised by `AiClientTest`; best-documented structured output; most predictable JSON adherence; strong English explanations; straightforward India billing.
- Cons: no free tier; the model has a published deprecation signal (2027), so the model name should be configurable (it already is).
- Best for: production, where a malformed or fabricated explanation is worse than $2/month.

**Groq `gpt-oss-20b` / `gpt-oss-120b`**
- Pros: zero-cost development with a real provider; OpenAI-compatible; very fast; cheapest paid rate; free tier is enough for this app's volume.
- Cons: the free 8k TPM cap allows only ~5 of our requests per minute, so bulk generation is impractical; open-weight model quality can be less consistent; free-tier availability can change.
- Best for: controlled integration testing and a free demo deployment.

**Google `gemini-3.1-flash-lite`**
- Pros: free tier; large context; good structured output on the native API; low cost at scale.
- Cons: an OpenAI-compatible base URL does not expose Google's full structured-output feature set; free-tier data may be used to improve Google products, which is a privacy consideration for article text; current Flash-Lite line changes model names/prices frequently.
- Best for: teams already on Google Cloud that can use the native SDK.

**DeepSeek `deepseek-flash`**
- Pros: competitive price; 1M context; JSON output; strong reasoning.
- Cons: peak/off-peak pricing makes cost forecasting harder; data residency in China is hard to justify for a user-facing product; no free tier.
- Best for: cost-sensitive experimentation where data residency is acceptable.

**OpenRouter free models**
- Pros: no cost; one key for many models; structured outputs on supported endpoints.
- Cons: 50 requests/day on the free tier is below this app's expected volume; model IDs expire; routing/latency varies; production reliability is not guaranteed.
- Best for: prototyping, not a dependable default.

**Z.ai `glm-4.7-flash` (catalog recommendation, sponsored placement)**
- Pros: genuinely free API models; OpenAI-compatible; India access.
- Cons: sponsored catalog placement is not a quality endorsement; free models are smaller/older; Chinese data processing; JSON-schema reliability should be validated before trusting it.
- Best for: a zero-cost smoke test, not as the sole production provider.

## 5. Recommendation

**Primary: OpenAI `gpt-4o-mini`** (no code change needed; `AI_MODEL` already configurable).

Reasons: factuality and schema adherence matter more than the last fraction of a cent here; the total cost is ~$1.67/month at the planned volume; the existing client, tests and configuration already target this API; and it avoids data-residency ambiguity.

**Secondary for zero-cost development: Groq `openai/gpt-oss-20b`.** Same OpenAI protocol, free 200k tokens/day, so integration and failure-mode tests can run against a real provider without spending money. Keep `AI_ENABLED=false` as the committed default.

The application stays provider-independent: switching provider means changing `AI_BASE_URL`, `AI_MODEL` and `AI_API_KEY` only.

## 6. Proposed implementation (after approval)

1. **Schema** (new Flyway `V10`, additive only — no reset): add `who_benefits`, `use_cases`, `technical_explanation`, `recommendation`, `recommendation_reason`, `claims` (JSON text), `source_evidence`, `provider`, `model`, `input_tokens`, `output_tokens`, `latency_ms`, `status`. Keep existing columns for compatibility.
2. **Provider output**: expand `ExplanationDraft` to the Phase 8 sections plus a claim list; each claim carries `text` + `evidenceQuote`; reject a claim whose quote is not in the feed text and do not present it as fact.
3. **Grounding**: keep the BASIC path and the evidence-substring rule; mark AI interpretation separately from publisher facts; preserve the original URL.
4. **Personalization**: keep article explanations global and cache-friendly; compute the `LEARN_NOW` / `EXPLORE_LATER` / `SKIP` verdict and reason per request from the authenticated user's interests (extend `ArticleUsefulnessEngine`). This prevents cross-user preference leakage and avoids one API call per user.
5. **Cost controls**: only generate on explicit article view (current behaviour), add a configurable daily AI call cap and per-article cost budget, skip low-priority/business/opinion items, and never retry a 4xx.
6. **Telemetry**: log provider, model, outcome, latency, token usage and safe failure reason (no keys, no full prompts). Expose aggregate counters for the cost report.
7. **Tests**: mock-provider tests for 400/401/429/500/503, timeout, malformed/incomplete JSON, unsupported claims, retry exhaustion and fallback; user-isolation tests; frontend build; Docker build. Real-provider test only if credentials are supplied and only a handful of calls.

## 7. Decision needed

Choose one:

- **A. OpenAI `gpt-4o-mini`** — recommended production default.
- **B. Groq `openai/gpt-oss-20b`** — free-tier development and evaluation first.
- **C. Google `gemini-3.1-flash-lite`** — alternative low-cost provider.
- **D. Mock provider only for now** — implement and test everything with no external account.

No account will be created, no credits purchased and no billable request made until you approve one.
