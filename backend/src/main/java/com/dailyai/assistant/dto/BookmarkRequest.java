package com.dailyai.assistant.dto;

import com.dailyai.assistant.entity.Bookmark;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookmarkRequest(
        @NotNull(message = "itemType is required (ARTICLE, TOOL or TOPIC)")
        Bookmark.ItemType itemType,

        @NotNull(message = "itemId is required")
        @Positive(message = "itemId must be positive")
        Long itemId
) {
}
