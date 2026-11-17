package com.dailyai.assistant.dto;

import java.time.Instant;
import java.util.List;

public record RefreshResultDto(
        Instant finishedAt,
        int feedsAttempted,
        int feedsSucceeded,
        int feedsFailed,
        int itemsRead,
        int saved,
        int duplicates,
        int skippedInvalid,
        List<FeedError> errors
) {
    public record FeedError(String feed, String message) {
    }
}
