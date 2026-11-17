package com.dailyai.assistant.controller;

import com.dailyai.assistant.dto.DailyPlanDto;
import com.dailyai.assistant.dto.DailyTaskDto;
import com.dailyai.assistant.dto.LearningTopicDto;
import com.dailyai.assistant.dto.ProgressDto;
import com.dailyai.assistant.dto.ProgressRequest;
import com.dailyai.assistant.dto.RoadmapDto;
import com.dailyai.assistant.service.LearningService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/learning")
public class LearningController {

    private final LearningService learningService;

    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }

    @GetMapping("/roadmap")
    public RoadmapDto roadmap() {
        return learningService.roadmap();
    }

    @GetMapping("/topics")
    public List<LearningTopicDto> topics(@RequestParam(required = false) Integer level) {
        return learningService.topics(level);
    }

    @GetMapping("/topics/{id}")
    public LearningTopicDto topic(@PathVariable Long id) {
        return learningService.topic(id);
    }

    @PostMapping("/progress")
    public ProgressDto updateProgress(@Valid @RequestBody ProgressRequest request) {
        return learningService.updateProgress(request);
    }

    @GetMapping("/progress")
    public List<ProgressDto> progress() {
        return learningService.progress();
    }

    @GetMapping("/daily-plan")
    public DailyPlanDto dailyPlan() {
        return learningService.dailyPlan();
    }

    @PostMapping("/daily-plan/{taskId}")
    public DailyTaskDto completeDailyTask(@PathVariable Long taskId,
                                          @RequestParam(defaultValue = "true") boolean completed) {
        return learningService.completeDailyTask(taskId, completed);
    }
}
