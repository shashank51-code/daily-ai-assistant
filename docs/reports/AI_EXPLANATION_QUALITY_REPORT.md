# AI Explanation Quality Report

Date: 2026-11-17

## Problem

The BASIC fallback previously inferred article-specific use cases and related technologies from category alone. Existing stored BASIC explanations could preserve that older generic wording. The AI provider also validates a returned draft against one exact source excerpt; that is a useful grounding anchor but does not prove every sentence.

## What changed

- BASIC explanations now use the publisher feed description for “What happened?” and state when it is missing.
- Unsupported “How it works?”, “Where is it used?”, technical labels and related technologies now say `Not available in the provided text.` rather than asserting category-wide claims.
- The practical example is explicitly a source-checking exercise, not a claimed product use case.
- Existing stored BASIC rows are refreshed from the current template when opened. AI-generated rows are not overwritten by this refresh.
- Article detail labels publisher feed text and interpretation separately and warns that a feed summary cannot verify every factual claim.
- Existing AI configuration remains opt-in; `AI_ENABLED=false` was confirmed in the running container. No provider key was read or exposed, and no real provider was called.
- Existing bounded retry, timeout, cooldown and BASIC fallback behavior remains in place. Automated tests exercise malformed/unsupported draft fallback and simulated 429, 503 and timeout cases.

## Why this helps

Readers can distinguish what the publisher supplied from the app’s interpretation, and stored fallback explanations no longer imply a specific product capability based only on a category label.

## Files modified

- `backend/src/main/java/com/dailyai/assistant/service/ExplanationService.java`
- `backend/src/test/java/com/dailyai/assistant/service/ExplanationServiceTest.java`
- `frontend/src/pages/ArticlePage.jsx`

## Verification

Backend tests exercise missing descriptions, invalid/unanchored provider output, fallback behavior, and retry/error behavior. The live browser check confirmed the provenance warning and detail page sections. See `PHASE_4_TEST_REPORT.md`.

## Remaining limitation

There is no claim-level evidence model. The existing AI response stores explanation fields as plain text and checks only that one returned quote occurs in the source title/description. That check cannot establish that every claim is supported. The UI therefore calls AI content generated interpretation and tells users to check the original source. Persisting evidence against individual claims would require a new response/API and storage design; it was not introduced in this phase. Real provider quality remains untested because no provider is configured.
