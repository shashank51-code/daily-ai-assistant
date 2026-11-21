# Phase 8.2 Implementation Report

Date: 19 November 2026
Scope: durable AI budget enforcement, single-flight generation, claim provenance,
research-article handling, and provenance display.
Provider calls during implementation: **none** (`AI_ENABLED=false`, mock provider only).

## 1. Delivered behaviour

### Durable daily usage ledger

- Added additive migration `V11__add_ai_usage_ledger.sql`.
  - Table `ai_usage_daily` keyed by `usage_date`.
  - Counters: `attempts`, `successes`, `failures`, `input_tokens`, `output_tokens`.
  - Non-negative check constraint.
  - No existing table or row is changed.
- `AiClient` now reserves one attempt in the database before every provider HTTP
  call, including retries.
- A reservation uses a short `REQUIRES_NEW` transaction with a pessimistic row
  lock. The lock is released before the provider call.
- Concurrent first-use creation is handled with a bounded retry after a unique
  key conflict.
- The ledger is authoritative and **fail-closed**: if the row cannot be read,
  created or locked, no provider call is made.
- `remainingDailyCalls()` reports `0` when the ledger is unavailable.
- Successes and failures are recorded after the call; telemetry loss does not
  discard an already-returned explanation.

### Single-flight and authenticated generation

- Added `ArticleAiGenerationGuard`, an in-process per-article lock.
- `ArticleService.get` and `AdminService.explain` use the guard, so concurrent
  readers of one article share one generation path.
- Anonymous requests may still read cached AI explanations, but only an
  authenticated request may start a new AI generation.
- The guard is process-local by design; the database ledger remains the
  cross-instance budget authority.

### Claims and factuality

- `ExplanationDraft.Claim` now carries:
  - `section`: the narrative field the claim supports.
  - `kind`: `SOURCE_FACT` or `INTERPRETATION`.
  - existing `statement` and exact `evidence` quote.
- `NarrativeFactualityValidator` deterministically checks narrative fields and
  claims against the supplied title/description.
- Unsupported product names, numbers and technical terms cause the affected
  field to become `"Not available in the provided text."`.
- Strong wording (`ensures`, `guarantees`, `proves`, etc.) downgrades a claim
  from `SOURCE_FACT` to `INTERPRETATION`.
- A draft with no grounded `SOURCE_FACT` claim is rejected.
- Existing `claims_json` rows with only `statement` and `evidence` remain valid.

### Research articles

- Added `ResearchArticleDetector` for category, arXiv source and abstract
  markers.
- `AiClient` selects a research-specific system prompt and user request type
  when the article is research.
- Research drafts must contain real research content, not merely nonblank
  placeholders:
  - at least four substantive sections;
  - at least two of `whatHappened`, `importance`, `howItWorks`,
    `technicalExplanation`.
- `ResearchSummaryBuilder` now extracts whole source sentences for:
  - research problem;
  - investigation;
  - main finding;
  - why it may matter;
  - beginner summary.
- It handles the article-131 abstract, including “relatively little is known”,
  “We study this in 56 interactive worlds” and “All six models we test learn
  less in the chain”.
- Fields that cannot be supported remain explicitly unavailable.

### API and frontend

- `ExplanationDto.ClaimDto` now exposes optional `section` and `kind`.
- Legacy claims parse with those fields as `null`; they are not dropped.
- `ArticlePage.jsx` labels claims as **Source fact** or **App interpretation**
  and shows the related explanation section when supplied.
- Legacy claims without provenance still render exactly as before.
- Added focused CSS for provenance labels.

### Configuration and migration tests

- `docker-compose.yml` now passes:
  - `AI_TIMEOUT_MS`
  - `AI_MAX_ATTEMPTS`
  - `AI_RETRY_DELAY_MS`
  - `AI_FAILURE_THRESHOLD`
  - `AI_COOLDOWN_MS`
- Root `.env` was not edited.
- `FlywayMigrationTest` now expects 11 migrations, checks V11, checks the ledger
  columns, and confirms the new table starts empty.

## 2. Files changed (Phase 8.2)

Backend production:

- `backend/src/main/resources/db/migration/V11__add_ai_usage_ledger.sql`
- `backend/src/main/java/com/dailyai/assistant/entity/AiUsageDaily.java`
- `backend/src/main/java/com/dailyai/assistant/repository/AiUsageDailyRepository.java`
- `backend/src/main/java/com/dailyai/assistant/integration/AiUsageBudget.java`
- `backend/src/main/java/com/dailyai/assistant/integration/DatabaseAiUsageBudget.java`
- `backend/src/main/java/com/dailyai/assistant/integration/AiClient.java`
- `backend/src/main/java/com/dailyai/assistant/integration/ExplanationDraft.java`
- `backend/src/main/java/com/dailyai/assistant/integration/NarrativeFactualityValidator.java`
- `backend/src/main/java/com/dailyai/assistant/integration/ResearchArticleDetector.java`
- `backend/src/main/java/com/dailyai/assistant/service/ArticleAiGenerationGuard.java`
- `backend/src/main/java/com/dailyai/assistant/service/ArticleService.java`
- `backend/src/main/java/com/dailyai/assistant/service/AdminService.java`
- `backend/src/main/java/com/dailyai/assistant/service/ExplanationService.java`
- `backend/src/main/java/com/dailyai/assistant/service/ResearchSummaryBuilder.java`
- `backend/src/main/java/com/dailyai/assistant/dto/ExplanationDto.java`

Frontend:

- `frontend/src/pages/ArticlePage.jsx`
- `frontend/src/styles.css`
- `frontend/scripts/e2e-smoke.mjs`

Configuration:

- `docker-compose.yml`

Tests:

- `backend/src/test/java/com/dailyai/assistant/integration/AiClientTest.java`
- `backend/src/test/java/com/dailyai/assistant/integration/DatabaseAiUsageBudgetTest.java`
- `backend/src/test/java/com/dailyai/assistant/integration/NarrativeFactualityValidatorTest.java`
- `backend/src/test/java/com/dailyai/assistant/dto/ExplanationDtoTest.java`
- `backend/src/test/java/com/dailyai/assistant/service/ArticleAiGenerationGuardTest.java`
- `backend/src/test/java/com/dailyai/assistant/service/ArticleServiceTest.java`
- `backend/src/test/java/com/dailyai/assistant/service/ResearchSummaryBuilderTest.java`
- `backend/src/test/java/com/dailyai/assistant/migration/FlywayMigrationTest.java`

Documentation:

- `docs/PHASE_8_2_IMPLEMENTATION_REPORT.md`
- `docs/PHASE_8_2_TEST_REPORT.md`

## 3. Compatibility and preservation

- V11 is additive only.
- Existing PostgreSQL volume was retained.
- Existing article explanations were not regenerated.
- Articles 16, 131 and 44 remain `generated_by=AI` with `ai_attempts=1`.
- No optional `explanation_version` column was added.
- No AI or provider request was made during implementation or verification.
- No secrets were printed, requested or modified.
- No commit, push or deployment was performed.

## 4. Known limitations

- The single-flight guard is process-local. The database ledger prevents
  over-budget calls across instances, but it does not deduplicate two
  simultaneous generations on different instances.
- The deterministic validator is conservative: supported narrative text may be
  replaced by the unavailable marker if it uses an unlisted technical term.
- No real-provider quality verification was run in this phase.
