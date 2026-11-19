package com.dailyai.assistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class RssRefreshExecutionRunner {

    private static final Logger log = LoggerFactory.getLogger(RssRefreshExecutionRunner.class);

    @Async
    public void run(SchedulerRefreshService service, long runId) {
        try {
            service.executeRun(runId);
        } catch (Exception ex) {
            log.warn("RSS refresh execution {} failed: {}", runId, ex.getMessage());
        }
    }
}