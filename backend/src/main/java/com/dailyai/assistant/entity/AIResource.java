package com.dailyai.assistant.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ai_resources", uniqueConstraints = @UniqueConstraint(name = "uk_resources_name", columnNames = "name"))
public class AIResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 120)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(length = 500)
    private String website;

    /** Only filled with information that was verified when the seed data was written. */
    @Column(columnDefinition = "TEXT")
    private String pricing;

    @Column(name = "free_alternatives", columnDefinition = "TEXT")
    private String freeAlternatives;

    @Column(nullable = false)
    private boolean trending = false;

    @Column(name = "pricing_verified_on", length = 20)
    private String pricingVerifiedOn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getPricing() {
        return pricing;
    }

    public void setPricing(String pricing) {
        this.pricing = pricing;
    }

    public String getFreeAlternatives() {
        return freeAlternatives;
    }

    public void setFreeAlternatives(String freeAlternatives) {
        this.freeAlternatives = freeAlternatives;
    }

    public boolean isTrending() {
        return trending;
    }

    public void setTrending(boolean trending) {
        this.trending = trending;
    }

    public String getPricingVerifiedOn() {
        return pricingVerifiedOn;
    }

    public void setPricingVerifiedOn(String pricingVerifiedOn) {
        this.pricingVerifiedOn = pricingVerifiedOn;
    }
}
