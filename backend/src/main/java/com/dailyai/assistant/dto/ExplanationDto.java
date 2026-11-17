package com.dailyai.assistant.dto;

import com.dailyai.assistant.entity.ArticleExplanation;

import java.time.Instant;

public record ExplanationDto(
        String generatedBy,
        Instant generatedAt,
        String whatHappened,
        String technology,
        String importance,
        String howItWorks,
        String whereUsed,
        String fresherLearning,
        String practicalExample,
        String relatedTechnologies
) {
    public static ExplanationDto from(ArticleExplanation explanation) {
        if (explanation == null) {
            return null;
        }
        return new ExplanationDto(
                explanation.getGeneratedBy().name(),
                explanation.getGeneratedAt(),
                explanation.getWhatHappened(),
                explanation.getTechnology(),
                explanation.getImportance(),
                explanation.getHowItWorks(),
                explanation.getWhereUsed(),
                explanation.getFresherLearning(),
                explanation.getPracticalExample(),
                explanation.getRelatedTechnologies()
        );
    }
}
