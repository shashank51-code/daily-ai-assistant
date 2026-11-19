package com.dailyai.assistant.integration;

import com.dailyai.assistant.entity.Article;

import java.util.List;
import java.util.Locale;

/**
 * Provider-independent explanation payload.
 *
 * <p>Every field except {@code claims} must be present. Claims are the preferred
 * evidence unit: each claim carries a short quote copied from the publisher text,
 * and a claim whose quote is not found in the source is dropped rather than shown.</p>
 */
public record ExplanationDraft(
        String whatHappened,
        String technology,
        String importance,
        String howItWorks,
        String whereUsed,
        String fresherLearning,
        String practicalExample,
        String relatedTechnologies,
        String sourceEvidence,
        String whoBenefits,
        String useCases,
        String technicalExplanation,
        List<Claim> claims,
        Metadata metadata
) {

    /**
     * Claim provenance. A null section is accepted for legacy stored claims;
     * new provider claims declare the narrative field they support.
     */
    public record Claim(String section, Kind kind, String statement, String evidence) {

        public enum Kind {
            SOURCE_FACT,
            INTERPRETATION
        }

        /** Backward-compatible constructor for existing callers and stored rows. */
        public Claim(String statement, String evidence) {
            this(null, Kind.SOURCE_FACT, statement, evidence);
        }

        public Claim withKind(Kind replacement) {
            return new Claim(section, replacement, statement, evidence);
        }
    }

    public record Metadata(String provider, String model, int inputTokens, int outputTokens, long latencyMs) {
        public static Metadata none() {
            return new Metadata("none", "none", 0, 0, 0);
        }
    }

    public boolean isComplete() {
        return hasText(whatHappened)
                && hasText(technology)
                && hasText(importance)
                && hasText(howItWorks)
                && hasText(whereUsed)
                && hasText(fresherLearning)
                && hasText(practicalExample)
                && hasText(relatedTechnologies)
                && hasText(sourceEvidence)
                && hasText(whoBenefits)
                && hasText(useCases)
                && hasText(technicalExplanation);
    }

    public boolean hasSourceEvidence(Article article) {
        if (article == null || sourceEvidence == null || sourceEvidence.isBlank()) {
            return false;
        }
        return sourceText(article).contains(sourceEvidence.trim().toLowerCase(Locale.ROOT))
                || beginsWithArticleTitle(article, sourceEvidence);
    }

    /**
     * Keeps only claims whose quote is literally present in the publisher text.
     * One valid global quote is not treated as proof for unrelated claims.
     */
    public List<Claim> supportedClaims(Article article) {
        if (claims == null || article == null) {
            return List.of();
        }
        String source = sourceText(article);
        return claims.stream()
                .filter(claim -> claim != null && hasText(claim.statement()) && hasText(claim.evidence()))
                .filter(claim -> source.contains(claim.evidence().trim().toLowerCase(Locale.ROOT)))
                .filter(claim -> claim.kind() == null || claim.kind() == Claim.Kind.SOURCE_FACT
                        || claim.kind() == Claim.Kind.INTERPRETATION)
                .distinct()
                .toList();
    }

    public boolean hasGroundedSourceFact(Article article) {
        return supportedClaims(article).stream()
                .anyMatch(claim -> claim.kind() == null || claim.kind() == Claim.Kind.SOURCE_FACT);
    }

    /**
     * A draft full of unavailable placeholders is complete in shape but not useful.
     * Research explanations additionally need real research content, not only an
     * audience sentence.
     */
    public boolean hasSufficientSubstance(boolean research) {
        int substance = substantiveFieldCount();
        if (research) {
            int researchFields = countSubstantive(whatHappened, importance, howItWorks, technicalExplanation);
            return substance >= 4 && researchFields >= 2;
        }
        return substance >= 3;
    }

    private int substantiveFieldCount() {
        return countSubstantive(whatHappened, technology, importance, howItWorks, whereUsed,
                fresherLearning, practicalExample, relatedTechnologies, whoBenefits, useCases,
                technicalExplanation);
    }

    private int countSubstantive(String... values) {
        int count = 0;
        for (String value : values) {
            if (hasText(value) && !isUnavailable(value)) {
                count++;
            }
        }
        return count;
    }

    private boolean isUnavailable(String value) {
        String lower = value.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("not available in the provided text")
                || lower.startsWith("not available in the supplied text");
    }

    public ExplanationDraft withClaims(List<Claim> supported) {
        return new ExplanationDraft(whatHappened, technology, importance, howItWorks, whereUsed,
                fresherLearning, practicalExample, relatedTechnologies, sourceEvidence,
                whoBenefits, useCases, technicalExplanation, supported, metadata);
    }

    private static boolean beginsWithArticleTitle(Article article, String evidence) {
        String title = article.getTitle();
        return title != null && evidence.trim().length() >= 8
                && title.toLowerCase(Locale.ROOT).startsWith(evidence.trim().toLowerCase(Locale.ROOT));
    }

    private static String sourceText(Article article) {
        return ((article.getTitle() == null ? "" : article.getTitle()) + " "
                + (article.getDescription() == null ? "" : article.getDescription()))
                .toLowerCase(Locale.ROOT);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
