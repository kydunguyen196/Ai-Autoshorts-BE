package com.autoshorts.ai.service;

import com.autoshorts.ai.client.TikTokApiClient;
import com.autoshorts.ai.client.model.TikTokPublishStatus;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.PublishStatus;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.repository.VideoJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Polls TikTok for the final status of posts that were submitted via PULL_FROM_URL and finalizes
 * the corresponding video job (PUBLISHED / PUBLISH_FAILED). The publisher only initializes the
 * post; this service turns the asynchronous TikTok processing into a definitive job state.
 */
@Service
public class TikTokPublishStatusService {

    private static final Logger log = LoggerFactory.getLogger(TikTokPublishStatusService.class);

    private static final String PROVIDER_KEY = "tiktok";

    private final VideoJobRepository videoJobRepository;
    private final TikTokApiClient tikTokApiClient;
    private final TikTokOAuthService tikTokOAuthService;
    private final VideoPublishService videoPublishService;
    private final AppProperties appProperties;

    public TikTokPublishStatusService(
        VideoJobRepository videoJobRepository,
        TikTokApiClient tikTokApiClient,
        TikTokOAuthService tikTokOAuthService,
        VideoPublishService videoPublishService,
        AppProperties appProperties
    ) {
        this.videoJobRepository = videoJobRepository;
        this.tikTokApiClient = tikTokApiClient;
        this.tikTokOAuthService = tikTokOAuthService;
        this.videoPublishService = videoPublishService;
        this.appProperties = appProperties;
    }

    /**
     * Poll a batch of pending TikTok posts and advance their job state.
     *
     * @return number of jobs that reached a terminal state (PUBLISHED or PUBLISH_FAILED) this cycle
     */
    public int reconcilePendingPublishes() {
        if (!appProperties.getTiktok().isDirectPublishEnabled()) {
            return 0;
        }
        List<VideoJob> pending = videoJobRepository.findPendingAsyncPublishes(
            PublishStatus.PUBLISHING,
            PROVIDER_KEY,
            PageRequest.of(0, appProperties.getTiktok().getStatusPollBatchSize())
        );

        int finalized = 0;
        for (VideoJob job : pending) {
            try {
                if (reconcileOne(job)) {
                    finalized++;
                }
            } catch (Exception ex) {
                // Don't let one bad job stall the batch; record the check and move on.
                log.warn("event=tiktok_status_poll_error jobId={} message={}", job.getId(), ex.getMessage());
                safeTouch(job.getId());
            }
        }
        return finalized;
    }

    private boolean reconcileOne(VideoJob job) {
        String accessToken = tikTokOAuthService.getValidAccessToken(job.getUserId(), job.getChannelId());
        TikTokPublishStatus status = tikTokApiClient.fetchPostStatus(accessToken, job.getPublishExternalId());

        if (status.isTerminalSuccess()) {
            videoPublishService.completeAsyncPublish(job.getId(), status.publicPostId(), status.rawResponseJson());
            return true;
        }
        if (status.isTerminalFailure()) {
            String reason = StringUtils.hasText(status.failReason())
                ? "TikTok publish failed: " + status.failReason()
                : "TikTok publish failed";
            videoPublishService.failAsyncPublish(job.getId(), reason, status.rawResponseJson());
            return true;
        }
        if (hasTimedOut(job)) {
            videoPublishService.failAsyncPublish(
                job.getId(),
                "TikTok publish did not complete within the timeout window (last status: " + status.status() + ")",
                status.rawResponseJson()
            );
            log.warn("event=tiktok_status_poll_timeout jobId={} lastStatus={}", job.getId(), status.status());
            return true;
        }
        // Still processing — record the check so ordering stays fair and move on.
        videoPublishService.touchPublishStatusCheck(job.getId());
        return false;
    }

    private boolean hasTimedOut(VideoJob job) {
        Instant startedAt = job.getPublishStartedAt();
        if (startedAt == null) {
            return false;
        }
        Duration timeout = Duration.ofMinutes(appProperties.getTiktok().getStatusTimeoutMinutes());
        return startedAt.plus(timeout).isBefore(Instant.now());
    }

    private void safeTouch(java.util.UUID jobId) {
        try {
            videoPublishService.touchPublishStatusCheck(jobId);
        } catch (Exception ex) {
            log.debug("event=tiktok_status_touch_failed jobId={} message={}", jobId, ex.getMessage());
        }
    }
}
