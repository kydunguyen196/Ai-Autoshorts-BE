package com.autoshorts.ai.service;

import com.autoshorts.ai.client.NewsFeedClient;
import com.autoshorts.ai.entity.NewsItem;
import com.autoshorts.ai.entity.NewsItemStatus;
import com.autoshorts.ai.entity.NewsSource;
import com.autoshorts.ai.entity.NotificationType;
import com.autoshorts.ai.entity.TopicIdea;
import com.autoshorts.ai.entity.TopicIdeaStatus;
import com.autoshorts.ai.repository.NewsItemRepository;
import com.autoshorts.ai.repository.NewsSourceRepository;
import com.autoshorts.ai.repository.TopicIdeaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Pulls entries from configured RSS sources and turns new ones into {@link TopicIdea}
 * records (status PENDING) that the existing {@link TopicAutomationService} then converts
 * into video jobs. This is the single integration seam between news and the video pipeline.
 */
@Service
public class NewsIngestionService {

    private static final Logger log = LoggerFactory.getLogger(NewsIngestionService.class);

    private final NewsSourceRepository newsSourceRepository;
    private final NewsItemRepository newsItemRepository;
    private final TopicIdeaRepository topicIdeaRepository;
    private final NewsFeedClient newsFeedClient;
    private final NotificationService notificationService;

    public NewsIngestionService(
        NewsSourceRepository newsSourceRepository,
        NewsItemRepository newsItemRepository,
        TopicIdeaRepository topicIdeaRepository,
        NewsFeedClient newsFeedClient,
        NotificationService notificationService
    ) {
        this.newsSourceRepository = newsSourceRepository;
        this.newsItemRepository = newsItemRepository;
        this.topicIdeaRepository = topicIdeaRepository;
        this.newsFeedClient = newsFeedClient;
        this.notificationService = notificationService;
    }

    /** Fetch + convert every enabled source that is due for a refresh. Returns total new items. */
    public int ingestDueSources() {
        Instant now = Instant.now();
        int total = 0;
        for (NewsSource source : newsSourceRepository.findAllByEnabledTrue()) {
            if (!isDue(source, now)) {
                continue;
            }
            try {
                total += ingestSource(source);
            } catch (Exception ex) {
                log.warn("event=news_ingest_source_failed sourceId={} message={}", source.getId(), ex.getMessage());
            }
        }
        return total;
    }

    /** Fetch a single source and convert new entries into topic ideas. Returns count of new items. */
    @Transactional
    public int ingestSource(NewsSource source) {
        int created = 0;
        try {
            List<NewsFeedClient.NewsFeedEntry> entries =
                newsFeedClient.fetch(source.getFeedUrl(), Math.max(1, source.getMaxItemsPerFetch()));
            for (NewsFeedClient.NewsFeedEntry entry : entries) {
                if (newsItemRepository.existsByNewsSourceIdAndExternalGuid(source.getId(), entry.guid())) {
                    continue;
                }
                NewsItem item = new NewsItem();
                item.setNewsSourceId(source.getId());
                item.setExternalGuid(entry.guid());
                item.setTitle(entry.title());
                item.setLink(entry.link());
                item.setSummary(entry.summary());
                item.setPublishedAt(entry.publishedAt());
                item.setStatus(NewsItemStatus.NEW);

                TopicIdea topic = toTopicIdea(source, entry);
                TopicIdea savedTopic = topicIdeaRepository.save(topic);

                item.setTopicIdeaId(savedTopic.getId());
                item.setStatus(NewsItemStatus.CONVERTED);
                newsItemRepository.save(item);
                created++;
            }
            source.setLastFetchedAt(Instant.now());
            source.setLastStatus("OK");
            source.setLastError(null);
            newsSourceRepository.save(source);
            log.info("event=news_ingest_completed sourceId={} newItems={}", source.getId(), created);

            if (created > 0) {
                safeNotify(source, created);
            }
        } catch (Exception ex) {
            source.setLastFetchedAt(Instant.now());
            source.setLastStatus("ERROR");
            source.setLastError(truncate(ex.getMessage(), 1000));
            newsSourceRepository.save(source);
            throw ex;
        }
        return created;
    }

    private TopicIdea toTopicIdea(NewsSource source, NewsFeedClient.NewsFeedEntry entry) {
        TopicIdea topic = new TopicIdea();
        topic.setUserId(source.getOwnerUserId());
        topic.setChannelId(source.getChannelId());
        topic.setTopic(truncate(entry.title(), 500));
        if (StringUtils.hasText(source.getContentStyle())) {
            topic.setContentStyle(source.getContentStyle().trim());
        }
        topic.setStatus(TopicIdeaStatus.PENDING);
        topic.setSource("news");
        topic.setScheduledFor(Instant.now());
        topic.setAutoPublish(source.isAutoPublish());
        return topic;
    }

    private boolean isDue(NewsSource source, Instant now) {
        if (source.getLastFetchedAt() == null) {
            return true;
        }
        Duration interval = Duration.ofMinutes(Math.max(1, source.getFetchIntervalMinutes()));
        return source.getLastFetchedAt().plus(interval).isBefore(now);
    }

    private void safeNotify(NewsSource source, int created) {
        try {
            notificationService.notify(
                source.getOwnerUserId(),
                NotificationType.NEWS_INGESTED,
                "Đã nhập tin tức mới",
                "Nguồn \"" + source.getName() + "\" có " + created + " tin mới được đưa vào hàng đợi tạo video.",
                Map.of("newsSourceId", source.getId().toString(), "count", created)
            );
        } catch (Exception ex) {
            log.debug("event=news_ingest_notify_failed sourceId={} message={}", source.getId(), ex.getMessage());
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
