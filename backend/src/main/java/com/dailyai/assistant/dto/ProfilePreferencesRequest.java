package com.dailyai.assistant.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProfilePreferencesRequest(
        @NotNull(message = "preferences must be a list; use an empty list to reset")
        @Size(max = 8, message = "at most 8 preferences may be selected")
        List<String> preferences
) {
}
