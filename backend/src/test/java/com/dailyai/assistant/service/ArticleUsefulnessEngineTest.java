package com.dailyai.assistant.service;

import com.dailyai.assistant.dto.LearningRecommendation;
import com.dailyai.assistant.entity.Article;
import com.dailyai.assistant.entity.Category;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleUsefulnessEngineTest {
    private final ArticleUsefulnessEngine engine = new ArticleUsefulnessEngine();

    @Test
    void noPreferencesProducesNeutralRecommendationEvenWhenMetadataIsMissing() {
        Article article = new Article();
        article.setTitle("An update without source details");

        var result = engine.evaluate(article, List.of());

        assertThat(result.recommendation()).isEqualTo(LearningRecommendation.EXPLORE_LATER);
        assertThat(result.recommendationReason()).contains("Neutral suggestion");
        assertThat(result.relevanceToInterests()).contains("not set");
        assertThat(result.estimatedLearningMinutes()).isNull();
    }

    @Test
    void matchingCodingAgentInterestGetsPracticalRecommendation() {
        Article article = article("ai-agents");
        article.setTitle("A new AI coding agent launches");
        article.setDescription("The agent is available as a coding tool for developers.");

        var result = engine.evaluate(article, List.of("JAVA_BACKEND", "AI_CODING_AGENTS"));

        assertThat(result.recommendation()).isEqualTo(LearningRecommendation.LEARN_NOW);
        assertThat(result.relevanceToInterests()).contains("AI Coding Agents", "Java and Backend Development");
        assertThat(result.practicalUseCase()).contains("Spring Boot");
    }

    @Test
    void nonMatchingPreferencesCanBeSkippedWithAReason() {
        var result = engine.evaluate(article("ai-research"), List.of("JAVA_BACKEND"));

        assertThat(result.recommendation()).isEqualTo(LearningRecommendation.SKIP);
        assertThat(result.recommendationReason()).contains("does not match");
    }

    @Test
    void matchingResearchInterestIsExploreLaterRatherThanForcedStudy() {
        var result = engine.evaluate(article("ai-research"), List.of("AI_RESEARCH"));

        assertThat(result.recommendation()).isEqualTo(LearningRecommendation.EXPLORE_LATER);
        assertThat(result.recommendationReason()).contains("review the study and its evidence");
    }

    @Test
    void matchingFundingNewsIsNotPromotedToLearnNow() {
        Article article = article("generative-ai");
        article.setTitle("AI company raises $400 million in Series C funding");
        article.setDescription("The startup announced a new funding round led by investors.");

        var result = engine.evaluate(article, List.of("GENERATIVE_AI"));

        assertThat(result.recommendation()).isEqualTo(LearningRecommendation.SKIP);
        assertThat(result.recommendationReason()).contains("business news");
    }

    @Test
    void actionableCodingToolMatchesDeveloperInterest() {
        Article article = article("latest-ai-news");
        article.setTitle("New AI coding agent launches for software teams");
        article.setDescription("The coding agent is available today and can edit code in a project.");

        var result = engine.evaluate(article, List.of("JAVA_BACKEND"));

        assertThat(result.recommendation()).isEqualTo(LearningRecommendation.LEARN_NOW);
        assertThat(result.practicalUseCase()).contains("Spring Boot");
    }

    @Test
    void missingDescriptionCannotGetLearnNowEvenWhenCategoryMatches() {
        Article article = article("ai-agents");
        article.setTitle("A new coding agent releases");
        article.setDescription(null);

        var result = engine.evaluate(article, List.of("AI_CODING_AGENTS"));

        assertThat(result.recommendation()).isEqualTo(LearningRecommendation.EXPLORE_LATER);
        assertThat(result.recommendationReason()).contains("not provide enough detail");
    }

    @Test
    void researchRemainsLaterForResearchInterestAndSkipForOtherTechnicalInterest() {
        Article article = article("ai-research");
        article.setTitle("Research paper introduces a new AI benchmark");
        article.setDescription("The paper evaluates a method on a benchmark dataset.");

        assertThat(engine.evaluate(article, List.of("AI_RESEARCH")).recommendation())
                .isEqualTo(LearningRecommendation.EXPLORE_LATER);
        assertThat(engine.evaluate(article, List.of("JAVA_BACKEND")).recommendation())
                .isEqualTo(LearningRecommendation.SKIP);
    }

    private Article article(String slug) {
        Article article = new Article();
        article.setTitle("A paper about AI");
        article.setDescription("The feed summary does not describe an implementation.");
        article.setCategory(new Category(slug, slug, null));
        return article;
    }
}
