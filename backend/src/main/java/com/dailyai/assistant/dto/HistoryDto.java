package com.dailyai.assistant.dto;

import java.time.Instant;

public record HistoryDto(
        Long id,
        Long articleId,
        String title,
        String source,
        String url,
        Instant readAt
) {
}
