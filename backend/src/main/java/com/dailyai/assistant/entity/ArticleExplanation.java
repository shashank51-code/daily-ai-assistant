package com.dailyai.assistant.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Beginner friendly explanation of an article. The {@code generatedBy} column tells
 * the client whether the text came from an LLM, from the offline template or from
 * the publisher's own description.
 */
@Entity
@Table(name = "article_explanations",
        uniqueConstraints = @UniqueConstraint(name = "uk_explanations_article", columnNames = "article_id"))
public class ArticleExplanation {

    public enum GeneratedBy {
        AI, BASIC
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(name = "what_happened", columnDefinition = "TEXT")
    private String whatHappened;

    @Column(columnDefinition = "TEXT")
    private String technology;

    @Column(columnDefinition = "TEXT")
    private String importance;

    @Column(name = "how_it_works", columnDefinition = "TEXT")
    private String howItWorks;

    @Column(name = "where_used", columnDefinition = "TEXT")
    private String whereUsed;

    @Column(name = "fresher_learning", columnDefinition = "TEXT")
    private String fresherLearning;

    @Column(name = "practical_example", columnDefinition = "TEXT")
    private String practicalExample;

    @Column(name = "related_technologies", columnDefinition = "TEXT")
    private String relatedTechnologies;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by", nullable = false, length = 20)
    private GeneratedBy generatedBy = GeneratedBy.BASIC;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Article getArticle() {
        return article;
    }

    public void setArticle(Article article) {
        this.article = article;
    }

    public String getWhatHappened() {
        return whatHappened;
    }

    public void setWhatHappened(String whatHappened) {
        this.whatHappened = whatHappened;
    }

    public String getTechnology() {
        return technology;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public String getImportance() {
        return importance;
    }

    public void setImportance(String importance) {
        this.importance = importance;
    }

    public String getHowItWorks() {
        return howItWorks;
    }

    public void setHowItWorks(String howItWorks) {
        this.howItWorks = howItWorks;
    }

    public String getWhereUsed() {
        return whereUsed;
    }

    public void setWhereUsed(String whereUsed) {
        this.whereUsed = whereUsed;
    }

    public String getFresherLearning() {
        return fresherLearning;
    }

    public void setFresherLearning(String fresherLearning) {
        this.fresherLearning = fresherLearning;
    }

    public String getPracticalExample() {
        return practicalExample;
    }

    public void setPracticalExample(String practicalExample) {
        this.practicalExample = practicalExample;
    }

    public String getRelatedTechnologies() {
        return relatedTechnologies;
    }

    public void setRelatedTechnologies(String relatedTechnologies) {
        this.relatedTechnologies = relatedTechnologies;
    }

    public GeneratedBy getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(GeneratedBy generatedBy) {
        this.generatedBy = generatedBy;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
