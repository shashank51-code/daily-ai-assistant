package com.dailyai.assistant.dto;

import java.time.Instant;

public record ProgressDto(
        Long topicId,
        String topicTitle,
        String status,
        Instant completedAt,
        Instant updatedAt
) {
}
