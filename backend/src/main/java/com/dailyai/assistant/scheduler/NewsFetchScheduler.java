package com.dailyai.assistant.scheduler;

import com.dailyai.assistant.dto.RefreshResultDto;
import com.dailyai.assistant.service.NewsIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NewsFetchScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsFetchScheduler.class);

    private final NewsIngestionService ingestionService;

    public NewsFetchScheduler(NewsIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /** Every three hours in the configured time zone (Asia/Kolkata by default). */
    @Scheduled(cron = "0 0 */3 * * *", zone = "${app.timezone:Asia/Kolkata}")
    public void fetchFeeds() {
        log.info("Scheduled RSS refresh started");
        try {
            RefreshResultDto result = ingestionService.refreshAll();
            log.info("Scheduled RSS refresh finished: {} saved, {} duplicates, {} failed feeds",
                    result.saved(), result.duplicates(), result.feedsFailed());
        } catch (Exception ex) {
            log.warn("Scheduled RSS refresh could not run: {}", ex.getMessage());
        }
    }
}
