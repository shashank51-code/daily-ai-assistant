package com.dailyai.assistant.integration;

import com.dailyai.assistant.config.AppProperties;
import com.dailyai.assistant.entity.Article;
import com.dailyai.assistant.entity.Category;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real AiClient against a local mock provider that speaks the
 * OpenAI chat completions protocol, so no external API key is needed.
 */
class AiClientTest {

    private HttpServer server;
    private final List<StubResponse> responses = new ArrayList<>();
    private final AtomicInteger requestCount = new AtomicInteger();
    private AppProperties properties;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", this::handle);
        server.setExecutor(null);
        server.start();

        properties = new AppProperties();
        properties.setTimezone("Asia/Kolkata");
        properties.getAi().setEnabled(true);
        properties.getAi().setApiKey("test-key");
        properties.getAi().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.getAi().setModel("mock-model");
        properties.getAi().setTimeoutMs(2000);
        properties.getAi().setMaxAttempts(2);
        properties.getAi().setRetryDelayMs(10);
        properties.getAi().setFailureThreshold(2);
        properties.getAi().setCooldownMs(60_000);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void withoutApiKeyTheProviderIsDisabledAndNeverCalled() {
        properties.getAi().setApiKey("");

        AiClient client = new AiClient(properties, new ObjectMapper());

        assertThat(client.isEnabled()).isFalse();
        assertThat(client.generateExplanation(article())).isEmpty();
        assertThat(requestCount.get()).isZero();
    }

    @Test
    void generatesDraftFromProviderResponse() {
        responses.add(StubResponse.json(200, completion("""
                {"whatHappened":"A new model shipped.","technology":"A neural network.",
                 "importance":"It is faster.","howItWorks":"Not available in the provided text.",
                 "whereUsed":"Chat.","fresherLearning":"Learn transformers.",
                 "practicalExample":"Build a demo.","relatedTechnologies":"LLMs, RAG"}
                """)));

        AiClient client = new AiClient(properties, new ObjectMapper());
        Optional<ExplanationDraft> draft = client.generateExplanation(article());

        assertThat(draft).isPresent();
        assertThat(draft.get().whatHappened()).isEqualTo("A new model shipped.");
        assertThat(draft.get().isComplete()).isTrue();
        assertThat(requestCount.get()).isEqualTo(1);
    }

    @Test
    void retriesRateLimitThenSucceeds() {
        responses.add(StubResponse.plain(429, "{\"error\":\"rate limited\"}", "0"));
        responses.add(StubResponse.json(200, completion("""
                {"whatHappened":"A.","technology":"B.","importance":"C."}
                """)));

        AiClient client = new AiClient(properties, new ObjectMapper());
        Optional<ExplanationDraft> draft = client.generateExplanation(article());

        assertThat(draft).isPresent();
        assertThat(requestCount.get()).isEqualTo(2);
    }

    @Test
    void unusablePayloadFallsBackAfterBoundedRetries() {
        responses.add(StubResponse.json(200, completion("I cannot answer that.")));
        responses.add(StubResponse.json(200, completion("Still not JSON.")));

        AiClient client = new AiClient(properties, new ObjectMapper());

        assertThat(client.generateExplanation(article())).isEmpty();
        assertThat(requestCount.get()).isEqualTo(2);
    }

    @Test
    void repeatedServerErrorsOpenCooldownAndSkipFurtherCalls() {
        properties.getAi().setMaxAttempts(1);
        properties.getAi().setFailureThreshold(1);
        responses.add(StubResponse.plain(503, "{\"error\":\"down\"}", null));

        AiClient client = new AiClient(properties, new ObjectMapper());

        assertThat(client.generateExplanation(article())).isEmpty();
        assertThat(client.isCoolingDown()).isTrue();
        assertThat(client.isAvailable()).isFalse();

        // No second stub is queued: a call here would hit the mock and fail the count.
        assertThat(client.generateExplanation(article())).isEmpty();
        assertThat(requestCount.get()).isEqualTo(1);
    }

    @Test
    void timeoutDoesNotThrowAndReturnsEmpty() throws IOException {
        properties.getAi().setTimeoutMs(300);
        properties.getAi().setMaxAttempts(1);
        properties.getAi().setFailureThreshold(5);
        responses.add(StubResponse.delayed(200, completion("""
                {"whatHappened":"A.","technology":"B.","importance":"C."}
                """), 1500));

        AiClient client = new AiClient(properties, new ObjectMapper());

        assertThat(client.generateExplanation(article())).isEmpty();
        assertThat(client.isCoolingDown()).isFalse();
    }

    private void handle(HttpExchange exchange) throws IOException {
        requestCount.incrementAndGet();
        try (InputStream body = exchange.getRequestBody()) {
            body.readAllBytes();
        }
        StubResponse stub = responses.isEmpty()
                ? StubResponse.plain(500, "{\"error\":\"no stub\"}", null)
                : responses.remove(0);
        if (stub.delayMs() > 0) {
            try {
                Thread.sleep(stub.delayMs());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        if (stub.retryAfter() != null) {
            exchange.getResponseHeaders().add("Retry-After", stub.retryAfter());
        }
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        byte[] bytes = stub.body().getBytes(StandardCharsets.UTF_8);
        try {
            exchange.sendResponseHeaders(stub.status(), bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (IOException ignored) {
            // The client may have already timed out and closed the connection.
        } finally {
            exchange.close();
        }
    }

    private String completion(String contentJson) {
        try {
            return new ObjectMapper().writeValueAsString(
                    java.util.Map.of("choices", List.of(
                            java.util.Map.of("message", java.util.Map.of("content", contentJson)))));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Article article() {
        Category category = new Category("Generative AI", "generative-ai", null);
        Article article = new Article();
        article.setId(1L);
        article.setTitle("New model ships");
        article.setUrl("https://example.com/a");
        article.setDescription("A short feed description.");
        article.setSource("Example Feed");
        article.setCategory(category);
        return article;
    }

    private record StubResponse(int status, String body, String retryAfter, long delayMs) {
        static StubResponse json(int status, String body) {
            return new StubResponse(status, body, null, 0);
        }

        static StubResponse plain(int status, String body, String retryAfter) {
            return new StubResponse(status, body, retryAfter, 0);
        }

        static StubResponse delayed(int status, String body, long delayMs) {
            return new StubResponse(status, body, null, delayMs);
        }
    }
}
