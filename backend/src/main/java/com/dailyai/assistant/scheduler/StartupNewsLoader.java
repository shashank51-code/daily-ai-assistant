package com.dailyai.assistant.scheduler;

import com.dailyai.assistant.config.AppProperties;
import com.dailyai.assistant.dto.RefreshResultDto;
import com.dailyai.assistant.repository.ArticleRepository;
import com.dailyai.assistant.service.NewsIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * When the database is empty the app would look broken until the next scheduled run,
 * so one fetch is triggered at startup. Existing data is never refetched on boot,
 * which keeps repeated requests to the news sources low.
 */
@Component
public class StartupNewsLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupNewsLoader.class);

    private final AppProperties properties;
    private final ArticleRepository articleRepository;
    private final NewsIngestionService ingestionService;

    public StartupNewsLoader(AppProperties properties,
                             ArticleRepository articleRepository,
                             NewsIngestionService ingestionService) {
        this.properties = properties;
        this.articleRepository = articleRepository;
        this.ingestionService = ingestionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.getRss().isFetchOnStartup()) {
            return;
        }
        if (articleRepository.count() > 0) {
            log.info("Articles already present, skipping the startup fetch");
            return;
        }
        log.info("Article table is empty, fetching RSS feeds once at startup");
        try {
            RefreshResultDto result = ingestionService.refreshAll();
            log.info("Startup fetch finished: {} saved, {} duplicates, {} failed feeds",
                    result.saved(), result.duplicates(), result.feedsFailed());
        } catch (Exception ex) {
            log.warn("Startup fetch failed: {}. The app keeps running and the scheduler will retry.", ex.getMessage());
        }
    }
}
