package com.dailyai.assistant.service;

import com.dailyai.assistant.entity.Article;
import com.dailyai.assistant.entity.ArticleExplanation;
import com.dailyai.assistant.entity.Category;
import com.dailyai.assistant.integration.ExplanationDraft;
import com.dailyai.assistant.integration.ExplanationProvider;
import com.dailyai.assistant.repository.ArticleExplanationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExplanationServiceTest {

    @Mock
    private ArticleExplanationRepository explanationRepository;
    @Mock
    private ExplanationProvider provider;

    private ExplanationService service;

    @BeforeEach
    void setUp() {
        service = new ExplanationService(explanationRepository, provider);
        org.mockito.Mockito.lenient()
                .when(explanationRepository.save(any(ArticleExplanation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void withoutProviderItStillCreatesTheOfflineExplanation() {
        when(provider.isEnabled()).thenReturn(false);
        when(explanationRepository.findByArticleId(1L)).thenReturn(Optional.empty());

        ArticleExplanation explanation = service.getOrCreate(article(), true);

        assertThat(explanation.getGeneratedBy()).isEqualTo(ArticleExplanation.GeneratedBy.BASIC);
        assertThat(explanation.getWhatHappened()).isEqualTo("A short feed description.");
        assertThat(explanation.getTechnology()).contains("Generative AI");
        assertThat(explanation.getHowItWorks()).isEqualTo("Not available in the provided text.");
        assertThat(service.isAiEnabled()).isFalse();
    }

    @Test
    void providerDraftUpgradesTheExplanation() {
        when(provider.isEnabled()).thenReturn(true);
        when(explanationRepository.findByArticleId(1L)).thenReturn(Optional.empty());
        when(provider.generateExplanation(any(Article.class))).thenReturn(Optional.of(new ExplanationDraft(
                "A new model shipped.", "A neural network.", "It is faster.",
                "It predicts tokens.", "Chat assistants.", "Learn transformers.",
                "Build a demo.", "LLMs, RAG")));

        ArticleExplanation explanation = service.getOrCreate(article(), true);

        assertThat(explanation.getGeneratedBy()).isEqualTo(ArticleExplanation.GeneratedBy.AI);
        assertThat(explanation.getWhatHappened()).isEqualTo("A new model shipped.");
        assertThat(explanation.getRelatedTechnologies()).isEqualTo("LLMs, RAG");
        assertThat(explanation.getGeneratedAt()).isNotNull();
    }

    @Test
    void providerFailureKeepsTheOfflineExplanationStored() {
        when(provider.isEnabled()).thenReturn(true);
        ArticleExplanation existing = new ArticleExplanation();
        existing.setId(5L);
        existing.setArticle(article());
        existing.setWhatHappened("Offline text.");
        existing.setGeneratedBy(ArticleExplanation.GeneratedBy.BASIC);
        when(explanationRepository.findByArticleId(1L)).thenReturn(Optional.of(existing));
        when(provider.generateExplanation(any(Article.class))).thenReturn(Optional.empty());

        ArticleExplanation explanation = service.getOrCreate(article(), true);

        assertThat(explanation.getGeneratedBy()).isEqualTo(ArticleExplanation.GeneratedBy.BASIC);
        assertThat(explanation.getWhatHappened()).isEqualTo("Offline text.");
    }

    @Test
    void regenerateWithoutProviderDoesNotThrow() {
        when(provider.isEnabled()).thenReturn(false);
        when(explanationRepository.findByArticleId(1L)).thenReturn(Optional.empty());

        ArticleExplanation explanation = service.regenerateWithAi(article());

        ArgumentCaptor<ArticleExplanation> captor = ArgumentCaptor.forClass(ArticleExplanation.class);
        org.mockito.Mockito.verify(explanationRepository).save(captor.capture());
        assertThat(captor.getValue().getGeneratedBy()).isEqualTo(ArticleExplanation.GeneratedBy.BASIC);
        assertThat(explanation.getGeneratedBy()).isEqualTo(ArticleExplanation.GeneratedBy.BASIC);
    }

    private Article article() {
        Category category = new Category("Generative AI", "generative-ai", null);
        Article article = new Article();
        article.setId(1L);
        article.setTitle("New model ships");
        article.setUrl("https://example.com/a");
        article.setDescription("A short feed description.");
        article.setCategory(category);
        return article;
    }
}
