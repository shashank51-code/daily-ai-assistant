package com.dailyai.assistant.dto;

import com.dailyai.assistant.entity.AIResource;

import java.util.List;

public record ToolDto(
        Long id,
        String name,
        String category,
        String description,
        List<String> features,
        String website,
        String pricing,
        String freeAlternatives,
        boolean trending,
        String pricingVerifiedOn,
        boolean bookmarked
) {
    public static ToolDto from(AIResource resource, boolean bookmarked) {
        return new ToolDto(
                resource.getId(),
                resource.getName(),
                resource.getCategory(),
                resource.getDescription(),
                resource.getFeatures() == null ? List.of()
                        : resource.getFeatures().lines().map(String::trim).filter(s -> !s.isEmpty()).toList(),
                resource.getWebsite(),
                resource.getPricing(),
                resource.getFreeAlternatives(),
                resource.isTrending(),
                resource.getPricingVerifiedOn(),
                bookmarked
        );
    }
}
