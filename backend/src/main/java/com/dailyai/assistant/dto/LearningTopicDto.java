package com.dailyai.assistant.dto;

import com.dailyai.assistant.entity.LearningProgress;
import com.dailyai.assistant.entity.LearningTopic;

import java.time.Instant;
import java.util.List;

public record LearningTopicDto(
        Long id,
        Integer level,
        String levelName,
        String title,
        String slug,
        String explanation,
        List<String> prerequisites,
        List<String> resources,
        List<String> exercises,
        List<String> projectIdeas,
        List<String> interviewQuestions,
        Integer orderIndex,
        String status,
        Instant completedAt
) {
    public static LearningTopicDto from(LearningTopic topic, LearningProgress progress) {
        return new LearningTopicDto(
                topic.getId(),
                topic.getLevel(),
                topic.getLevelName(),
                topic.getTitle(),
                topic.getSlug(),
                topic.getExplanation(),
                lines(topic.getPrerequisites()),
                lines(topic.getResources()),
                lines(topic.getExercises()),
                lines(topic.getProjectIdeas()),
                lines(topic.getInterviewQuestions()),
                topic.getOrderIndex(),
                progress == null ? "NOT_STARTED" : progress.getStatus().name(),
                progress == null ? null : progress.getCompletedAt()
        );
    }

    private static List<String> lines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
    }
}
