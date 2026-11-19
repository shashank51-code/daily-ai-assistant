package com.dailyai.assistant.service;

import com.dailyai.assistant.dto.ArticleUsefulnessDto;
import com.dailyai.assistant.dto.LearningRecommendation;
import com.dailyai.assistant.entity.Article;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Transparent, conservative relevance rules based on feed text, article type and interests. */
@Component
public class ArticleUsefulnessEngine {
    private static final Map<String, Set<String>> CATEGORY_INTERESTS = Map.of(
            "generative-ai", Set.of("AI_ML", "GENERATIVE_AI", "DEVELOPER_TOOLS"),
            "ai-agents", Set.of("AI_CODING_AGENTS", "JAVA_BACKEND", "DEVELOPER_TOOLS", "AI_AUTOMATION"),
            "ai-tools", Set.of("DEVELOPER_TOOLS", "JAVA_BACKEND", "AI_CODING_AGENTS"),
            "ai-models", Set.of("AI_ML", "GENERATIVE_AI", "CLOUD_AI_INFRASTRUCTURE"),
            "machine-learning", Set.of("AI_ML", "AI_RESEARCH"),
            "deep-learning", Set.of("AI_ML", "AI_RESEARCH"),
            "ai-research", Set.of("AI_RESEARCH", "AI_ML"),
            "ai-startups", Set.of("AI_ML", "CLOUD_AI_INFRASTRUCTURE"),
            "latest-ai-news", Set.of("AI_ML", "GENERATIVE_AI", "AI_AUTOMATION", "CLOUD_AI_INFRASTRUCTURE"),
            "tutorials", Set.of("AI_ML", "DEVELOPER_TOOLS", "JAVA_BACKEND", "AI_CODING_AGENTS")
    );
    private static final Map<String, String> INTEREST_LABELS = Map.ofEntries(
            Map.entry("AI_ML", "AI and Machine Learning"), Map.entry("GENERATIVE_AI", "Generative AI"),
            Map.entry("AI_CODING_AGENTS", "AI Coding Agents"), Map.entry("JAVA_BACKEND", "Java and Backend Development"),
            Map.entry("DEVELOPER_TOOLS", "Developer Tools"), Map.entry("AI_AUTOMATION", "AI Automation"),
            Map.entry("CLOUD_AI_INFRASTRUCTURE", "Cloud and AI Infrastructure"), Map.entry("AI_RESEARCH", "AI Research")
    );
    private static final Pattern BUSINESS = Pattern.compile("\\b(funding|raises? \\$|series [a-f]|valuation|investor|acquisition|ipo|revenue|layoffs?)\\b");
    private static final Pattern OPINION = Pattern.compile("\\b(opinion|editorial|op-ed|commentary|why i think|my take)\\b");
    private static final Pattern ACTIONABLE = Pattern.compile("\\b(release[sd]?|launch(?:es|ed)?|available|introduc(?:e|es|ed)|open[- ]source|api|sdk|agent|coding assistant|tool|model|benchmark|paper|research|automation|inference|chip|gpu|cloud)\\b");
    private static final Pattern PRODUCT = Pattern.compile("\\b(release[sd]?|launch(?:es|ed)?|available|introduc(?:e|es|ed)|open[- ]source|api|sdk|agent|coding assistant|tool|model)\\b");

    public ArticleUsefulnessDto evaluate(Article article, List<String> preferences) {
        String category = article == null || article.getCategory() == null
                ? "latest-ai-news" : article.getCategory().getSlug();
        String title = article == null || article.getTitle() == null ? "" : article.getTitle();
        String description = article == null || article.getDescription() == null ? "" : article.getDescription();
        String text = (title + " " + description).toLowerCase(Locale.ROOT);
        String type = classifyType(category, text);
        Set<String> matches = new java.util.HashSet<>(CATEGORY_INTERESTS.getOrDefault(category, Set.of()));
        matches.addAll(interestsForType(type));
        List<String> selected = preferences == null ? List.of() : preferences.stream().filter(matches::contains).toList();
        boolean hasContent = !title.isBlank() && !description.isBlank();
        boolean sufficient = hasContent && ACTIONABLE.matcher(text).find();
        boolean business = "company funding or business news".equals(type);
        boolean opinion = "opinion or commentary".equals(type);

        LearningRecommendation recommendation;
        String reason;
        String relevance;
        if (preferences == null || preferences.isEmpty()) {
            recommendation = LearningRecommendation.EXPLORE_LATER;
            reason = sufficient
                    ? "Neutral suggestion: the feed suggests " + type + ", but no interests are selected to judge its value for you."
                    : "Neutral suggestion: explore later because the available feed text is too limited to support a stronger recommendation.";
            relevance = "Personal relevance is not set. Choose interests in Profile to personalize this explanation.";
        } else if (selected.isEmpty()) {
            recommendation = LearningRecommendation.SKIP;
            reason = "This update does not match your selected interests. Skip it unless it solves a current need.";
            relevance = "No match among your selected interests.";
        } else {
            relevance = "Matches your interests: " + selected.stream().map(INTEREST_LABELS::get).distinct().reduce((a, b) -> a + ", " + b).orElse("") + ".";
            if (business || opinion) {
                recommendation = LearningRecommendation.SKIP;
                reason = "This is " + type + ", which may be useful industry context but does not describe a technical update to learn now.";
            } else if (!sufficient) {
                recommendation = LearningRecommendation.EXPLORE_LATER;
                reason = "The feed text does not provide enough detail to judge practical value, so a stronger recommendation would be uncertain.";
            } else if (type.equals("AI research paper or finding")) {
                recommendation = selected.contains("AI_RESEARCH") ? LearningRecommendation.EXPLORE_LATER : LearningRecommendation.SKIP;
                reason = selected.contains("AI_RESEARCH")
                        ? "This research topic matches your interests; review the study and its evidence when useful."
                        : "This research update is less directly actionable for your selected interests.";
            } else if (Set.of("AI coding tool or agent", "new developer API", "AI automation capability", "AI product update").contains(type)) {
                recommendation = LearningRecommendation.LEARN_NOW;
                reason = "The feed describes a practical " + type + " and it matches your interests. Check the original source for setup and limitations.";
            } else {
                recommendation = LearningRecommendation.EXPLORE_LATER;
                reason = "This matches your interests, but the feed does not clearly establish an actionable change to learn now.";
            }
        }

        return new ArticleUsefulnessDto(recommendation, reason, whoBenefits(category, type), relevance,
                practicalUse(type), null);
    }

    private Set<String> interestsForType(String type) {
        return switch (type) {
            case "AI coding tool or agent" -> Set.of("AI_CODING_AGENTS", "JAVA_BACKEND", "DEVELOPER_TOOLS", "AI_AUTOMATION");
            case "new developer API" -> Set.of("JAVA_BACKEND", "DEVELOPER_TOOLS", "AI_CODING_AGENTS");
            case "AI research paper or finding" -> Set.of("AI_RESEARCH", "AI_ML");
            case "AI automation capability" -> Set.of("AI_AUTOMATION", "DEVELOPER_TOOLS");
            case "AI infrastructure development" -> Set.of("CLOUD_AI_INFRASTRUCTURE", "AI_ML");
            case "new AI model release" -> Set.of("AI_ML", "GENERATIVE_AI", "CLOUD_AI_INFRASTRUCTURE");
            case "AI product update" -> Set.of("GENERATIVE_AI", "DEVELOPER_TOOLS", "AI_ML");
            default -> Set.of();
        };
    }

    private String classifyType(String category, String text) {
        if (BUSINESS.matcher(text).find() || category.equals("ai-startups")) return "company funding or business news";
        if (OPINION.matcher(text).find()) return "opinion or commentary";
        if (category.equals("ai-research") || text.contains(" arxiv") || text.contains("research paper") || text.contains("paper proposes")) return "AI research paper or finding";
        if (text.matches(".*\\b(api|sdk|developer endpoint)\\b.*")) return "new developer API";
        if (text.matches(".*\\b(coding agent|coding assistant|code agent)\\b.*") || category.equals("ai-agents")) return "AI coding tool or agent";
        if (text.matches(".*\\b(automation|automate|workflow agent)\\b.*")) return "AI automation capability";
        if (text.matches(".*\\b(gpu|inference infrastructure|data center|data centre|cloud infrastructure|accelerator)\\b.*")) return "AI infrastructure development";
        if (text.matches(".*\\b(model|llm|large language model)\\b.*") || category.equals("ai-models")) return "new AI model release";
        if (PRODUCT.matcher(text).find() || category.equals("ai-tools") || category.equals("generative-ai")) return "AI product update";
        if (ACTIONABLE.matcher(text).find()) return "general AI technical update";
        return "general technology news or unclear update";
    }

    private String whoBenefits(String category, String type) {
        if (type.equals("company funding or business news")) return "Readers tracking AI companies, investment and the market.";
        if (type.equals("AI research paper or finding")) return "AI researchers and readers who want to evaluate research evidence.";
        if (type.equals("AI coding tool or agent") || type.equals("new developer API")) return "Software developers evaluating tools or APIs for a real project.";
        if (type.equals("AI infrastructure development")) return "Teams responsible for model hosting, cloud platforms or AI infrastructure.";
        if (type.equals("AI automation capability")) return "Teams looking for ways to automate a specific repeatable workflow.";
        return switch (category) {
            case "tutorials" -> "Learners looking for practical AI guidance.";
            case "ai-tools" -> "Developers comparing tools, libraries or APIs.";
            default -> "Readers following AI developments; the feed may not identify a direct audience.";
        };
    }

    private String practicalUse(String type) {
        return switch (type) {
            case "AI coding tool or agent" -> "If the source documents a coding agent, a Java developer could evaluate it on a small Spring Boot change, review its diff and tests, and keep the final approval. The feed alone may not confirm Java support.";
            case "new developer API" -> "Check the source's documented API and limits, then try a small isolated request before using it in an application.";
            case "AI research paper or finding" -> "Read the study design and evidence first; use the result as research context until it is validated for your own task.";
            case "company funding or business news", "opinion or commentary" -> "No direct implementation use is established by this type of update.";
            case "AI infrastructure development" -> "Compare the source's documented infrastructure change with a real hosting or performance need before evaluating it.";
            case "AI automation capability" -> "Map the documented capability to one repetitive workflow and check its human review and failure handling.";
            default -> "The feed does not establish a specific use case. Read the original source before applying this update.";
        };
    }
}
