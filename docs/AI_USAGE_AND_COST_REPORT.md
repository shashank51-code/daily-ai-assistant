# AI Usage and Cost Report — Phase 8.1

Date: 19 November 2026
Provider/model: Groq `openai/gpt-oss-20b`
Real requests: **3**
Actual spend: **$0.00** (Groq free tier)
Paid-tier equivalent: **$0.001437**

## 1. Live-test budget

| Control | Value |
| --- | --- |
| Articles allowed | 3 |
| Provider attempts allowed | 10 |
| Provider attempts used | **3** |
| Remaining budget in that process | 7 |
| Retries used | **0** |
| Failed requests | **0** |
| BASIC fallbacks | **0** |

Each article used exactly one attempt. No request was made for a fourth article, and the
second user's cache hits consumed no budget.

## 2. Per-article usage

| Article | Input tokens | Output tokens | Total tokens | Provider latency |
| --- | --- | --- | --- | --- |
| 16 | 525 | 1,653 | 2,178 | 2,642 ms |
| 131 | 824 | 1,297 | 2,121 | 1,884 ms |
| 44 | 527 | 1,370 | 1,897 | 2,119 ms |
| **Total** | **1,876** | **4,320** | **6,196** | **6,645 ms** |

Mean provider latency: **2,215 ms**.

The completion-token counts include the model's reasoning tokens, which is why the 1,297
output tokens for article 131 produced relatively little visible text.

## 3. Cost

Groq list prices used for the paid-tier equivalent:

- Input: $0.075 per 1M tokens
- Output: $0.30 per 1M tokens

Calculation:

```text
input   1,876 / 1,000,000 * $0.075 = $0.0001407
output  4,320 / 1,000,000 * $0.30  = $0.0012960
total                               = $0.0014367
```

Rounded: **$0.001437** (about one seventh of a cent).

Actual charge on the free tier: **$0.00**.

## 4. Free-tier headroom

| Limit | Used | Headroom |
| --- | --- | --- |
| 200,000 tokens/day | 6,196 | 193,804 |
| 8,000 tokens/minute | largest single call ≈ 2,178 | ≈ 5,822 |
| 30 requests/minute | 3 requests, sequential | 27 |

No 429 or rate-limit response occurred.

## 5. Cached reuse

The second user fetched the same three articles after the first user had generated them:

- Additional provider requests: **0**
- Additional tokens: **0**
- Response time: **41–62 ms** per article
- Cached `generatedAt` unchanged and `ai_attempts` remained `1`

This is the expected cost behavior: personalization varies per user, but the explanation is
computed and billed once per article.

## 6. Failure costs

Not exercised live this round. Expected behavior from the mock suite:

| Failure | Expected cost |
| --- | --- |
| 401 invalid key | One rejected request; no completion billed |
| 400 bad request | No retry, no completion |
| 429 rate limit | Retried once within the 10-attempt cap |
| Timeout | Provider may still bill; capped by budget |
| 5xx | Retried once; capped by budget |
| Budget exhausted | No request sent |

## 7. Limitation

`AI_MAX_CALLS_PER_DAY` is enforced by an **in-memory** counter in `AiClient`. Recreating or
restarting the backend resets the counter, so the cap protects against retry storms within a
process but is not a durable daily ledger. The three live calls were made in one process, so
the 3-of-10 accounting above is accurate for this run.

For production, a restart-safe usage ledger (or provider-side spend limit) would be needed if
the budget must survive deployments.

## 8. Final configuration

| Setting | Value |
| --- | --- |
| `AI_ENABLED` | `false` |
| `AI_MAX_CALLS_PER_DAY` | `10` |
| `AI_RETRY_AFTER_MINUTES` | `360` |
| API key | present in `.env`, not displayed |
| Backend health | `UP` |
