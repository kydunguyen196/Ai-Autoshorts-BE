package com.autoshorts.ai.service;

import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.dto.GenerateVideoRequest;
import com.autoshorts.ai.dto.VideoJobResponse;
import com.autoshorts.ai.entity.TopicIdea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Service
public class TopicAutomationService {

    private static final Logger log = LoggerFactory.getLogger(TopicAutomationService.class);

    private final AppProperties appProperties;
    private final TopicIdeaService topicIdeaService;
    private final VideoJobService videoJobService;
    private final VideoJobDispatchService videoJobDispatchService;

    public TopicAutomationService(
        AppProperties appProperties,
        TopicIdeaService topicIdeaService,
        VideoJobService videoJobService,
        VideoJobDispatchService videoJobDispatchService
    ) {
        this.appProperties = appProperties;
        this.topicIdeaService = topicIdeaService;
        this.videoJobService = videoJobService;
        this.videoJobDispatchService = videoJobDispatchService;
    }

    public int dispatchPendingTopics() {
        int batchSize = Math.max(1, appProperties.getScheduler().getBatchSize());
        List<TopicIdea> claimed = topicIdeaService.claimPendingTopicsForAutomation(batchSize, Instant.now());
        if (claimed.isEmpty()) {
            log.info("event=scheduler_dispatch_empty batchSize={}", batchSize);
            return 0;
        }

        int dispatched = 0;
        for (TopicIdea topicIdea : claimed) {
            try {
                GenerateVideoRequest request = toGenerateRequest(topicIdea);
                if (videoJobService.hasActiveJobFor(
                    request.getTopic(),
                    request.getStyle(),
                    topicIdea.getUserId(),
                    topicIdea.getChannelId()
                )) {
                    Instant nextRetryAt = Instant.now().plusMillis(appProperties.getScheduler().getFixedDelayMs());
                    topicIdeaService.requeueTopic(topicIdea.getId(), nextRetryAt, "Duplicate active video job exists for topic/style");
                    log.info(
                        "event=scheduler_topic_skipped reason=active_job_exists topicId={} topic={} style={} requeuedFor={}",
                        topicIdea.getId(),
                        request.getTopic(),
                        request.getStyle(),
                        nextRetryAt
                    );
                    continue;
                }
                VideoJobResponse response = videoJobService.createPendingJob(
                    request,
                    topicIdea.getUserId(),
                    topicIdea.getChannelId()
                );
                try {
                    videoJobDispatchService.publishOrMarkFailed(
                        response.getJobId(),
                        "scheduler_dispatch",
                        "Scheduler dispatch failed before queue publish"
                    );
                } catch (Exception dispatchEx) {
                    throw dispatchEx;
                }
                topicIdeaService.markTopicUsed(topicIdea.getId(), appProperties.getScheduler().getTopicSource());
                dispatched++;
                log.info(
                    "event=scheduler_topic_dispatched topicId={} jobId={} topic={} contentStyle={}",
                    topicIdea.getId(),
                    response.getJobId(),
                    topicIdea.getTopic(),
                    topicIdea.getContentStyle()
                );
            } catch (Exception ex) {
                topicIdeaService.markTopicFailed(topicIdea.getId(), ex.getMessage());
                log.error(
                    "event=scheduler_topic_dispatch_failed topicId={} topic={} message={}",
                    topicIdea.getId(),
                    topicIdea.getTopic(),
                    ex.getMessage(),
                    ex
                );
            }
        }

        log.info("event=scheduler_dispatch_completed claimed={} dispatched={}", claimed.size(), dispatched);
        return dispatched;
    }

    private GenerateVideoRequest toGenerateRequest(TopicIdea topicIdea) {
        String style = StringUtils.hasText(topicIdea.getContentStyle())
            ? topicIdea.getContentStyle().trim()
            : PromptTemplateService.DEFAULT_STYLE;

        GenerateVideoRequest request = new GenerateVideoRequest();
        request.setTopic(topicIdea.getTopic());
        request.setStyle(style);
        request.setContentStyle(style);
        request.setChannelId(topicIdea.getChannelId());
        request.setDurationSeconds(appProperties.getScheduler().getDefaultDurationSeconds());
        request.setVoiceId(trimToNull(appProperties.getScheduler().getDefaultVoiceId()));
        return request;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
