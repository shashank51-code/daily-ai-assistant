# Phase 8.2 Test Report

Date: 19 November 2026
Scope: durable AI budget ledger, single-flight generation, claim provenance,
research completeness, API/frontend compatibility and live V11 application.
Real provider calls: **none**.

## 1. Summary

| Check | Status | Detail |
| --- | --- | --- |
| Baseline backend suite | PASS | 83 tests, 0 failures |
| Final backend suite | PASS | **103 tests, 0 failures, 0 errors, 0 skipped** |
| Usage-ledger tests | PASS | 3 tests, including concurrent reservation |
| Mock AI provider tests | PASS | 17 tests, all against a local OpenAI-compatible stub |
| Factuality validator tests | PASS | 7 tests |
| Claim DTO compatibility | PASS | 2 tests: legacy and provenance claims |
| Single-flight tests | PASS | 2 tests |
| Research summary tests | PASS | 4 tests, including article-131 abstract |
| Flyway migration test | PASS | 11 migrations; V11 columns present; ledger empty |
| Frontend production build | PASS | Vite, 103 modules |
| Claim provenance browser fixture | PASS | Both labels and section label rendered |
| E2E script syntax | PASS | `node --check frontend/scripts/e2e-smoke.mjs` |
| Live V11 migration | PASS | PostgreSQL 16.14, existing volume, applied additively |
| Existing data counts | PASS | 1 user, 230 articles, 230 explanations, 3 AI rows |
| Existing AI rows untouched | PASS | Articles 16, 44, 131 each `AI` / `ai_attempts=1` |
| AI disabled during verification | PASS | Container `AI_ENABLED=false` |
| Real Groq/provider call | NOT RUN | Prohibited by this phase |
| Full e2e suite | NOT RUN | Would create a temporary account; focused mocked fixture used instead |
| Commit / push / deploy | NOT DONE | Out of scope |

## 2. Commands and result

```bash
set -o pipefail
JAVA_HOME=$(pwd)/.tools/jdk-17/jdk-17.0.21+9 \
  .tools/maven/apache-maven-3.9.9/bin/mvn -q -f backend/pom.xml test
```

Result: exit code `0`; Surefire reports total **103 tests, 0 failures, 0 errors**.

```bash
npm --prefix frontend run build
```

Result:

```text
vite v8.3.8 building client environment for production...
✓ 103 modules transformed.
✓ built in 164ms
```

```bash
node --check frontend/scripts/e2e-smoke.mjs
```

Result: syntax OK.

## 3. Test classes relevant to Phase 8.2

| Test class | Tests | Result |
| --- | --- | --- |
| `AiClientTest` | 17 | PASS |
| `DatabaseAiUsageBudgetTest` | 3 | PASS |
| `NarrativeFactualityValidatorTest` | 7 | PASS |
| `ExplanationDtoTest` | 2 | PASS |
| `ArticleAiGenerationGuardTest` | 2 | PASS |
| `ArticleServiceTest` | 6 | PASS |
| `ResearchSummaryBuilderTest` | 4 | PASS |
| `FlywayMigrationTest` | 1 | PASS |

Coverage includes:

- ledger reservation before the provider call;
- ledger-unavailable fail-closed behaviour;
- concurrent reservation safety;
- retries consuming separate reservations;
- budget exhaustion stopping further calls;
- single-flight de-duplication;
- anonymous requests not starting generation;
- grounded-claim requirement;
- unsupported claim removal;
- strong-wording downgrade to `INTERPRETATION`;
- research prompt selection and research completeness rejection;
- article-131 deterministic summary;
- legacy claims JSON without `section`/`kind` still parsing.

## 4. Live database verification

Before applying V11:

```text
Flyway versions: 1..10
users=1
articles=230
explanations=230
ai_rows=3
ai_16_44_131=16:AI:1,44:AI:1,131:AI:1
```

The local backend container was rebuilt and restarted. Only the additive V11
migration was applied. The PostgreSQL volume was not removed or reset.

After applying V11:

```text
latest_version=11
ledger_columns=usage_date,attempts,successes,failures,input_tokens,output_tokens,created_at,updated_at
users=1
articles=230
explanations=230
ai_rows=3
ai_ledger_rows=0
ai_16_44_131=16:AI:1,44:AI:1,131:AI:1
claims_16_44_131=16:562,44:588,131:239
```

Backend health became `healthy`; startup logs show:

```text
Migrating schema "public" to version "11 - add ai usage ledger"
Successfully applied 1 migration to schema "public", now at version v11
Started DailyAiAssistantApplication
```

Container environment reported `AI_ENABLED=false`.

## 5. Browser verification

The full e2e suite was not run because it registers a temporary account and
would add rows to the preserved database. Instead, a focused Playwright fixture
served the production build and mocked authentication plus one article response.

Result:

```text
PASS claim provenance fixture: Source fact / App interpretation |
Relates to: How does it work?
```

This verifies that new `section`/`kind` values render as visible provenance
labels. Legacy rows remain covered by `ExplanationDtoTest`.

## 6. Not verified

- Real Groq/provider output quality and JSON adherence: prohibited in this
  phase; all provider paths used a local mock.
- Full authenticated e2e suite: not run to avoid creating test accounts in the
  preserved database.
- Cross-instance single-flight: the guard is process-local; only the
  database-backed budget is cross-instance.
- Multi-instance budget contention under production load: not exercised beyond
  the concurrency test against the ledger.

## 7. Data and secret safety

- No table was dropped, truncated or reset.
- No existing row was deleted or regenerated.
- No `.env` value was read, printed or changed.
- No API key was requested or exposed.
- No commits, pushes or deployments were made.
