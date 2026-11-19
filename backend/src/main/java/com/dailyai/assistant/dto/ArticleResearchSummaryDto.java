package com.dailyai.assistant.dto;

/** Exact source excerpts plus a cautious note about beginner developer applicability. */
public record ArticleResearchSummaryDto(
        String beginnerSummary,
        String researchProblem,
        String investigation,
        String mainFinding,
        String whyItMatters,
        String beginnerDeveloperRelevance,
        String plainLanguageTerms,
        String evidenceNote
) {
}
