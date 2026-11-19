package com.dailyai.assistant.service;

import com.dailyai.assistant.config.AppProperties;
import com.dailyai.assistant.entity.Article;
import com.dailyai.assistant.entity.ArticleExplanation;
import com.dailyai.assistant.integration.ExplanationDraft;
import com.dailyai.assistant.integration.ExplanationProvider;
import com.dailyai.assistant.repository.ArticleExplanationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ExplanationService {

    private static final Logger log = LoggerFactory.getLogger(ExplanationService.class);
    private static final String UNKNOWN = "Not available in the provided text.";

    private final ArticleExplanationRepository explanationRepository;
    private final ExplanationProvider explanationProvider;
    private final ObjectMapper objectMapper;
    private final int retryAfterMinutes;

    public ExplanationService(ArticleExplanationRepository explanationRepository,
                              ExplanationProvider explanationProvider,
                              ObjectMapper objectMapper,
                              AppProperties properties) {
        this.explanationRepository = explanationRepository;
        this.explanationProvider = explanationProvider;
        this.objectMapper = objectMapper;
        this.retryAfterMinutes = Math.max(0, properties.getAi().getRetryAfterMinutes());
    }

    /**
     * Builds a truthful offline explanation using only the feed data. It never invents
     * technical details; fields the feed cannot support say so explicitly.
     */
    @Transactional
    public ArticleExplanation createBasic(Article article) {
        Optional<ArticleExplanation> existing = explanationRepository.findByArticleId(article.getId());
        if (existing.isPresent()) {
            return existing.get();
        }
        ArticleExplanation explanation = new ArticleExplanation();
        explanation.setArticle(article);
        fillBasic(explanation, article);
        explanation.setGeneratedBy(ArticleExplanation.GeneratedBy.BASIC);
        explanation.setGeneratedAt(Instant.now());
        return explanationRepository.save(explanation);
    }

    @Transactional
    public ArticleExplanation getOrCreate(Article article, boolean allowAi) {
        Optional<ArticleExplanation> existing = explanationRepository.findByArticleId(article.getId());
        if (existing.isEmpty()) {
            ArticleExplanation created = createBasic(article);
            if (allowAi) {
                return upgradeWithAi(created, article, false);
            }
            return created;
        }

        ArticleExplanation explanation = existing.get();
        if (explanation.getGeneratedBy() == ArticleExplanation.GeneratedBy.BASIC) {
            // Refresh old BASIC rows using current source-grounded templates so persisted
            // pre-Phase-4 generic category claims are not shown on existing articles.
            fillBasic(explanation, article);
            explanationRepository.save(explanation);
            if (allowAi) {
                return upgradeWithAi(explanation, article, false);
            }
        }
        return explanation;
    }

    @Transactional
    public ArticleExplanation regenerateWithAi(Article article) {
        ArticleExplanation explanation = explanationRepository.findByArticleId(article.getId())
                .orElseGet(() -> createBasic(article));
        return upgradeWithAi(explanation, article, true);
    }

    public boolean isAiEnabled() {
        return explanationProvider.isEnabled();
    }

    private ArticleExplanation upgradeWithAi(ArticleExplanation explanation, Article article, boolean forced) {
        if (!explanationProvider.isEnabled()) {
            return explanation;
        }
        if (!forced && recentlyAttempted(explanation)) {
            log.debug("Skipping AI retry for article {}: attempted at {}", article.getId(),
                    explanation.getAiAttemptedAt());
            return explanation;
        }

        explanation.setAiAttemptedAt(Instant.now());
        explanation.setAiAttempts(explanation.getAiAttempts() + 1);
        Optional<ExplanationDraft> draft = explanationProvider.generateExplanation(article)
                .filter(ExplanationDraft::isComplete)
                .filter(value -> value.hasSourceEvidence(article))
                .filter(value -> value.hasGroundedSourceFact(article));

        if (draft.isEmpty()) {
            // Persist the attempt timestamp so a broken provider does not spend budget
            // again on every page load. The BASIC text stays visible.
            explanationRepository.save(explanation);
            return explanation;
        }

        ExplanationDraft value = draft.get();
        explanation.setWhatHappened(value.whatHappened());
        explanation.setTechnology(value.technology());
        explanation.setImportance(value.importance());
        explanation.setHowItWorks(value.howItWorks());
        explanation.setWhereUsed(value.whereUsed());
        explanation.setFresherLearning(value.fresherLearning());
        explanation.setPracticalExample(value.practicalExample());
        explanation.setRelatedTechnologies(value.relatedTechnologies());
        explanation.setWhoBenefits(value.whoBenefits());
        explanation.setUseCases(value.useCases());
        explanation.setTechnicalExplanation(value.technicalExplanation());
        explanation.setSourceEvidence(value.sourceEvidence());
        explanation.setClaimsJson(toJson(value.supportedClaims(article)));

        ExplanationDraft.Metadata metadata = value.metadata() == null
                ? ExplanationDraft.Metadata.none() : value.metadata();
        explanation.setProvider(metadata.provider());
        explanation.setModel(metadata.model());
        explanation.setInputTokens(metadata.inputTokens());
        explanation.setOutputTokens(metadata.outputTokens());
        explanation.setLatencyMs((int) metadata.latencyMs());

        explanation.setGeneratedBy(ArticleExplanation.GeneratedBy.AI);
        explanation.setGeneratedAt(Instant.now());
        log.info("Generated AI explanation for article {} with {} supported claims",
                article.getId(), value.supportedClaims(article).size());
        return explanationRepository.save(explanation);
    }

    private boolean recentlyAttempted(ArticleExplanation explanation) {
        Instant attemptedAt = explanation.getAiAttemptedAt();
        if (attemptedAt == null) {
            return false;
        }
        Duration window = Duration.ofMinutes(retryAfterMinutes);
        return retryAfterMinutes > 0 && attemptedAt.plus(window).isAfter(Instant.now());
    }

    private String toJson(List<ExplanationDraft.Claim> claims) {
        try {
            return objectMapper.writeValueAsString(claims);
        } catch (Exception ex) {
            log.warn("Could not serialise claim evidence: {}", ex.getMessage());
            return null;
        }
    }

    private void fillBasic(ArticleExplanation explanation, Article article) {
        String slug = article.getCategory() == null ? "latest-ai-news" : article.getCategory().getSlug();
        String description = article.getDescription();

        explanation.setWhatHappened(description != null && !description.isBlank()
                ? description
                : "The feed published this item without a description. The title is: " + article.getTitle());

        explanation.setTechnology(UNKNOWN);

        explanation.setImportance("ai-research".equals(slug)
                ? "The abstract reports a research result, not proof that the result is ready to use in a product. "
                    + "Read the study design and limitations before applying it."
                : "Tracking this helps you see how the AI industry is moving and which skills are becoming useful. "
                    + "Read the original article before relying on any numbers or claims.");

        explanation.setHowItWorks(UNKNOWN);

        explanation.setWhereUsed(UNKNOWN);

        explanation.setFresherLearning(switch (slug) {
            case "generative-ai" -> "Learn how LLMs work, then practise prompt engineering and build one small "
                    + "summarisation feature.";
            case "ai-models" -> "Learn the difference between a base model and an instruction tuned model, and how "
                    + "context windows and tokens affect cost.";
            case "ai-agents" -> "Learn agents, tool calling and the plan act observe loop. Add step limits and logging "
                    + "to every agent you build.";
            case "deep-learning" -> "Learn neural networks and transformers before going deeper into this topic.";
            case "machine-learning" -> "Learn supervised learning, train test splits and evaluation metrics.";
            case "ai-research" -> "Read the abstract and conclusion first, then check the dataset and evaluation setup.";
            case "ai-tools" -> "Try the tool on a small task and note what it does well and where it fails.";
            case "ai-startups" -> "Follow the product, not just the funding. Ask what problem the product solves.";
            case "tutorials" -> "Follow the tutorial, then rebuild the same thing without looking at the code.";
            default -> "Open the original article and write a three line summary in your own words.";
        });

        explanation.setPracticalExample("A safe next step is to open the original article, summarize its main claim, "
                + "and check the source details before applying it. This is a reading exercise, not a claimed product use case.");

        explanation.setRelatedTechnologies(UNKNOWN);

        explanation.setWhoBenefits("Readers following this topic. The feed does not identify a specific audience.");

        explanation.setUseCases("The feed does not establish a concrete use case. Read the original article before applying it.");

        explanation.setTechnicalExplanation(UNKNOWN);

        // BASIC text is publisher text plus transparent templates; there are no claimed
        // AI interpretations and therefore no claim-level evidence to persist.
        explanation.setClaimsJson(null);
        explanation.setSourceEvidence(null);
        explanation.setProvider(null);
        explanation.setModel(null);
        explanation.setInputTokens(null);
        explanation.setOutputTokens(null);
        explanation.setLatencyMs(null);
    }
}
