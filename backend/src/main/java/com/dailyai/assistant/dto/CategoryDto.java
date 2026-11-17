package com.dailyai.assistant.dto;

public record CategoryDto(
        Long id,
        String name,
        String slug,
        String description,
        long articleCount
) {
}
