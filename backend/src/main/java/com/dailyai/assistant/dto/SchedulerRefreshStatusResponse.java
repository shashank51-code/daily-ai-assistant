package com.dailyai.assistant.dto;

import java.time.Instant;

public record SchedulerRefreshStatusResponse(
        boolean running,
        Long runId,
        Instant startedAt,
        Instant finishedAt,
        String status,
        int feedsAttempted,
        int feedsSucceeded,
        int feedsFailed,
        int itemsRead,
        int saved,
        int duplicates,
        int skippedInvalid,
        Long durationMs,
        String errorSummary
) {
}