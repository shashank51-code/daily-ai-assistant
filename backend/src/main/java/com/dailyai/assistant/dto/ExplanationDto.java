package com.dailyai.assistant.dto;

import com.dailyai.assistant.entity.ArticleExplanation;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

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
        String relatedTechnologies,
        String whoBenefits,
        String useCases,
        String technicalExplanation,
        String sourceEvidence,
        List<ClaimDto> claims,
        String provider,
        String model,
        Integer inputTokens,
        Integer outputTokens,
        Integer latencyMs
) {

    public record ClaimDto(String section, String kind, String statement, String evidence) {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
                explanation.getRelatedTechnologies(),
                explanation.getWhoBenefits(),
                explanation.getUseCases(),
                explanation.getTechnicalExplanation(),
                explanation.getSourceEvidence(),
                parseClaims(explanation.getClaimsJson()),
                explanation.getProvider(),
                explanation.getModel(),
                explanation.getInputTokens(),
                explanation.getOutputTokens(),
                explanation.getLatencyMs()
        );
    }

    private static List<ClaimDto> parseClaims(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<ClaimDto>>() {
            });
        } catch (Exception ignored) {
            // Bad stored JSON must not break the article page.
            return List.of();
        }
    }
}
