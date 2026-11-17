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
}
