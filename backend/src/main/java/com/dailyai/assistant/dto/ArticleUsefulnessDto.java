package com.dailyai.assistant.dto;

public record ArticleUsefulnessDto(
        LearningRecommendation recommendation,
        String recommendationReason,
        String whoBenefits,
        String relevanceToInterests,
        String practicalUseCase,
        Integer estimatedLearningMinutes
) {
}
