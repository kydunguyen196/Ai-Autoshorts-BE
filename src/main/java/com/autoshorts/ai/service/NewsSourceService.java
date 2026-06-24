package com.autoshorts.ai.service;

import com.autoshorts.ai.dto.NewsItemResponse;
import com.autoshorts.ai.dto.NewsSourceRequest;
import com.autoshorts.ai.dto.NewsSourceResponse;
import com.autoshorts.ai.dto.PagedResponse;
import com.autoshorts.ai.entity.NewsSource;
import com.autoshorts.ai.exception.ResourceNotFoundException;
import com.autoshorts.ai.repository.NewsItemRepository;
import com.autoshorts.ai.repository.NewsSourceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class NewsSourceService {

    private final NewsSourceRepository newsSourceRepository;
    private final NewsItemRepository newsItemRepository;
    private final NewsIngestionService newsIngestionService;
    private final ChannelService channelService;

    public NewsSourceService(
        NewsSourceRepository newsSourceRepository,
        NewsItemRepository newsItemRepository,
        NewsIngestionService newsIngestionService,
        ChannelService channelService
    ) {
        this.newsSourceRepository = newsSourceRepository;
        this.newsItemRepository = newsItemRepository;
        this.newsIngestionService = newsIngestionService;
        this.channelService = channelService;
    }

    @Transactional(readOnly = true)
    public List<NewsSourceResponse> list(UUID userId) {
        return newsSourceRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(userId)
            .stream().map(NewsSourceResponse::from).toList();
    }

    @Transactional
    public NewsSourceResponse create(UUID userId, NewsSourceRequest request) {
        NewsSource source = new NewsSource();
        source.setOwnerUserId(userId);
        source.setChannelId(channelService.resolveOwnedChannelIdOrDefault(userId, request.getChannelId()));
        applyRequest(source, request);
        return NewsSourceResponse.from(newsSourceRepository.save(source));
    }

    @Transactional
    public NewsSourceResponse update(UUID userId, UUID id, NewsSourceRequest request) {
        NewsSource source = requireOwned(userId, id);
        if (request.getChannelId() != null) {
            source.setChannelId(channelService.resolveOwnedChannelIdOrDefault(userId, request.getChannelId()));
        }
        applyRequest(source, request);
        return NewsSourceResponse.from(newsSourceRepository.save(source));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        NewsSource source = requireOwned(userId, id);
        newsSourceRepository.delete(source);
    }

    @Transactional
    public int fetchNow(UUID userId, UUID id) {
        NewsSource source = requireOwned(userId, id);
        return newsIngestionService.ingestSource(source);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NewsItemResponse> listItems(UUID userId, UUID sourceId, int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, limit), 100));
        if (sourceId != null) {
            requireOwned(userId, sourceId);
            return PagedResponse.from(
                newsItemRepository.findAllByNewsSourceIdOrderByCreatedAtDesc(sourceId, pageable)
                    .map(NewsItemResponse::from));
        }
        List<UUID> ownedSourceIds = newsSourceRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(userId)
            .stream().map(NewsSource::getId).toList();
        if (ownedSourceIds.isEmpty()) {
            return PagedResponse.from(org.springframework.data.domain.Page.empty(pageable));
        }
        return PagedResponse.from(
            newsItemRepository.findAllByNewsSourceIdInOrderByCreatedAtDesc(ownedSourceIds, pageable)
                .map(NewsItemResponse::from));
    }

    private void applyRequest(NewsSource source, NewsSourceRequest request) {
        source.setName(request.getName().trim());
        source.setFeedUrl(request.getFeedUrl().trim());
        source.setEnabled(request.isEnabled());
        source.setAutoPublish(request.isAutoPublish());
        source.setFetchIntervalMinutes(request.getFetchIntervalMinutes());
        source.setMaxItemsPerFetch(request.getMaxItemsPerFetch());
        source.setContentStyle(StringUtils.hasText(request.getContentStyle()) ? request.getContentStyle().trim() : null);
    }

    private NewsSource requireOwned(UUID userId, UUID id) {
        return newsSourceRepository.findByIdAndOwnerUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("News source not found: " + id));
    }
}
