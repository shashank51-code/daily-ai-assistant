package com.dailyai.assistant.controller;

import com.dailyai.assistant.dto.SchedulerRefreshStatusResponse;
import com.dailyai.assistant.service.SchedulerRefreshService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    private final SchedulerRefreshService refreshService;

    public SchedulerController(SchedulerRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @PostMapping("/news/refresh")
    public ResponseEntity<SchedulerRefreshStatusResponse> refresh() {
        SchedulerRefreshStatusResponse response = refreshService.trigger("GITHUB_ACTIONS");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/news/status")
    public SchedulerRefreshStatusResponse status() {
        return refreshService.status();
    }
}