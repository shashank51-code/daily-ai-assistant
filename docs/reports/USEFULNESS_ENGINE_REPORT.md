# Usefulness Engine Report

## What was implemented

Each article response now includes a `usefulness` object with:

- `recommendation`: `LEARN_NOW`, `EXPLORE_LATER`, or `SKIP`.
- `recommendationReason`: always present, including for neutral recommendations.
- `whoBenefits`: a category-level audience description.
- `relevanceToInterests`: a match explanation or an explicit “not set” message.
- `practicalUseCase`: cautious next-step guidance based on category and selected interests.
- `estimatedLearningMinutes`: currently absent/null because the feed does not support a reliable estimate.

The rule-based engine is separate from provider text. With no preferences it returns **Explore later** as a neutral default. With selected interests that do not match an article category it returns **Skip** with a reason. A matching practical topic can return **Learn now**; research and model categories default to **Explore later** even when relevant. This is a transparent category-based heuristic, not a claim that an article is objectively important.

Article cards show the short recommendation and reason. Article detail adds “Is it worth your time?”, who benefits, personal relevance and practical-use guidance. The original source URL/date remain visible. Publisher-provided summaries are explicitly labeled. The existing detailed explanation remains available; the learning next step is shown only for Learn Now.

The existing BASIC explanation remains the fallback and is labeled “Source-based BASIC explanation.” It uses the feed text and marks missing technical details unavailable. No AI provider is enabled by default.

## AI response safeguards

AI drafts must now contain all eight explanation sections as nonblank text plus a `sourceEvidence` quote that exactly occurs in the article title or feed description. Non-string fields, incomplete JSON and unsupported evidence are rejected, leaving the BASIC explanation in place. The prompt continues to prohibit facts absent from the source.

This is a minimum anchoring check, not a general fact verifier: an exact quote does not prove every other generated sentence. The source URL remains available so users can check the publisher. A production factuality guarantee would need stronger claim-by-claim evidence and review.

## Remaining recommendations

| Recommendation | Why it helps | Priority / effort |
|---|---|---|
| Add claim-level citations or source spans for AI explanations | Lets users inspect which source text supports each generated claim. | P1 / M |
| Review category-level usefulness rules with real users | Finds false Learn Now/Skip suggestions before the labels become trusted. | P1 / S–M |
| Curate optional learning time and prerequisites from verified material | Adds useful estimates without guessing from a category. | P2 / M |
| Add a dismiss/feedback signal for recommendations | Gives future relevance tuning evidence beyond broad category matches. | P2 / M |

No paid provider, external service or new notification feature was introduced.
