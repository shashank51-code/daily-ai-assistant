# AI Relevance Engine Report

Date: 2026-11-17

## Problem

Phase 3 recommendations were determined mostly by feed category and selected profile interests. That could promote funding announcements or poorly described items as technical learning updates, and a wrong feed category could mislead the decision.

## What changed

`ArticleUsefulnessEngine` now classifies feed title and description into update types such as coding tools/agents, developer APIs, research, automation, infrastructure, model releases, product updates, business news, opinion, or unclear/general news. It combines the detected type with category and selected interests, then applies conservative rules:

- Learn Now requires a matching interest, both title and description, and text signaling an actionable development.
- Funding/business and opinion updates are never Learn Now.
- Research is Explore Later for AI Research interests and Skip for unrelated interests.
- Missing or weak evidence yields Explore Later; recommendations never hide articles.
- No selected interests yields a neutral Explore Later recommendation.
- Each recommendation includes a reason, audience, interest relevance and cautious practical guidance.

A Java developer example is explicitly framed as a possible evaluation workflow and notes that the feed may not establish Java support.

## Why this helps

Users can judge the update type and why it was recommended rather than seeing every AI-tagged article treated alike. Conservative defaults reduce pressure to study announcements that have little actionable detail.

## Files modified

- `backend/src/main/java/com/dailyai/assistant/service/ArticleUsefulnessEngine.java`
- `backend/src/test/java/com/dailyai/assistant/service/ArticleUsefulnessEngineTest.java`

## Verification

Added cases for matched coding tools, funding, research, missing descriptions, irrelevant interests and neutral recommendations. Backend suite result is recorded in `PHASE_4_TEST_REPORT.md`.

## Limitations

This remains a transparent keyword/rule engine, not semantic verification. RSS category labels and snippets may be inaccurate. An unclear item may therefore be Explore Later even when the original article is useful. Recommendations do not remove or hide content.
