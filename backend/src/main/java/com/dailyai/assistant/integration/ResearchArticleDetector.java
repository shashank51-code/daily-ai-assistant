package com.dailyai.assistant.integration;

import com.dailyai.assistant.entity.Article;

import java.util.Locale;
import java.util.regex.Pattern;

/** Shared, deterministic research-article detection for prompts and summaries. */
public final class ResearchArticleDetector {

    private static final Pattern ABSTRACT_MARKER = Pattern.compile("(?im)(^|\\n)\\s*abstract\\s*:");

    private ResearchArticleDetector() {
    }

    public static boolean isResearch(Article article) {
        if (article == null) {
            return false;
        }
        String category = article.getCategory() == null ? "" : article.getCategory().getSlug();
        if ("ai-research".equalsIgnoreCase(category)) {
            return true;
        }
        String source = article.getSource() == null ? "" : article.getSource().toLowerCase(Locale.ROOT);
        if (source.contains("arxiv")) {
            return true;
        }
        String description = article.getDescription() == null ? "" : article.getDescription();
        String lower = description.toLowerCase(Locale.ROOT);
        return lower.contains("arxiv:") || ABSTRACT_MARKER.matcher(description).find();
    }
}
