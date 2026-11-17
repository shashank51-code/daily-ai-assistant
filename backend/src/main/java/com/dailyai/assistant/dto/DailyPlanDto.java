package com.dailyai.assistant.dto;

import java.util.List;

public record DailyPlanDto(
        List<Day> days,
        int completedTasks,
        int totalTasks,
        int progressPercent
) {
    public record Day(
            Integer dayNumber,
            List<DailyTaskDto> tasks,
            int completedTasks,
            int totalTasks
    ) {
    }
}
