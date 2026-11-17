package com.dailyai.assistant.dto;

import com.dailyai.assistant.entity.LearningProgress;
import jakarta.validation.constraints.NotNull;

public record ProgressRequest(
        @NotNull(message = "topicId is required")
        Long topicId,

        @NotNull(message = "status is required (NOT_STARTED, IN_PROGRESS or COMPLETED)")
        LearningProgress.Status status
) {
}
