package com.dailyai.assistant.dto;

import java.time.Instant;

public record BookmarkDto(
        Long id,
        String itemType,
        Long itemId,
        String title,
        String subtitle,
        String url,
        Instant createdAt
) {
}
