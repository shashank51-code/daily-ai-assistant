# Article Classification Report

Date: 2026-11-17

## Reproduced defect and cause

The article “When Does Confidence Reveal Problem Difficulty?” had arXiv abstract metadata but was labeled `AI Tools`. The prior weighted keyword classifier counted incidental tool/model terms in the abstract strongly enough to beat the feed’s research signal. Title-only correction would have been too broad and was avoided.

## Fix

`ArticleCategorizer` now recognizes high-confidence scholarly metadata first (arXiv/DOI/abstract context plus paper signals), ahead of generic tool/model terms. Explicit business funding/acquisition, coding-agent, model-release, and developer API/tool patterns are handled distinctly; the prior weighted fallback remains for ambiguous cases.

Migration V8 corrects only rows whose URL is an arXiv abstract URL and whose description contains `Abstract:`. It leaves non-arXiv content untouched.

## Verification

- Live API `GET /api/news/160`: category `ai-research`, category name `AI Research`.
- Read-only PostgreSQL query: 30 arXiv abstract rows in `ai-research`; no remaining arXiv abstract rows in other categories.
- Regression tests cover an abstract containing incidental tool terms, a developer API, a specific model release, business funding, and research.
- Backend regression suite: **PASS**, 63 tests.

## Boundaries

The rule engine remains heuristic. Ambiguous feeds can still need review, and this change does not claim semantic fact-checking. The article’s source abstract itself contains awkward comparative wording (“strong associations with population difficulty than token-level uncertainty”); the UI preserves that exact sentence as a source excerpt and does not silently rewrite it as a verified claim.
