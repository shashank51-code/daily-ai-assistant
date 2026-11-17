package com.dailyai.assistant.service;

import com.dailyai.assistant.entity.Article;
import com.dailyai.assistant.entity.ArticleExplanation;
import com.dailyai.assistant.integration.ExplanationDraft;
import com.dailyai.assistant.integration.ExplanationProvider;
import com.dailyai.assistant.repository.ArticleExplanationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class ExplanationService {

    private static final Logger log = LoggerFactory.getLogger(ExplanationService.class);
    private static final String UNKNOWN = "Not available in the provided text.";

    private final ArticleExplanationRepository explanationRepository;
    private final ExplanationProvider explanationProvider;

    public ExplanationService(ArticleExplanationRepository explanationRepository,
                              ExplanationProvider explanationProvider) {
        this.explanationRepository = explanationRepository;
        this.explanationProvider = explanationProvider;
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
                return upgradeWithAi(created, article);
            }
            return created;
        }

        ArticleExplanation explanation = existing.get();
        if (allowAi && explanation.getGeneratedBy() == ArticleExplanation.GeneratedBy.BASIC) {
            return upgradeWithAi(explanation, article);
        }
        return explanation;
    }

    @Transactional
    public ArticleExplanation regenerateWithAi(Article article) {
        ArticleExplanation explanation = explanationRepository.findByArticleId(article.getId())
                .orElseGet(() -> createBasic(article));
        return upgradeWithAi(explanation, article);
    }

    public boolean isAiEnabled() {
        return explanationProvider.isEnabled();
    }

    private ArticleExplanation upgradeWithAi(ArticleExplanation explanation, Article article) {
        if (!explanationProvider.isEnabled()) {
            return explanation;
        }
        Optional<ExplanationDraft> draft = explanationProvider.generateExplanation(article);
        if (draft.isEmpty()) {
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
        explanation.setGeneratedBy(ArticleExplanation.GeneratedBy.AI);
        explanation.setGeneratedAt(Instant.now());
        log.info("Generated AI explanation for article {}", article.getId());
        return explanationRepository.save(explanation);
    }

    private void fillBasic(ArticleExplanation explanation, Article article) {
        String slug = article.getCategory() == null ? "latest-ai-news" : article.getCategory().getSlug();
        String description = article.getDescription();

        explanation.setWhatHappened(description != null && !description.isBlank()
                ? description
                : "The feed published this item without a description. The title is: " + article.getTitle());

        explanation.setTechnology(switch (slug) {
            case "generative-ai" -> "This belongs to Generative AI: models that produce new text, images, audio or code "
                    + "after learning statistical patterns from large amounts of data.";
            case "ai-models" -> "This is about an AI model. A model is a trained file of weights that turns an input "
                    + "into a prediction or a generated output.";
            case "ai-agents" -> "This is about AI agents: language models that can plan steps and call tools in a loop "
                    + "to complete a goal.";
            case "deep-learning" -> "This is about deep learning, which trains multi layer neural networks on data.";
            case "machine-learning" -> "This is about machine learning, where a program learns patterns from examples "
                    + "instead of following hand written rules.";
            case "ai-research" -> "This is a research item. Research usually proposes a method and evaluates it on "
                    + "benchmarks, so the result should be read as a study, not a shipped product.";
            case "ai-tools" -> "This covers AI developer tools and libraries that help teams build with models.";
            case "ai-startups" -> "This is business news about an AI company rather than a technical release.";
            case "tutorials" -> "This is learning material that explains a practical AI technique.";
            default -> UNKNOWN;
        });

        explanation.setImportance("Tracking this helps you see how the AI industry is moving and which skills are "
                + "becoming useful. Read the original article before relying on any numbers or claims.");

        explanation.setHowItWorks(UNKNOWN);

        explanation.setWhereUsed(switch (slug) {
            case "generative-ai" -> "Common uses: chat assistants, code generation, summarisation, image and video tools.";
            case "ai-models" -> "Common uses: chat, coding assistance, search, document analysis and automation.";
            case "ai-agents" -> "Common uses: research assistants, workflow automation and developer tooling.";
            case "deep-learning" -> "Common uses: vision, speech, translation and recommendation systems.";
            case "machine-learning" -> "Common uses: fraud detection, recommendations, forecasting and classification.";
            case "ai-research" -> "Common uses: new model techniques, evaluation methods and training improvements.";
            case "ai-tools" -> "Common uses: building, testing and deploying AI features in real products.";
            case "ai-startups" -> "Common uses: understanding which products and business models are gaining traction.";
            case "tutorials" -> "Common uses: learning a technique you can apply in your own project.";
            default -> UNKNOWN;
        });

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

        explanation.setPracticalExample("Open the original article, summarise it in three lines, then note one new term "
                + "and look it up in the learning roadmap.");

        explanation.setRelatedTechnologies(switch (slug) {
            case "generative-ai" -> "LLMs, embeddings, prompt engineering, RAG";
            case "ai-models" -> "Transformers, tokenisation, inference, fine-tuning";
            case "ai-agents" -> "Tool calling, function calling, multi-agent systems, RAG";
            case "deep-learning" -> "Neural networks, CNN, RNN, transformers";
            case "machine-learning" -> "Supervised learning, evaluation metrics, feature engineering";
            case "ai-research" -> "Benchmarks, datasets, evaluation";
            case "ai-tools" -> "APIs, SDKs, vector databases";
            case "ai-startups" -> "Product development, AI APIs";
            case "tutorials" -> "Python, APIs, project building";
            default -> UNKNOWN;
        });
    }
}
