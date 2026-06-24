package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.NewsFeedClient;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.exception.ExternalServiceException;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * RSS/Atom feed reader backed by Rome. Applies the configured connect/read timeouts and
 * normalizes entries into {@link NewsFeedEntry}. Missing GUIDs fall back to a hash of
 * link+title so dedupe still works.
 */
@Component
public class RssNewsFeedClient implements NewsFeedClient {

    private static final Logger log = LoggerFactory.getLogger(RssNewsFeedClient.class);

    private final AppProperties appProperties;

    public RssNewsFeedClient(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public List<NewsFeedEntry> fetch(String feedUrl, int maxItems) {
        int timeout = (int) Math.min(Integer.MAX_VALUE, appProperties.getNews().getRequestTimeoutMs());
        try {
            URLConnection connection = URI.create(feedUrl).toURL().openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestProperty("User-Agent", "AutoShortsAI/1.0 (+news-ingestion)");

            SyndFeed feed;
            try (XmlReader reader = new XmlReader(connection)) {
                feed = new SyndFeedInput().build(reader);
            }

            List<NewsFeedEntry> entries = new ArrayList<>();
            for (SyndEntry entry : feed.getEntries()) {
                if (entries.size() >= maxItems) {
                    break;
                }
                entries.add(toEntry(entry));
            }
            log.info("event=news_feed_fetched url={} returned={}", feedUrl, entries.size());
            return entries;
        } catch (Exception ex) {
            log.warn("event=news_feed_fetch_failed url={} message={}", feedUrl, ex.getMessage());
            throw new ExternalServiceException("Failed to fetch RSS feed: " + ex.getMessage());
        }
    }

    private NewsFeedEntry toEntry(SyndEntry entry) {
        String title = entry.getTitle() == null ? "(no title)" : entry.getTitle().trim();
        String link = entry.getLink();
        String summary = entry.getDescription() != null ? entry.getDescription().getValue() : null;
        Instant published = null;
        if (entry.getPublishedDate() != null) {
            published = entry.getPublishedDate().toInstant();
        } else if (entry.getUpdatedDate() != null) {
            published = entry.getUpdatedDate().toInstant();
        }
        String guid = entry.getUri();
        if (guid == null || guid.isBlank()) {
            guid = Integer.toHexString((nullSafe(link) + "|" + title).hashCode());
        }
        return new NewsFeedEntry(truncate(guid, 512), truncate(title, 1000), link, summary, published);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
