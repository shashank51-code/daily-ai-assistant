package com.dailyai.assistant.integration;

import com.dailyai.assistant.entity.Article;
import com.dailyai.assistant.entity.Category;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NarrativeFactualityValidatorTest {

    private final NarrativeFactualityValidator validator = new NarrativeFactualityValidator();

    @Test
    void replacesAProductNameThatIsNotInTheSource() {
        ExplanationDraft draft = draft("OpenAI models identify features to build.");

        ExplanationDraft validated = validator.validate(draft, waveArticle());

        assertThat(validated.technicalExplanation())
                .isEqualTo(NarrativeFactualityValidator.NOT_AVAILABLE);
    }

    @Test
    void replacesTechnicalTermsThatAreNotInTheSource() {
        ExplanationDraft draft = draft("Wave uses transformer models for inference.");

        ExplanationDraft validated = validator.validate(draft, waveArticle());

        assertThat(validated.technicalExplanation())
                .isEqualTo(NarrativeFactualityValidator.NOT_AVAILABLE);
    }

    @Test
    void keepsANarrativeThatOnlyUsesSourceGroundedFacts() {
        ExplanationDraft draft = draft("Wave helps teams identify what to build.");

        ExplanationDraft validated = validator.validate(draft, waveArticle());

        assertThat(validated.technicalExplanation())
                .isEqualTo("Wave helps teams identify what to build.");
    }

    @Test
    void replacesAnInventedNumber() {
        ExplanationDraft draft = draft("Wave supports 500 million developers.");

        ExplanationDraft validated = validator.validate(draft, waveArticle());

        assertThat(validated.technicalExplanation())
                .isEqualTo(NarrativeFactualityValidator.NOT_AVAILABLE);
    }

    @Test
    void downgradesStrongClaimsInsteadOfPresentingThemAsPublisherFacts() {
        ExplanationDraft draft = draft("Wave helps teams identify what to build.");
        ExplanationDraft.Claim claim = new ExplanationDraft.Claim(
                "technicalExplanation",
                ExplanationDraft.Claim.Kind.SOURCE_FACT,
                "Wave ensures zero cloud data egress.",
                "with zero cloud data egress");
        draft = new ExplanationDraft(draft.whatHappened(), draft.technology(), draft.importance(),
                draft.howItWorks(), draft.whereUsed(), draft.fresherLearning(),
                draft.practicalExample(), draft.relatedTechnologies(), draft.sourceEvidence(),
                draft.whoBenefits(), draft.useCases(), draft.technicalExplanation(),
                List.of(claim), draft.metadata());

        ExplanationDraft validated = validator.validate(draft, waveArticle());

        assertThat(validated.claims()).hasSize(1);
        assertThat(validated.claims().get(0).kind())
                .isEqualTo(ExplanationDraft.Claim.Kind.INTERPRETATION);
        assertThat(validated.claims().get(0).section()).isEqualTo("technicalExplanation");
    }

    @Test
    void leavesAnHonestClaimAsASourceFact() {
        ExplanationDraft draft = draft("Wave helps teams identify what to build.");
        ExplanationDraft.Claim claim = new ExplanationDraft.Claim(
                "technicalExplanation",
                ExplanationDraft.Claim.Kind.SOURCE_FACT,
                "Wave helps teams identify what to build.",
                "helps teams identify what to build");
        draft = new ExplanationDraft(draft.whatHappened(), draft.technology(), draft.importance(),
                draft.howItWorks(), draft.whereUsed(), draft.fresherLearning(),
                draft.practicalExample(), draft.relatedTechnologies(), draft.sourceEvidence(),
                draft.whoBenefits(), draft.useCases(), draft.technicalExplanation(),
                List.of(claim), draft.metadata());

        ExplanationDraft validated = validator.validate(draft, waveArticle());

        assertThat(validated.claims()).hasSize(1);
        assertThat(validated.claims().get(0).kind())
                .isEqualTo(ExplanationDraft.Claim.Kind.SOURCE_FACT);
    }

    @Test
    void audienceRolesAreNotTreatedAsInventedProductNames() {
        ExplanationDraft draft = new ExplanationDraft(
                "Not available in the provided text.",
                "Not available in the provided text.",
                "Not available in the provided text.",
                "Not available in the provided text.",
                "Not available in the provided text.",
                "Not available in the provided text.",
                "Not available in the provided text.",
                "Not available in the provided text.",
                "Wave helps teams identify what to build.",
                "Developers comparing tools.",
                "Not available in the provided text.",
                "Not available in the provided text.",
                List.of(new ExplanationDraft.Claim(
                        "whoBenefits", ExplanationDraft.Claim.Kind.SOURCE_FACT,
                        "Developers comparing tools.", "Wave helps teams")),
                ExplanationDraft.Metadata.none());

        ExplanationDraft validated = validator.validate(draft, waveArticle());

        assertThat(validated.whoBenefits()).isEqualTo("Developers comparing tools.");
    }

    private ExplanationDraft draft(String technicalExplanation) {
        String unavailable = NarrativeFactualityValidator.NOT_AVAILABLE;
        return new ExplanationDraft(
                unavailable, unavailable, unavailable, unavailable, unavailable,
                unavailable, unavailable, unavailable,
                "Wave helps teams identify what to build with zero cloud data egress.",
                unavailable, unavailable, technicalExplanation,
                List.of(new ExplanationDraft.Claim(
                        "technicalExplanation", ExplanationDraft.Claim.Kind.SOURCE_FACT,
                        "Wave helps teams identify what to build.",
                        "helps teams identify what to build")),
                ExplanationDraft.Metadata.none());
    }

    private Article waveArticle() {
        Category category = new Category("AI Tools", "ai-tools", null);
        Article article = new Article();
        article.setId(16L);
        article.setTitle("Wave launches a builder for product teams");
        article.setDescription("Wave helps teams identify what to build with zero cloud data egress.");
        article.setCategory(category);
        return article;
    }
}
