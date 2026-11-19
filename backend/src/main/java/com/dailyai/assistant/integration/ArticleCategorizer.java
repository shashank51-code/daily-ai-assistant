package com.dailyai.assistant.integration;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Explainable category rules with high-confidence article-type evidence ahead of feed fallback scoring. */
@Component
public class ArticleCategorizer {

    private static final Map<String, List<String>> KEYWORDS = new LinkedHashMap<>();
    private static final Pattern FUNDING_TITLE = Pattern.compile("\\b(funding round|raises? \\$|raised \\$|series [a-f]\\b|seed round|acqui(?:res|red|sition)|ipo)\\b");
    private static final Pattern RESEARCH_MARKER = Pattern.compile("(?is)(arxiv(?:\\.org)?\\s*:\\s*|arxiv\\.org/|doi\\s*:\\s*|announce type:.*abstract:)");
    private static final Pattern RESEARCH_METHOD = Pattern.compile("(?i)\\b(abstract|we investigate|we evaluate|we examine|we study|we propose|we find|experiments?|dataset|preprint)\\b");
    private static final Pattern RELEASE = Pattern.compile("(?i)\\b(release[sd]?|launch(?:es|ed)?|introduc(?:e|es|ed)|announc(?:e|es|ed)|available)\\b");

    static {
        KEYWORDS.put("ai-agents", List.of(
                "ai agent", "ai agents", "agentic", "tool calling", "function calling",
                "multi-agent", "multi agent", "autonomous agent", "agent framework", "mcp server",
                "model context protocol"));
        KEYWORDS.put("ai-models", List.of(
                "new model", "model release", "releases model", "open-weight", "open weights",
                "foundation model", "gpt-5", "gpt-4", "gemini 2", "claude 4", "llama 4",
                "parameters model", "model card", "benchmark results"));
        KEYWORDS.put("generative-ai", List.of(
                "generative", "llm", "large language model", "chatgpt", "text-to-image",
                "text to image", "diffusion model", "image generation", "video generation",
                "prompt", "copilot", "gpt", "gemini", "claude", "llama", "mistral", "stable diffusion"));
        KEYWORDS.put("ai-research", List.of(
                "arxiv", "paper", "research", "study", "benchmark", "dataset", "state-of-the-art",
                "scientists", "experiment", "preprint"));
        KEYWORDS.put("ai-tools", List.of(
                "tool", "sdk", "library", "plugin", "extension", "api release", "developer tool",
                "open source", "open-source", "framework", "ide"));
        KEYWORDS.put("ai-startups", List.of(
                "startup", "funding", "raises", "raised", "series a", "series b", "seed round",
                "valuation", "acquisition", "acquires", "investors", "ipo"));
        KEYWORDS.put("tutorials", List.of(
                "tutorial", "how to", "guide", "course", "learn ", "beginner", "walkthrough",
                "step by step", "explained"));
        KEYWORDS.put("deep-learning", List.of(
                "neural network", "deep learning", "transformer", "attention mechanism", "cnn",
                "convolutional", "recurrent", "pytorch", "tensorflow", "jax", "training run"));
        KEYWORDS.put("machine-learning", List.of(
                "machine learning", "classifier", "regression", "clustering", "scikit-learn",
                "feature engineering", "training data", "supervised", "unsupervised"));
    }

    public String categorize(String title, String description, String fallbackSlug) {
        String titleText = normalize(title);
        String descriptionText = normalize(description);
        String combined = titleText + " " + descriptionText;

        // Explicit business announcements describe company activity, even if their copy also
        // mentions a model or tool.
        if (FUNDING_TITLE.matcher(titleText).find()) return "ai-startups";

        // Scholarly metadata is stronger evidence than incidental words such as "tool" in an abstract.
        boolean scholarlyRecord = RESEARCH_MARKER.matcher(descriptionText).find()
                && ("ai-research".equals(fallbackSlug)
                || RESEARCH_METHOD.matcher(descriptionText).find());
        boolean researchArticle = titleText.matches(".*\\b(research paper|preprint|research study)\\b.*")
                && RESEARCH_METHOD.matcher(descriptionText).find();
        if (scholarlyRecord || researchArticle) return "ai-research";

        if (combined.matches(".*\\b(ai coding agent|coding agent|code agent|agentic coding)\\b.*")) {
            return "ai-agents";
        }

        boolean modelRelease = RELEASE.matcher(titleText).find()
                && titleText.matches(".*\\b(model|llm|language model)\\b.*");
        if (modelRelease) return "ai-models";

        boolean developerTool = titleText.matches(".*\\b(api|sdk|library|developer tool|ide|plugin|framework)\\b.*")
                || titleText.contains("open-source tool") || titleText.contains("open source tool");
        if (developerTool && (RELEASE.matcher(titleText).find() || fallbackSlug == null || fallbackSlug.isBlank())) {
            return "ai-tools";
        }

        String best = null;
        int bestScore = 0;
        for (Map.Entry<String, List<String>> entry : KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (titleText.contains(keyword)) score += 3;
                if (descriptionText.contains(keyword)) score += 1;
            }
            if (score > bestScore) {
                bestScore = score;
                best = entry.getKey();
            }
        }
        if (best != null) return best;
        return fallbackSlug == null || fallbackSlug.isBlank() ? "latest-ai-news" : fallbackSlug;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
