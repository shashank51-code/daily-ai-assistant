# Phase 8 Test Report

Date: 19 November 2026
Scope: provider-independent AI explanation upgrade for the Daily AI Assistant.

## 1. Summary

| Check | Status | Detail |
| --- | --- | --- |
| Backend compile | PASS | `mvn -DskipTests compile` |
| Backend tests | PASS | **83 tests, 0 failures, 0 errors** |
| Mock AI provider tests | PASS | 14 `AiClientTest` scenarios |
| Explanation service tests | PASS | 9 tests |
| Flyway migration test | PASS | 10 migrations applied on H2 |
| Frontend production build | PASS | Vite, 103 modules, built in ~0.3 s |
| Docker image builds | NOT RUN | Docker CLI present, daemon not running |
| Live API/browser run | NOT RUN | No Postgres/backend started for this phase |
| Real Groq call | NOT RUN | No `AI_API_KEY` supplied; not billable |
| User isolation | PASS (unit) | Same article, different preference sets produce different verdicts |
| Secret scan | PASS | No provider key in frontend source, Android assets or committed config |

## 2. Commands

```bash
# Backend
cd backend
JAVA_HOME=../.tools/jdk-17/jdk-17.0.21+9 \
  ../.tools/maven/apache-maven-3.9.9/bin/mvn test

# Frontend
cd frontend
npm run build
```

Result tail:

```
Tests run: 83, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

```
vite v8.3.8 building client environment for production...
✓ 103 modules transformed.
✓ built in 278ms
```

## 3. Test coverage added

### AiClientTest

1. Disabled without a key (zero requests).
2. Full draft with sections, claims and token usage.
3. 429 retry then success.
4. Missing required section rejected.
5. Fabricated global evidence rejected.
6. Unsupported claims dropped individually.
7. Draft rejected when no claim is grounded.
8. Malformed JSON retried, then fallback.
9. HTTP 400 not retried.
10. HTTP 401 not retried.
11. HTTP 500 then 503 exhaustion.
12. Cooldown opens and skips further calls.
13. Daily budget stops retries and further calls.
14. Timeout returns empty without throwing.

### ExplanationServiceTest

- BASIC fallback without a provider.
- AI upgrade persists sections, claims, provider, model, tokens and attempt metadata.
- Provider failure keeps BASIC text and records the attempt.
- Incomplete/unanchored draft cannot overwrite BASIC text.
- Failed article is not retried inside the retry window.
- Manual regeneration bypasses the window.
- Regeneration without a provider is safe.
- Missing description does not invent facts.
- Research abstract is not treated as product readiness.

### Existing suites

Auth, bookmarks, news, admin, repository, ingestion, learning, profile preferences,
research summary and migration suites all pass unchanged.

## 4. Not verified

### Docker

`docker --version` reports 29.9.3, but `docker info` fails because the daemon is not running.
Backend and frontend Docker builds, and `docker compose up`, were therefore not executed.
The Dockerfiles were updated only with the new `AI_PROVIDER`, `AI_MODEL` and budget variables.

### Live browser/API

The existing `frontend/scripts/e2e-smoke.mjs` and `auth-e2e.mjs` need a running Postgres,
backend and frontend. Those services were not started. The new UI (claim list, new section
labels, model tooltip) was verified by a successful production build and code review only.

### Real provider

No API key was available and Phase 8 forbids billable calls without approval plus
credentials. The complete request/response path is verified against a local mock that speaks
the OpenAI chat-completions protocol. Real-model JSON adherence, factual quality and latency
remain **unverified**.

## 5. Regression risk

- `ExplanationService` constructor now takes `ObjectMapper` and `AppProperties`; Spring
  injection handles it and all tests pass.
- `ExplanationDraft` constructor changed; only tests and `AiClient` constructed it.
- Schema changes are additive; existing BASIC rows remain valid and are refreshed with the
  current templates when read.
- `AI_MODEL` / `AI_BASE_URL` defaults changed to Groq, but `AI_ENABLED=false` keeps the
  application fully offline by default.
- The frontend tolerates `claims: []` and older payloads without the new fields.

## 6. Recommended next verification

1. Start Docker Desktop, then `docker compose build`.
2. Supply a Groq key to a local environment only, set `AI_ENABLED=true`, open one article and
   confirm `generatedBy=AI`, `provider=groq`, non-zero tokens and grounded claims.
3. Run `npm run test:e2e` and `npm run test:auth` against the running stack.
