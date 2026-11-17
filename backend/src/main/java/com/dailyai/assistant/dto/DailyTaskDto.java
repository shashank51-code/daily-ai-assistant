package com.dailyai.assistant.dto;

import com.dailyai.assistant.entity.DailyTask;

public record DailyTaskDto(
        Long id,
        Integer dayNumber,
        String title,
        String description,
        String kind,
        Integer estimatedMinutes,
        boolean completedInPlan
) {
    public static DailyTaskDto from(DailyTask task, boolean completed) {
        return new DailyTaskDto(task.getId(), task.getDayNumber(), task.getTitle(), task.getDescription(),
                task.getKind().name(), task.getEstimatedMinutes(), completed);
    }
}
