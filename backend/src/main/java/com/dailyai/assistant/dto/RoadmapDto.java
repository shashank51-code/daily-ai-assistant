package com.dailyai.assistant.dto;

import java.util.List;

public record RoadmapDto(
        List<Level> levels,
        int completedTopics,
        int totalTopics,
        int progressPercent
) {
    public record Level(
            Integer level,
            String levelName,
            List<LearningTopicDto> topics,
            int completedTopics,
            int totalTopics
    ) {
    }
}
