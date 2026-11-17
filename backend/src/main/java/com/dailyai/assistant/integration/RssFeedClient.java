package com.dailyai.assistant.integration;

import com.dailyai.assistant.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

@Component
public class RssFeedClient {

    private static final Logger log = LoggerFactory.getLogger(RssFeedClient.class);
    private static final int MAX_FEED_BYTES = 5 * 1024 * 1024;

    private final AppProperties properties;
    private final HttpClient httpClient;

    public RssFeedClient(AppProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getRss().getConnectTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public byte[] fetch(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(properties.getRss().getReadTimeoutMs()))
                .header("User-Agent", properties.getRss().getUserAgent())
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml;q=0.9, */*;q=0.8")
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Feed returned HTTP " + response.statusCode());
        }
        byte[] body = response.body();
        if (body == null || body.length == 0) {
            throw new IOException("Feed returned an empty body");
        }
        body = decompress(body, response.headers().firstValue("Content-Encoding").orElse(null));
        if (body.length > MAX_FEED_BYTES) {
            throw new IOException("Feed is larger than the " + MAX_FEED_BYTES + " byte limit");
        }
        log.debug("Fetched {} bytes from {}", body.length, url);
        return body;
    }

    /**
     * Some publishers compress feeds even when the request did not ask for it
     * (DeepMind returns gzip with Content-Type application/xml). XML parsers then
     * fail with "Content is not allowed in prolog", so the payload is unpacked here.
     */
    private byte[] decompress(byte[] body, String contentEncoding) throws IOException {
        String encoding = contentEncoding == null ? "" : contentEncoding.toLowerCase(Locale.ROOT).trim();
        boolean gzip = encoding.contains("gzip") || encoding.contains("x-gzip")
                || (body.length > 2 && (body[0] & 0xff) == 0x1f && (body[1] & 0xff) == 0x8b);
        boolean deflate = encoding.contains("deflate");
        if (!gzip && !deflate) {
            return body;
        }
        InputStream stream = gzip
                ? new GZIPInputStream(new java.io.ByteArrayInputStream(body))
                : new InflaterInputStream(new java.io.ByteArrayInputStream(body));
        try (InputStream input = stream) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                if (output.size() > MAX_FEED_BYTES) {
                    throw new IOException("Decompressed feed is larger than the " + MAX_FEED_BYTES + " byte limit");
                }
            }
            return output.toByteArray();
        }
    }
}
