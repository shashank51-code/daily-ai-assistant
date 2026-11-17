package com.dailyai.assistant.integration;

public record ExplanationDraft(
        String whatHappened,
        String technology,
        String importance,
        String howItWorks,
        String whereUsed,
        String fresherLearning,
        String practicalExample,
        String relatedTechnologies
) {
    public boolean isComplete() {
        return hasText(whatHappened) && hasText(technology) && hasText(importance);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
