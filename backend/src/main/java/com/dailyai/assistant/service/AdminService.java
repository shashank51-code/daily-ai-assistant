package com.dailyai.assistant.service;

import com.dailyai.assistant.config.AppProperties;
import com.dailyai.assistant.dto.ExplanationDto;
import com.dailyai.assistant.dto.RefreshResultDto;
import com.dailyai.assistant.entity.Article;
import com.dailyai.assistant.entity.ArticleExplanation;
import com.dailyai.assistant.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminService {

    private final NewsIngestionService ingestionService;
    private final ArticleService articleService;
    private final ExplanationService explanationService;
    private final ArticleAiGenerationGuard aiGenerationGuard;
    private final AppProperties properties;

    public AdminService(NewsIngestionService ingestionService,
                        ArticleService articleService,
                        ExplanationService explanationService,
                        ArticleAiGenerationGuard aiGenerationGuard,
                        AppProperties properties) {
        this.ingestionService = ingestionService;
        this.articleService = articleService;
        this.explanationService = explanationService;
        this.aiGenerationGuard = aiGenerationGuard;
        this.properties = properties;
    }

    public RefreshResultDto refreshNews() {
        return ingestionService.refreshAll();
    }

    public ExplanationDto explain(Long articleId) {
        if (!explanationService.isAiEnabled()) {
            throw new BadRequestException(
                    "AI explanations are disabled. Set AI_ENABLED=true and AI_API_KEY to use this endpoint.");
        }
        Article article = articleService.requireArticle(articleId);
        ArticleExplanation explanation = aiGenerationGuard.run(articleId,
                () -> explanationService.regenerateWithAi(article));
        return ExplanationDto.from(explanation);
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("feedCount", properties.getRss().getFeeds().size());
        status.put("feeds", properties.getRss().getFeeds());
        status.put("refreshRunning", ingestionService.isRunning());
        status.put("lastRefreshFinishedAt", ingestionService.getLastSuccessfulRun());
        status.put("aiEnabled", explanationService.isAiEnabled());
        status.put("adminKeyRequired", !properties.getAdminApiKey().isBlank());
        return status;
    }
}
