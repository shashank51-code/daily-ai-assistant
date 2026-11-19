package com.dailyai.assistant.service;

import com.dailyai.assistant.dto.ArticleResearchSummaryDto;
import com.dailyai.assistant.entity.Article;
import com.dailyai.assistant.integration.ResearchArticleDetector;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Selects whole sentences from a publisher abstract; it does not synthesize research claims. */
@Component
public class ResearchSummaryBuilder {
    private static final String UNKNOWN = "Not available in the provided abstract.";
    private static final Pattern ABSTRACT = Pattern.compile("(?is)\\babstract\\s*:\\s*(.*)$");
    private static final Pattern SENTENCE = Pattern.compile("(?<=[.!?])\\s+(?=[A-Z0-9])");
    private static final Pattern INVESTIGATION = Pattern.compile(
            "(?i)\\b(investigat|evaluat|examin|compar|assess|analy[sz]|stud(?:y|ied|ies)|test)\\w*\\b");
    private static final Pattern EXPLICIT_INVESTIGATION = Pattern.compile(
            "(?i)^(in this (work|paper|study)|we (study|investigate|evaluate|examine|compare|assess|analy[sz]e|test))\\b");
    private static final Pattern FINDING = Pattern.compile(
            "(?i)\\b(we find|we show|we observe|our results|results (show|indicate|suggest)|our findings|"
                    + "learn(?:s|ed)? less|learn(?:s|ed)? more|outperform|improve[sd]?|reduc(?:e|es|ed)|"
                    + "increase[sd]?|recover(?:s|ed)?|demonstrat(?:e|es|ed)|reveal(?:s|ed)?)\\b");
    private static final Pattern IMPLICATION = Pattern.compile(
            "(?i)\\b(suggest|potential|implication|utility|highlight|enable|therefore|in simpler terms|"
                    + "matters|important|significance)\\b");

    public ArticleResearchSummaryDto build(Article article) {
        if (article == null || article.getDescription() == null
                || !ResearchArticleDetector.isResearch(article)) {
            return null;
        }
        String description = article.getDescription();
        Matcher abstractMatcher = ABSTRACT.matcher(description);
        if (!abstractMatcher.find()) {
            return null;
        }

        List<String> sentences = Arrays.stream(SENTENCE.split(abstractMatcher.group(1).trim()))
                .map(String::trim).filter(value -> !value.isBlank()).toList();
        if (sentences.isEmpty()) {
            return null;
        }

        String problem = sentences.stream().filter(this::statesProblem).findFirst().orElse(UNKNOWN);
        String investigation = sentences.stream()
                .filter(value -> EXPLICIT_INVESTIGATION.matcher(value).find())
                .findFirst()
                .orElseGet(() -> sentences.stream()
                        .filter(value -> INVESTIGATION.matcher(value).find())
                        .findFirst().orElse(UNKNOWN));
        String finding = sentences.stream().filter(this::statesFinding).findFirst().orElse(UNKNOWN);
        String why = sentences.stream().filter(this::statesImplication)
                .reduce((first, second) -> second).orElse(UNKNOWN);

        String lowerAbstract = abstractMatcher.group(1).toLowerCase(Locale.ROOT);
        String beginnerRelevance = lowerAbstract.matches("(?s).*(\\bapi\\b|\\bsoftware\\b|\\bcode\\b|\\bimplementation\\b|\\blibrary\\b|\\bsdk\\b).*")
                ? "The abstract mentions software or implementation details. A beginner developer can use the paper to understand the research question; check the full paper before trying to reproduce it."
                : "The abstract does not describe an API, library or implementation steps. For a beginner developer, this is research context rather than a ready-to-use coding technique.";
        StringBuilder terms = new StringBuilder();
        if (lowerAbstract.contains("confidence signals")) {
            terms.append("Confidence signals are measurements studied as clues about whether a model's answer is correct; the abstract does not give their exact calculations.");
        }
        if (lowerAbstract.contains("population-level problem difficulty")) {
            if (!terms.isEmpty()) terms.append(" ");
            terms.append("Population-level problem difficulty means how difficult problems tend to be across a group of examples. This is a plain-language reading of the abstract, not a separate result.");
        }
        String plainLanguageTerms = terms.isEmpty() ? UNKNOWN : terms.toString();

        return new ArticleResearchSummaryDto(
                beginnerSummary(problem, finding, investigation),
                problem,
                investigation,
                finding,
                why,
                beginnerRelevance,
                plainLanguageTerms,
                "The beginner summary and research fields use complete source sentences (or an explicit unavailable marker). "
                        + "The developer note is an interpretation of what the abstract does not describe.");
    }

    private String beginnerSummary(String problem, String finding, String investigation) {
        if (!UNKNOWN.equals(problem)) {
            String summary = "The abstract frames the question this way: " + problem;
            if (!UNKNOWN.equals(investigation)) {
                summary += " The study describes its approach as: " + investigation;
            }
            if (!UNKNOWN.equals(finding)) {
                summary += " The reported result is: " + finding;
            }
            return summary;
        }
        if (!UNKNOWN.equals(finding)) {
            return "The abstract reports: " + finding;
        }
        if (!UNKNOWN.equals(investigation)) {
            return "The abstract describes the work this way: " + investigation;
        }
        return UNKNOWN;
    }

    private boolean statesProblem(String sentence) {
        String lower = sentence.toLowerCase(Locale.ROOT);
        return lower.contains("less understood") || lower.contains("remain unclear")
                || lower.contains("remains unclear") || lower.contains("little is known")
                || lower.contains("not well understood") || lower.contains("open question")
                || lower.contains("challenge") || lower.contains("problem")
                || lower.contains("gap") || lower.contains("unknown")
                || lower.contains("limited evidence");
    }

    private boolean statesFinding(String sentence) {
        return FINDING.matcher(sentence).find();
    }

    private boolean statesImplication(String sentence) {
        return IMPLICATION.matcher(sentence).find();
    }
}
