package com.dailyai.assistant.integration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RssParserTest {

    private final RssParser parser = new RssParser();

    private static final String RSS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Example AI Blog</title>
                <link>https://example.com</link>
                <description>Example feed</description>
                <item>
                  <title>New open model released</title>
                  <link>https://example.com/posts/open-model?utm_source=rss</link>
                  <description><![CDATA[<p>A new <b>open weight</b> model was released.</p>]]></description>
                  <pubDate>Mon, 16 Nov 2026 09:30:00 GMT</pubDate>
                  <author>Jane Doe</author>
                </item>
                <item>
                  <title>No date item</title>
                  <link>https://example.com/posts/no-date</link>
                  <description>Text only.</description>
                </item>
                <item>
                  <title>Duplicate link</title>
                  <link>https://example.com/posts/no-date</link>
                </item>
              </channel>
            </rss>
            """;

    @Test
    void parsesEntriesWithoutInventingDates() throws Exception {
        List<ParsedArticle> articles = parser.parse(RSS.getBytes(StandardCharsets.UTF_8), "fallback");

        assertThat(articles).hasSize(2);
        ParsedArticle first = articles.get(0);
        assertThat(first.title()).isEqualTo("New open model released");
        assertThat(first.url()).isEqualTo("https://example.com/posts/open-model?utm_source=rss");
        assertThat(first.description()).isEqualTo("A new open weight model was released.");
        assertThat(first.publishedAt()).isEqualTo(Instant.parse("2026-11-16T09:30:00Z"));
        assertThat(first.source()).isEqualTo("Example AI Blog / Jane Doe");

        ParsedArticle second = articles.get(1);
        assertThat(second.title()).isEqualTo("No date item");
        assertThat(second.publishedAt()).isNull();
        assertThat(second.source()).isEqualTo("Example AI Blog");
    }

    @Test
    void rejectsMalformedXml() {
        byte[] broken = "<rss><channel><item>".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> parser.parse(broken, "broken")).isInstanceOf(Exception.class);
    }
}
