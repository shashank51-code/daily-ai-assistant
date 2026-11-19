package com.dailyai.assistant.service;

import com.dailyai.assistant.entity.Article;
import com.dailyai.assistant.entity.Category;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchSummaryBuilderTest {
    private final ResearchSummaryBuilder builder = new ResearchSummaryBuilder();

    @Test
    void extractsResearchProblemInvestigationAndFindingAsExactSentences() {
        String abstractText = "arXiv:2611.19173v1 Abstract: While confidence signals are commonly evaluated for their ability to predict a model's own correctness, their relationship with problem difficulty remains less understood. In this work, we investigate a wide range of confidence-related signals and whether they can serve as indicators of problem difficulty, comparing them with token-level uncertainty. We find that perturbation inconsistency and semantic entropy exhibit strong associations with population-level problem difficulty. Our findings highlight the utility of these signals as imperfect but informative proxies.";
        Article article = new Article();
        article.setDescription(abstractText);
        article.setSource("cs.AI updates on arXiv.org");
        article.setCategory(new Category("AI Research", "ai-research", null));

        var result = builder.build(article);

        assertThat(result.beginnerSummary()).contains("confidence signals", "token-level uncertainty",
                "The reported result is");
        assertThat(result.researchProblem()).startsWith("While confidence signals").endsWith("less understood.");
        assertThat(result.investigation()).startsWith("In this work, we investigate");
        assertThat(result.mainFinding()).startsWith("We find that perturbation inconsistency");
        assertThat(result.whyItMatters()).startsWith("Our findings highlight");
        assertThat(result.beginnerDeveloperRelevance()).contains("research context rather than a ready-to-use coding technique");
        assertThat(result.plainLanguageTerms()).contains("Population-level problem difficulty means");
        assertThat(result.evidenceNote()).contains("complete source sentences");
    }

    @Test
    void doesNotInventResearchSummaryWhenAbstractIsMissing() {
        Article article = new Article();
        article.setDescription("The publisher provided a short research update.");
        article.setCategory(new Category("AI Research", "ai-research", null));

        assertThat(builder.build(article)).isNull();
    }

    @Test
    void article131AbstractProducesAUsefulDeterministicSummary() {
        Article article = new Article();
        article.setDescription("arXiv:2611.18642v1 Announce Type: new \n"
                + "Abstract: Agents that work beyond a single context window, or in teams, hand their work to a successor as a short written summary. "
                + "Evaluation has focused on whether an agent can solve a task, and relatively little is known about how much of what an agent has learned survives being written down. "
                + "We study this in 56 interactive worlds with hidden rules, where an agent that is told nothing fails and an agent given a 300-character statement of the rules succeeds. "
                + "We compare one agent that plays eight lives and keeps its whole history with a chain of eight agents that each play one life and inherit a 300-character note. "
                + "All six models we test learn less in the chain: after eight generations it stands where the single agent stood after about four lives. "
                + "Controlled variants of the chain decompose this handoff loss into three components: none from changing the agent, a smaller loss from compressing to 300 characters, and a larger loss from compressing repeatedly. "
                + "When each note is written from the full record of earlier lives instead of from the previous note, the same 300 characters recover 70% to 82% of the loss. "
                + "The loss is robust across model sizes and is not alleviated by instructing the writer to record evidence, to revise the inherited note, or to keep a structured ledger. "
                + "In simpler terms, an agent cannot hand over what it has not yet understood: a note holds what its writer has worked out, and a rule that takes several lives to see is lost with the lives that were not passed on.");
        article.setSource("cs.AI updates on arXiv.org / Yibo Liu");
        article.setCategory(new Category("AI Research", "ai-research", null));

        var result = builder.build(article);

        assertThat(result).isNotNull();
        assertThat(result.researchProblem()).startsWith("Evaluation has focused")
                .contains("little is known");
        assertThat(result.investigation()).startsWith("We study this in 56 interactive worlds");
        assertThat(result.mainFinding()).startsWith("All six models we test learn less");
        assertThat(result.whyItMatters()).startsWith("In simpler terms");
        assertThat(result.beginnerSummary()).contains("little is known", "learn less in the chain");
        assertThat(result.beginnerDeveloperRelevance()).contains("research context");
    }

    @Test
    void unsupportedFindingIsExplicitlyUnavailable() {
        Article article = new Article();
        article.setDescription("Abstract: This paper studies evaluation methods and discusses their possible use.");
        article.setCategory(new Category("AI Research", "ai-research", null));

        var result = builder.build(article);

        assertThat(result.mainFinding()).isEqualTo("Not available in the provided abstract.");
    }
}
