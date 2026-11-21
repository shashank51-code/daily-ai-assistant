# AI Factuality Test Report — Phase 8

Date: 19 November 2026
Provider: mock OpenAI-compatible server only. No real model was called.

## 1. Controls under test

| Control | Implementation |
| --- | --- |
| Publisher facts separated from interpretation | `generatedBy=AI` labels the whole AI block; claim quotes are shown as publisher evidence; BASIC templates are labelled `Source-based BASIC explanation`. |
| No invented facts | System prompt forbids inventing capabilities, dates, prices, benchmarks, API features, specifications and announcements. |
| Insufficient source | Prompt requires the exact sentence `Not available in the provided text.` |
| Exact source evidence | `sourceEvidence` must be a literal substring of title + description. |
| Claim-level evidence | Each claim carries its own quote; the quote must exist in the publisher text. |
| No quote laundering | A valid `sourceEvidence` does not validate unrelated claims; unsupported claims are removed. |
| No AI text without grounding | If no claim survives, the AI draft is rejected and the BASIC text remains. |
| URL preservation | `Article.url` is never written by the AI path. |

## 2. Automated tests

`AiClientTest` (14 tests) runs the real `AiClient` against a local HTTP mock. No key, network
egress or billable call is involved.

| Test | Scenario | Expected |
| --- | --- | --- |
| `withoutApiKeyTheProviderIsDisabledAndNeverCalled` | No credentials | Disabled, zero requests |
| `generatesDraftWithSectionsClaimsAndUsageMetadata` | Valid grounded response | Draft accepted, one claim, token usage parsed |
| `retriesRateLimitThenSucceeds` | HTTP 429 then 200 | Two attempts, valid draft |
| `rejectsValidJsonWhenAnyRequiredSectionIsMissing` | Missing `whoBenefits` | Draft rejected |
| `rejectsExplanationWithoutQuotedSourceEvidence` | Fabricated global quote | Draft rejected |
| `dropsUnsupportedClaimsInsteadOfTreatingOneQuoteAsProof` | One valid, one invented claim | Only the valid claim retained |
| `rejectsDraftWhenNoClaimCanBeGrounded` | All claim quotes invented | Draft rejected |
| `malformedJsonFallsBackAfterBoundedRetries` | Non-JSON content twice | Empty result after two attempts |
| `badRequestIsNotRetried` | HTTP 400 | One attempt, empty result |
| `unauthorizedIsNotRetried` | HTTP 401 | One attempt, empty result |
| `serverErrorRetryExhaustionFallsBack` | HTTP 500 then 503 | Two attempts, empty result |
| `repeatedServerErrorsOpenCooldownAndSkipFurtherCalls` | HTTP 503 + threshold 1 | Cooldown opens, no further call |
| `dailyBudgetStopsRetriesAndFurtherCalls` | Budget 1, HTTP 500 | One attempt, budget zero, no further call |
| `timeoutDoesNotThrowAndReturnsEmpty` | Delayed response | Empty result, no cooldown below threshold |

`ExplanationServiceTest` (9 tests) additionally verifies:

- a rejected/unusable draft keeps the BASIC explanation;
- an unanchored draft cannot overwrite stored BASIC text;
- missing publisher descriptions do not invent facts;
- research abstracts are described as research evidence, not product readiness;
- a failed article is not retried inside the retry window;
- successful drafts persist claims, provider, model and token counts.

`FlywayMigrationTest` confirms the additive schema applies and the legacy account is not
reset or duplicated.

## 3. Results

```
Tests run: 83, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Frontend build succeeded. No factual-quality claim is made about a real model: the mock
provider proves the guards work, not that Groq's output is accurate.

## 4. Known limits

1. **Quote existence is not semantic support.** The control proves the quote appears in the
   feed, not that the claim follows from it. This is a deliberate, documented limit.
2. **Feed descriptions are short.** A one-sentence description can only support a small
   number of distinct claims; the model may return fewer claims than requested.
3. **No real-provider run.** Accuracy, refusal behaviour and JSON adherence of
   `openai/gpt-oss-20b` remain unverified until a key is supplied.
4. **No full-article fetch.** Only the RSS title and description are used. The app does not
   scrape the publisher page, which limits detail but avoids copyright and reliability
   issues.
5. **BASIC text is template text.** It is truthful about its limits but less specific than a
   good AI explanation.
