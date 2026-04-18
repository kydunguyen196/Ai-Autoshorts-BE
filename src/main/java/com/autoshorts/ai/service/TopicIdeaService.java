package com.autoshorts.ai.service;

import com.autoshorts.ai.dto.TopicIdeaCreateRequest;
import com.autoshorts.ai.dto.TopicIdeaImportRequest;
import com.autoshorts.ai.dto.TopicIdeaImportResponse;
import com.autoshorts.ai.dto.TopicIdeaResponse;
import com.autoshorts.ai.dto.PagedResponse;
import com.autoshorts.ai.entity.TopicIdea;
import com.autoshorts.ai.entity.TopicIdeaStatus;
import com.autoshorts.ai.repository.TopicIdeaRepository;
import com.autoshorts.ai.util.TopicIdeaMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class TopicIdeaService {

    private static final Logger log = LoggerFactory.getLogger(TopicIdeaService.class);

    private static final List<TopicIdeaStatus> ACTIVE_TOPIC_STATUSES = List.of(TopicIdeaStatus.PENDING, TopicIdeaStatus.PROCESSING);

    private final TopicIdeaRepository topicIdeaRepository;
    private final PromptTemplateService promptTemplateService;
    private final ChannelService channelService;

    public TopicIdeaService(
        TopicIdeaRepository topicIdeaRepository,
        PromptTemplateService promptTemplateService,
        ChannelService channelService
    ) {
        this.topicIdeaRepository = topicIdeaRepository;
        this.promptTemplateService = promptTemplateService;
        this.channelService = channelService;
    }

    @Transactional(readOnly = true)
    public List<TopicIdeaResponse> listTopics(UUID userId, int limit, TopicIdeaStatus status) {
        return listTopicsPage(userId, 0, limit, status).getItems();
    }

    @Transactional(readOnly = true)
    public PagedResponse<TopicIdeaResponse> listTopicsPage(UUID userId, int page, int limit, TopicIdeaStatus status) {
        PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TopicIdea> topics = status == null
            ? topicIdeaRepository.findAllByUserId(userId, pageRequest)
            : topicIdeaRepository.findAllByUserIdAndStatus(userId, status, pageRequest);
        return PagedResponse.from(topics.map(TopicIdeaMapper::toResponse));
    }

    @Transactional
    public TopicIdeaResponse createTopic(UUID userId, TopicIdeaCreateRequest request) {
        UUID channelId = channelService.resolveOwnedChannelIdOrDefault(userId, request.getChannelId());
        TopicIdea topicIdea = new TopicIdea();
        topicIdea.setId(UUID.randomUUID());
        topicIdea.setUserId(userId);
        topicIdea.setChannelId(channelId);
        topicIdea.setTopic(request.getTopic().trim());
        topicIdea.setContentStyle(normalizeStyleOrNull(request.getContentStyle()));
        topicIdea.setPriority(request.getPriority() == null ? 0 : request.getPriority());
        topicIdea.setStatus(TopicIdeaStatus.PENDING);
        topicIdea.setSource(trimToNull(request.getSource()));
        topicIdea.setTags(TopicIdeaMapper.joinTags(request.getTags()));
        topicIdea.setScheduledFor(request.getScheduledFor());

        TopicIdea saved = topicIdeaRepository.save(topicIdea);
        log.info(
            "event=topic_created topicId={} userId={} channelId={} contentStyle={} priority={} scheduledFor={} source={}",
            saved.getId(),
            saved.getUserId(),
            saved.getChannelId(),
            saved.getContentStyle(),
            saved.getPriority(),
            saved.getScheduledFor(),
            saved.getSource()
        );
        return TopicIdeaMapper.toResponse(saved);
    }

    @Transactional
    public TopicIdeaImportResponse importTopics(UUID userId, TopicIdeaImportRequest request) {
        String defaultSource = trimToNull(request.getDefaultSource());
        List<TopicIdeaResponse> imported = new ArrayList<>();
        Set<String> importDedup = new HashSet<>();
        UUID defaultChannelId = channelService.resolveOwnedChannelIdOrDefault(userId, request.getDefaultChannelId());

        for (TopicIdeaCreateRequest item : request.getTopics()) {
            String normalizedTopic = normalizeTopic(item.getTopic());
            String normalizedStyle = normalizeStyleOrNull(item.getContentStyle());
            UUID channelId = channelService.resolveOwnedChannelIdOrDefault(
                userId,
                item.getChannelId() == null ? defaultChannelId : item.getChannelId()
            );
            String dedupKey = normalizedTopic + "|" + (normalizedStyle == null ? "" : normalizedStyle) + "|" + channelId;
            if (!importDedup.add(dedupKey)) {
                log.info("event=topic_import_skipped reason=duplicate_in_payload topic={} contentStyle={}", item.getTopic(), normalizedStyle);
                continue;
            }

            boolean existsActive = topicIdeaRepository.existsByTopicAndContentStyleInStatuses(
                item.getTopic().trim(),
                normalizedStyle,
                userId,
                channelId,
                ACTIVE_TOPIC_STATUSES
            );
            if (existsActive) {
                log.info("event=topic_import_skipped reason=already_active topic={} contentStyle={}", item.getTopic(), normalizedStyle);
                continue;
            }

            TopicIdeaCreateRequest createRequest = new TopicIdeaCreateRequest();
            createRequest.setTopic(item.getTopic());
            createRequest.setContentStyle(normalizedStyle);
            createRequest.setPriority(item.getPriority());
            createRequest.setSource(StringUtils.hasText(item.getSource()) ? item.getSource() : defaultSource);
            createRequest.setTags(item.getTags());
            createRequest.setChannelId(channelId);
            createRequest.setScheduledFor(item.getScheduledFor());
            imported.add(createTopic(userId, createRequest));
        }

        TopicIdeaImportResponse response = new TopicIdeaImportResponse();
        response.setTotalRequested(request.getTopics().size());
        response.setTotalImported(imported.size());
        response.setCreatedAt(Instant.now());
        response.setTopics(imported);
        log.info("event=topic_import_completed requested={} imported={}", response.getTotalRequested(), response.getTotalImported());
        return response;
    }

    @Transactional
    public List<TopicIdea> claimPendingTopicsForAutomation(int limit, Instant now) {
        if (limit <= 0) {
            return List.of();
        }

        List<TopicIdea> topics = topicIdeaRepository.findClaimablePendingTopics(now, limit);
        if (topics.isEmpty()) {
            return List.of();
        }

        for (TopicIdea topic : topics) {
            topic.setStatus(TopicIdeaStatus.PROCESSING);
        }
        List<TopicIdea> saved = topicIdeaRepository.saveAll(topics);
        log.info("event=topic_claimed_for_automation count={} topicIds={}", saved.size(), saved.stream().map(TopicIdea::getId).toList());
        return saved;
    }

    @Transactional
    public void markTopicUsed(UUID topicId, String sourceFallback) {
        TopicIdea topicIdea = topicIdeaRepository.findByIdForUpdate(topicId).orElse(null);
        if (topicIdea == null) {
            log.warn("event=topic_mark_used_skipped reason=not_found topicId={}", topicId);
            return;
        }

        if (topicIdea.getStatus() != TopicIdeaStatus.PROCESSING) {
            log.info("event=topic_mark_used_skipped reason=unexpected_status topicId={} status={}", topicId, topicIdea.getStatus());
            return;
        }

        topicIdea.setStatus(TopicIdeaStatus.USED);
        topicIdea.setLastUsedAt(Instant.now());
        if (!StringUtils.hasText(topicIdea.getSource()) && StringUtils.hasText(sourceFallback)) {
            topicIdea.setSource(sourceFallback.trim());
        }
        topicIdeaRepository.save(topicIdea);
        log.info("event=topic_marked_used topicId={}", topicId);
    }

    @Transactional
    public void markTopicFailed(UUID topicId, String reason) {
        TopicIdea topicIdea = topicIdeaRepository.findByIdForUpdate(topicId).orElse(null);
        if (topicIdea == null) {
            log.warn("event=topic_mark_failed_skipped reason=not_found topicId={}", topicId);
            return;
        }
        if (topicIdea.getStatus() != TopicIdeaStatus.PROCESSING) {
            log.info("event=topic_mark_failed_skipped reason=unexpected_status topicId={} status={}", topicId, topicIdea.getStatus());
            return;
        }
        topicIdea.setStatus(TopicIdeaStatus.FAILED);
        topicIdeaRepository.save(topicIdea);
        log.warn("event=topic_marked_failed topicId={} reason={}", topicId, reason);
    }

    @Transactional
    public void requeueTopic(UUID topicId, Instant nextScheduledFor, String reason) {
        TopicIdea topicIdea = topicIdeaRepository.findByIdForUpdate(topicId).orElse(null);
        if (topicIdea == null) {
            log.warn("event=topic_requeue_skipped reason=not_found topicId={}", topicId);
            return;
        }
        if (topicIdea.getStatus() != TopicIdeaStatus.PROCESSING) {
            log.info("event=topic_requeue_skipped reason=unexpected_status topicId={} status={}", topicId, topicIdea.getStatus());
            return;
        }
        topicIdea.setStatus(TopicIdeaStatus.PENDING);
        topicIdea.setScheduledFor(nextScheduledFor);
        topicIdeaRepository.save(topicIdea);
        log.info("event=topic_requeued topicId={} nextScheduledFor={} reason={}", topicId, nextScheduledFor, reason);
    }

    private String normalizeStyleOrNull(String style) {
        if (!StringUtils.hasText(style)) {
            return null;
        }
        return promptTemplateService.normalizeStyle(style);
    }

    private String normalizeTopic(String topic) {
        return topic == null ? "" : topic.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
