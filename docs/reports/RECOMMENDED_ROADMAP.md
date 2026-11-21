# Recommended Roadmap

| Priority | Recommendation | Problem and proposed solution | User benefit | Effort | Dependencies / risks |
|---|---|---|---|---|---|
| P0 | Add authentication before public hosting | Shared profile/data has no authentication; bind data to authenticated users. | Private progress and bookmarks. | L (1–2 weeks) | Identity/session choice; data migration. |
| P1 | Restore and run verification | Current checkout lacks Maven, npm dependencies, local env and running services; restore setup and run backend, build and E2E checks. | Reliable local setup and trusted flows. | S (hours) | Java/Maven, Node, DB/Docker, Chrome, network. |
| P1 | Validate AI output and provenance | Validate response shape/recommendation and fall back on invalid output; distinguish feed facts from AI interpretation. | Lower risk of misleading explanations. | M (2–4 days) | Provider response variation. |
| P1 | Show feed freshness and source status | Add last refresh and honest publication dates; avoid unsupported “latest” claims. | Users can judge timeliness. | S–M (1–3 days) | Refresh metadata/API/UI. |
| P1 | Production config guardrails | Require explicit admin key and allowed origins outside local mode. | Safer deployment defaults. | S (1–2 days) | Profile/environment convention. |
| P2 | Configurable personalization | Capture optional interests/background/goals; use them in relevance explanations. | Fewer irrelevant updates. | M (4–7 days) | Schema and prompt changes; explain ranking. |
| P2 | Optional update learning path | For Learn Now only, offer prerequisites, docs, hands-on task and time estimate. | Clear next step without pressure. | M (4–7 days) | Curated content and source links. |
| P2 | Accessibility/device audit | Verify keyboard, contrast, touch targets, narrow layouts and Android WebView. | Usable mobile experience. | M (2–4 days) | Browser and Android test setup. |
| P3 | Weekly summary / hide controls | Consider only after relevance preferences and usage feedback; support hiding low-value updates and opt-in summaries. | Less information overload. | M (1 week) | Product validation; notification policy. |

## Suggested sequence

1. Restore local setup and establish a passing baseline.
2. Address authentication before any public deployment.
3. Improve freshness and AI output validation.
4. Test and refine the existing Home/Discover experience.
5. Add configurable relevance preferences, then optional learning paths.
6. Consider weekly summaries only after user feedback.

Estimates are rough engineering effort, not calendar commitments. No new feature or external paid service is included in this audit work.
