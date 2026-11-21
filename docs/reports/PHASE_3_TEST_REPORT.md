# Phase 3 Test Report

**Date:** 17 Nov 2026. No external or paid AI provider was configured or called.

## Automated checks

| Check | Result |
|---|---|
| Backend `mvn test` in Maven 3.9 / Java 17 container | PASS — 51 tests, 0 failures, errors or skips. |
| Frontend `npm run build` | PASS — Vite 5.4.21, 111 modules transformed. |
| Docker Compose production builds | PASS — backend and frontend images built; all services healthy. |
| Flyway migration on existing PostgreSQL | PASS — schema advanced to version 7; existing profile preserved. |

New unit coverage includes neutral/no-preference behavior, matching and nonmatching interests, Learn Now versus Explore Later, missing article category/metadata, empty preference reset, unknown-interest rejection, missing publisher description, incomplete AI JSON and absent/unsupported source quote.

## Live integration/UI checks

Using Playwright with local Chrome against the running application:

- Saved **Generative AI** from Profile; API returned `GENERATIVE_AI`.
- Opened article 1 and confirmed its recommendation changed to **Learn now**, with “Matches your interests: Generative AI.” API recommendation matched the UI.
- Reset interests; API returned an empty preference list and the article returned to neutral **Explore later** with an explicit reason.
- Confirmed the profile name remained “Shashank.”
- Captured [article usefulness screenshot](C:/Users/sanja/Downloads/daily-ai-assistant/phase3-usefulness.png).

Final database check: Flyway version 7; preferences stored empty after reset. The live article endpoint returned a nonempty neutral recommendation reason. AI status remained disabled.

## AI/provider status

AI is disabled in the running local environment. The backend suite uses a local mock HTTP server with a test-only key string to verify parsing, timeout and failure behavior; it is not a real provider test. The actual provider remains **BLOCKED/unverified**. The BASIC path remains active and visibly labeled in article detail.

## Remaining work

- Test a real provider only if the user configures credentials; this was not done.
- The evidence gate checks one exact quote against the title/description but cannot prove every generated claim.
- Recommendation mappings are category heuristics, not a learned relevance model; user evaluation is still needed.
- Learning-time estimates are omitted until verified data supports them.
- Authentication and per-user preference isolation remain outside this phase.

No deployment or GitHub push occurred.
