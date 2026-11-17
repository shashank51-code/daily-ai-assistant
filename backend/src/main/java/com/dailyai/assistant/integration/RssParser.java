package com.dailyai.assistant.integration;

import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RssParser {

    private static final Logger log = LoggerFactory.getLogger(RssParser.class);

    /**
     * Parses a feed into article candidates. Never invents a publication date:
     * when the feed has no date the value stays null.
     */
    public List<ParsedArticle> parse(byte[] body, String configuredSource) throws Exception {
        SyndFeedInput input = new SyndFeedInput();
        input.setAllowDoctypes(true);

        SyndFeed feed;
        try (XmlReader reader = new XmlReader(new ByteArrayInputStream(body))) {
            feed = input.build(reader);
        }

        String feedTitle = feed.getTitle() == null || feed.getTitle().isBlank()
                ? configuredSource
                : feed.getTitle().trim();

        Map<String, ParsedArticle> byUrl = new LinkedHashMap<>();
        for (SyndEntry entry : feed.getEntries()) {
            String title = TextUtils.cleanHtml(entry.getTitle());
            String link = entry.getLink();
            if (link == null || link.isBlank()) {
                link = entry.getUri();
            }
            if (title == null || title.isBlank() || link == null || link.isBlank()) {
                continue;
            }
            link = link.trim();
            if (!link.startsWith("http://") && !link.startsWith("https://")) {
                continue;
            }

            String description = entry.getDescription() != null
                    ? entry.getDescription().getValue()
                    : firstContent(entry);
            description = TextUtils.truncate(TextUtils.cleanHtml(description), 2000);

            Instant published = null;
            if (entry.getPublishedDate() != null) {
                published = entry.getPublishedDate().toInstant();
            } else if (entry.getUpdatedDate() != null) {
                published = entry.getUpdatedDate().toInstant();
            }

            String source = feedTitle;
            if (entry.getAuthor() != null && !entry.getAuthor().isBlank()) {
                source = feedTitle + " / " + entry.getAuthor().trim();
            }

            String image = findImage(entry);
            ParsedArticle article = new ParsedArticle(title.trim(), link, description, published,
                    TextUtils.truncate(source, 200), image);
            byUrl.putIfAbsent(link, article);
        }

        log.debug("Parsed {} unique entries from feed {}", byUrl.size(), feedTitle);
        return new ArrayList<>(byUrl.values());
    }

    private String firstContent(SyndEntry entry) {
        return entry.getContents().stream()
                .findFirst()
                .map(content -> content.getValue())
                .orElse(null);
    }

    private String findImage(SyndEntry entry) {
        List<SyndEnclosure> enclosures = entry.getEnclosures();
        if (enclosures != null) {
            for (SyndEnclosure enclosure : enclosures) {
                if (enclosure.getType() != null && enclosure.getType().startsWith("image/")
                        && enclosure.getUrl() != null) {
                    return enclosure.getUrl();
                }
            }
        }
        List<Element> foreign = entry.getForeignMarkup();
        if (foreign != null) {
            for (Element element : foreign) {
                String localName = element.getName();
                if (("content".equals(localName) || "thumbnail".equals(localName))
                        && element.getAttributeValue("url") != null) {
                    return element.getAttributeValue("url");
                }
            }
        }
        return null;
    }
}
