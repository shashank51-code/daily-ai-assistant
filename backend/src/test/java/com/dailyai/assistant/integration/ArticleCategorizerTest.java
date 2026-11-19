package com.dailyai.assistant.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleCategorizerTest {

    private final ArticleCategorizer categorizer = new ArticleCategorizer();

    @Test
    void titleKeywordsWinOverDescription() {
        String slug = categorizer.categorize(
                "New AI agent framework ships tool calling",
                "A tutorial for beginners.",
                "latest-ai-news");
        assertThat(slug).isEqualTo("ai-agents");
    }

    @Test
    void fallsBackToFeedCategoryWhenNothingMatches() {
        String slug = categorizer.categorize("Company picnic photos", "Nothing technical here.", "ai-startups");
        assertThat(slug).isEqualTo("ai-startups");
    }

    @Test
    void usesLatestNewsWhenNoFallbackIsConfigured() {
        assertThat(categorizer.categorize("Unrelated", null, null)).isEqualTo("latest-ai-news");
    }

    @Test
    void scholarlyAbstractFromArxivWinsOverIncidentalToolWords() {
        String slug = categorizer.categorize(
                "When Does Confidence Reveal Problem Difficulty?",
                "arXiv:2611.19173v1 Announce Type: new Abstract: We investigate confidence signals and tools for evaluation.",
                "ai-research");
        assertThat(slug).isEqualTo("ai-research");
    }

    @Test
    void developerApiReleaseIsClassifiedAsATool() {
        assertThat(categorizer.categorize("Spring AI releases a new Java API",
                "The API is available for developers.", "latest-ai-news")).isEqualTo("ai-tools");
    }

    @Test
    void explicitNewModelReleaseIsClassifiedAsAModel() {
        assertThat(categorizer.categorize("OpenAI releases a new language model",
                "The model is available today.", "generative-ai")).isEqualTo("ai-models");
    }

    @Test
    void companyFundingAnnouncementIsClassifiedAsBusinessNews() {
        assertThat(categorizer.categorize("AI startup raises $40 million in Series B",
                "The company said the round will fund its business.", "latest-ai-news"))
                .isEqualTo("ai-startups");
    }
}
