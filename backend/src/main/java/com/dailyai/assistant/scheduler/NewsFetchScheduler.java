package com.dailyai.assistant.scheduler;

import com.dailyai.assistant.dto.RefreshResultDto;
import com.dailyai.assistant.service.SchedulerRefreshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NewsFetchScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsFetchScheduler.class);

    private final SchedulerRefreshService refreshService;

    public NewsFetchScheduler(SchedulerRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    /** Every three hours in the configured time zone (Asia/Kolkata by default). */
    @Scheduled(cron = "0 0 */3 * * *", zone = "${app.timezone:Asia/Kolkata}")
    public void fetchFeeds() {
        log.info("Scheduled RSS refresh started");
        try {
            RefreshResultDto result = refreshService.runSynchronously("SCHEDULER");
            if (result == null) {
                log.info("Scheduled RSS refresh skipped because another execution is already running");
            } else {
                log.info("Scheduled RSS refresh finished: {} saved, {} duplicates, {} failed feeds",
                        result.saved(), result.duplicates(), result.feedsFailed());
            }
        } catch (Exception ex) {
            log.warn("Scheduled RSS refresh could not run: {}", ex.getMessage());
        }
    }
}