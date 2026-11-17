package com.dailyai.assistant.integration;

import com.dailyai.assistant.config.AppProperties;
import com.dailyai.assistant.entity.Article;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Optional AI integration. Any OpenAI compatible chat completions endpoint works, so the
 * application is not tied to one vendor: only the base URL, model name and API key change.
 *
 * Resilience rules:
 * - the provider is skipped entirely when no key is configured;
 * - HTTP 429 and 5xx responses are retried with a bounded exponential backoff, honouring
 *   a Retry-After header when the provider sends one;
 * - timeouts and network errors are retried and then swallowed;
 * - after repeated failures the client backs off for a cooldown window so article pages
 *   keep loading quickly instead of waiting on a broken provider.
 *
 * Callers always fall back to the offline explanation built from the feed text.
 */
@Component
public class AiClient implements ExplanationProvider {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);

    private static final String SYSTEM_PROMPT = """
            You are a patient AI mentor explaining industry news to a fresher who knows basic programming.
            Rules:
            - Use simple English and short sentences.
            - Never invent facts. Only use the supplied title and description.
            - If the supplied text does not contain enough detail for a field, write exactly:
              "Not available in the provided text."
            - Do not claim a publication date, price or benchmark that is not in the supplied text.
            Return ONLY a JSON object with these keys:
            whatHappened, technology, importance, howItWorks, whereUsed, fresherLearning,
            practicalExample, relatedTechnologies.
            """;

    private static final long MAX_BACKOFF_MS = 10_000;

    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong cooldownUntil = new AtomicLong(0);

    public AiClient(AppProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofMillis(properties.getAi().getTimeoutMs());
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);

        this.restClient = RestClient.builder()
                .baseUrl(properties.getAi().getBaseUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAi().getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public boolean isEnabled() {
        return properties.getAi().isEnabled();
    }

    @Override
    public boolean isAvailable() {
        return isEnabled() && !isCoolingDown();
    }

    /** True while the client skips calls after repeated provider failures. */
    public boolean isCoolingDown() {
        return System.currentTimeMillis() < cooldownUntil.get();
    }

    @Override
    public Optional<ExplanationDraft> generateExplanation(Article article) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        if (isCoolingDown()) {
            log.debug("Skipping AI explanation for article {}: provider in cooldown", article.getId());
            return Optional.empty();
        }

        int maxAttempts = Math.max(1, properties.getAi().getMaxAttempts());
        boolean jsonMode = true;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ProviderResponse response = callProvider(article, jsonMode);
                if (response.status() == 200) {
                    Optional<JsonNode> json = extractJson(response.body());
                    if (json.isPresent()) {
                        Optional<ExplanationDraft> draft = json
                                .map(this::toDraft)
                                .filter(ExplanationDraft::isComplete);
                        if (draft.isPresent()) {
                            recordSuccess();
                            return draft;
                        }
                    }
                    // A parseable provider that returns unusable JSON is usually a model that
                    // does not support response_format, so retry once without JSON mode.
                    jsonMode = false;
                    if (attempt < maxAttempts) {
                        continue;
                    }
                    recordFailure("unusable JSON payload", article);
                } else if (response.status() == 429 || response.status() >= 500) {
                    log.warn("AI provider returned HTTP {} for article {} (attempt {}/{})",
                            response.status(), article.getId(), attempt, maxAttempts);
                    if (attempt < maxAttempts) {
                        sleep(backoffMs(attempt, response.retryAfter()));
                        continue;
                    }
                    recordFailure("HTTP " + response.status(), article);
                } else {
                    // 400/401/403 are configuration problems; retrying cannot help.
                    recordFailure("HTTP " + response.status(), article);
                    break;
                }
            } catch (ResourceAccessException ex) {
                log.warn("AI provider unreachable for article {} (attempt {}/{}): {}",
                        article.getId(), attempt, maxAttempts, ex.getMessage());
                if (attempt < maxAttempts) {
                    sleep(backoffMs(attempt, null));
                    continue;
                }
                recordFailure("timeout or network error", article);
            } catch (Exception ex) {
                log.warn("AI explanation failed for article {}: {}", article.getId(), ex.getMessage());
                recordFailure(ex.getClass().getSimpleName(), article);
                break;
            }
        }
        return Optional.empty();
    }

    private ProviderResponse callProvider(Article article, boolean jsonMode) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getAi().getModel());
        payload.put("temperature", 0.2);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", buildUserPrompt(article))
        ));
        if (jsonMode) {
            payload.put("response_format", Map.of("type", "json_object"));
        }

        return restClient.post()
                .uri("/chat/completions")
                .body(payload)
                .exchange((request, response) -> new ProviderResponse(
                        response.getStatusCode().value(),
                        new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8),
                        response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)));
    }

    private long backoffMs(int attempt, String retryAfter) {
        Long fromHeader = parseRetryAfter(retryAfter);
        if (fromHeader != null) {
            return Math.min(fromHeader, MAX_BACKOFF_MS);
        }
        long base = Math.max(0, properties.getAi().getRetryDelayMs());
        long delay = base * (1L << Math.min(attempt - 1, 4));
        return Math.min(delay, MAX_BACKOFF_MS);
    }

    private Long parseRetryAfter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Math.max(0, Long.parseLong(value.trim()) * 1000);
        } catch (NumberFormatException ignored) {
            // Retry-After may also be an HTTP date; fall back to exponential backoff.
            return null;
        }
    }

    private void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void recordSuccess() {
        consecutiveFailures.set(0);
    }

    private void recordFailure(String reason, Article article) {
        int failures = consecutiveFailures.incrementAndGet();
        int threshold = Math.max(1, properties.getAi().getFailureThreshold());
        log.warn("AI explanation unavailable for article {} ({}); consecutive failures {}/{}",
                article.getId(), reason, failures, threshold);
        if (failures >= threshold) {
            cooldownUntil.set(System.currentTimeMillis() + Math.max(0, properties.getAi().getCooldownMs()));
            consecutiveFailures.set(0);
            log.warn("Pausing AI explanations for {} ms after repeated failures",
                    properties.getAi().getCooldownMs());
        }
    }

    private String buildUserPrompt(Article article) {
        String published = article.getPublishedAt() == null
                ? "not provided by the feed"
                : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                        article.getPublishedAt().atZone(ZoneId.of(properties.getTimezone())));
        return """
                Article title: %s
                Source: %s
                Category: %s
                Published: %s
                Feed description: %s
                """.formatted(
                article.getTitle(),
                article.getSource() == null ? "unknown" : article.getSource(),
                article.getCategory() == null ? "unknown" : article.getCategory().getName(),
                published,
                article.getDescription() == null || article.getDescription().isBlank()
                        ? "(no description supplied by the feed)"
                        : article.getDescription());
    }

    private Optional<JsonNode> extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                return Optional.empty();
            }
            String cleaned = stripCodeFence(content);
            return Optional.of(objectMapper.readTree(cleaned));
        } catch (Exception ex) {
            log.debug("Could not parse AI response: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String stripCodeFence(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private ExplanationDraft toDraft(JsonNode node) {
        return new ExplanationDraft(
                text(node, "whatHappened"),
                text(node, "technology"),
                text(node, "importance"),
                text(node, "howItWorks"),
                text(node, "whereUsed"),
                text(node, "fresherLearning"),
                text(node, "practicalExample"),
                text(node, "relatedTechnologies")
        );
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private record ProviderResponse(int status, String body, String retryAfter) {
    }
}
