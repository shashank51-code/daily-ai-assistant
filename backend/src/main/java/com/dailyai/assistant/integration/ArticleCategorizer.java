package com.dailyai.assistant.integration;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Very small keyword based categorizer. It is intentionally simple and explainable:
 * title matches weigh more than description matches. The feed configuration provides
 * a fallback category for text that matches nothing.
 */
@Component
public class ArticleCategorizer {

    private static final Map<String, List<String>> KEYWORDS = new LinkedHashMap<>();

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
        String titleText = title == null ? "" : title.toLowerCase(Locale.ROOT);
        String descriptionText = description == null ? "" : description.toLowerCase(Locale.ROOT);

        String best = null;
        int bestScore = 0;
        for (Map.Entry<String, List<String>> entry : KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (titleText.contains(keyword)) {
                    score += 3;
                }
                if (descriptionText.contains(keyword)) {
                    score += 1;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = entry.getKey();
            }
        }

        if (best == null) {
            return fallbackSlug == null || fallbackSlug.isBlank() ? "latest-ai-news" : fallbackSlug;
        }
        return best;
    }
}
