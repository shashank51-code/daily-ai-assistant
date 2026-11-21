# Real AI Quality Report — Phase 8.1

Date: 19 November 2026
Provider/model: Groq `openai/gpt-oss-20b`
Articles reviewed: 16, 131, 44
Status: **PARTIAL PASS — grounded claims are sound; explanatory quality is uneven.**

## 1. Rubric and result

| Check | Result |
| --- | --- |
| Valid JSON and all required sections present | 3/3 PASS |
| At least 1 claim with literal evidence | 3/3 PASS |
| Every persisted claim has literal publisher evidence | 11/11 PASS |
| `sourceEvidence` is a literal publisher quote | 3/3 PASS |
| No invented dates, prices or benchmarks | 3/3 PASS |
| Sections reflect the supplied title/description | 2/3 PASS (article 131 fails usefulness) |
| Narrative fields stay within the source | 2/3 PASS (articles 16 and 44 contain unsupported detail) |
| Correct placeholder wording | 0/1 applicable (article 131 uses “supplied”, not the required “provided”) |

## 2. Article-by-article review

### Article 16 — Amplitude / OpenAI (ai-tools)

Generated sections were concise and mostly grounded:

- `whatHappened`, `technology`, `howItWorks`, `whereUsed`, `whoBenefits` and `useCases` match
  the title and feed description.
- All 5 claims had literal evidence, including “Wave identifies what to build”, “helps teams
  ship changes with coding agents” and “measures the impact”.
- **Defect:** `technicalExplanation` says “OpenAI models identify features”. The feed says
  **Wave** (Amplitude's product agent) identifies what to build; it does not say the OpenAI
  models perform that step. This is a minor subject misattribution.
- **Minor inference:** the 1–2 day figure comes from the title, while the description only says
  teams ship changes with coding agents. Combining them is reasonable but not a direct
  statement that Wave itself delivers in 1–2 days.

Verdict: usable, with one imprecise sentence.

### Article 131 — Agents Cannot Hand Over... (ai-research)

The feed supplied a **1,543-character abstract** with study design, sample size, results and
limitations. The model returned:

- `whatHappened`, `technology`, `importance`, `howItWorks`, `whereUsed`, `fresherLearning`,
  `practicalExample`, `relatedTechnologies`, `useCases` and `technicalExplanation`: all exactly
  **“Not available in the supplied text.”**
- Only `whoBenefits` was populated.
- 2 claims, both with literal evidence: the title, and “Announce Type: new”.

This is **safe but not useful**. The model did not hallucinate, but it failed the primary task of
explaining a research abstract that was fully present in the prompt. The result gives a reader
almost nothing beyond the title.

Two additional compliance issues:

1. The required fallback string is “Not available in the **provided** text.” The model wrote
   “**supplied**”. The string is still an obvious placeholder, but the prompt instruction was
   not followed exactly.
2. `whoBenefits` names “developers building multi-agent systems” and “AI practitioners
   interested in agent communication”; the abstract discusses agent handoff and multi-agent
   chains, so this is a reasonable interpretation, but it is not a publisher statement.

Verdict: **fails the usefulness bar** for research feeds; factually conservative.

### Article 44 — DeepMind / Singapore scam detection (tutorials)

Most sections are grounded and readable:

- Correctly identifies the collaborators, the on-device Gemma model, private chats and the
  zero-cloud-egress claim.
- All 4 claims have literal evidence.
- **Unsupported narrative detail:** `technicalExplanation` says Gemma models are “transformer
  models” that perform “inference on chat text to identify scam patterns”. The feed says
  “lightweight on-device Gemma models” and “intercept conversational scams”; it does not state
  the architecture or the inference mechanism. These are general AI facts, but they are not in
  the supplied source.
- **Overstatement:** the claim “The approach ensures zero cloud data egress” is stronger than
  the publisher's “with zero cloud data egress”. “Ensures” is an interpretation.
- `whereUsed` adds “in Singapore”; the source names GovTech Singapore, so this is a fair
  contextual inference rather than a new fact.

Verdict: usable, but the technical explanation goes beyond the source.

## 3. Claim-level evidence audit

| Article | Claims stored | Literal evidence | Non-literal evidence | Notes |
| --- | --- | --- | --- | --- |
| 16 | 5 | 5 | 0 | Short quotes, including fragments such as “and measures the impact” |
| 131 | 2 | 2 | 0 | One claim rests only on “Announce Type: new” |
| 44 | 4 | 4 | 0 | “zero cloud data egress” quote supports the topic but not the word “ensures” |
| **Total** | **11** | **11** | **0** | Filter worked as designed |

The claim filter is effective at blocking invented quotes. It does **not** validate the
narrative fields, and it does not detect a claim that is literally quoted but rhetorically
strengthened (“ensures”).

## 4. Personalization quality

The explanation text is intentionally neutral and shared. Personalization is computed
separately from the user's selected interests, and it behaved correctly:

| Article | Java/backend reader | AI research reader |
| --- | --- | --- |
| 16 coding/product update | LEARN_NOW | SKIP |
| 131 research paper | SKIP | EXPLORE_LATER |
| 44 model announcement | EXPLORE_LATER | SKIP |

No user identity or preference string appeared in the AI text. The two users received the same
cached explanation and different recommendations, which is the intended design.

## 5. Findings by severity

| Severity | Finding |
| --- | --- |
| High (usefulness) | Article 131 ignored a long, information-rich abstract and returned placeholders for 10 of 11 sections. |
| Medium | Article 44's `technicalExplanation` adds architecture and inference details not present in the feed. |
| Low | Article 16 attributes feature identification to OpenAI models instead of Wave. |
| Low | Article 131 uses “supplied” instead of the required “provided” placeholder. |
| Low | Rhetorical strengthening (“ensures”) passes the literal-quote filter. |

## 6. Verdict

The integration meets the **safety** goal: no fabricated quotes, no invented dates or prices,
and no cross-user personalization leakage. It does **not** yet meet a consistent **quality**
bar. The next improvement should target abstract handling (summarize explicit study details
instead of defaulting to placeholders) and a check that narrative fields do not introduce
technical facts absent from the source. No code changes were made in this verification run.
