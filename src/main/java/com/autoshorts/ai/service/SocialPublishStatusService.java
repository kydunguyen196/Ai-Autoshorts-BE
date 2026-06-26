package com.autoshorts.ai.service;

import com.autoshorts.ai.client.InstagramApiClient;
import com.autoshorts.ai.client.YoutubeApiClient;
import com.autoshorts.ai.client.model.SocialPublishInit;
import com.autoshorts.ai.client.model.SocialPublishStatus;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.PublishStatus;
import com.autoshorts.ai.entity.SocialPlatform;
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
import java.util.UUID;

/**
 * Reconciles async YouTube / Instagram posts the way {@link TikTokPublishStatusService} does for
 * TikTok: poll the provider for a submitted post and drive the job to PUBLISHED / PUBLISH_FAILED.
 * For Instagram there is an extra step — once the container finishes processing we must call
 * {@code media_publish} before the post is live.
 */
@Service
public class SocialPublishStatusService {

    private static final Logger log = LoggerFactory.getLogger(SocialPublishStatusService.class);

    private final VideoJobRepository videoJobRepository;
    private final YoutubeApiClient youtubeApiClient;
    private final InstagramApiClient instagramApiClient;
    private final SocialConnectionService socialConnectionService;
    private final VideoPublishService videoPublishService;
    private final AppProperties appProperties;

    public SocialPublishStatusService(
        VideoJobRepository videoJobRepository,
        YoutubeApiClient youtubeApiClient,
        InstagramApiClient instagramApiClient,
        SocialConnectionService socialConnectionService,
        VideoPublishService videoPublishService,
        AppProperties appProperties
    ) {
        this.videoJobRepository = videoJobRepository;
        this.youtubeApiClient = youtubeApiClient;
        this.instagramApiClient = instagramApiClient;
        this.socialConnectionService = socialConnectionService;
        this.videoPublishService = videoPublishService;
        this.appProperties = appProperties;
    }

    /** @return number of jobs that reached a terminal state this cycle, across both platforms. */
    public int reconcilePendingPublishes() {
        int finalized = 0;
        if (appProperties.getYoutube().isDirectPublishEnabled()) {
            finalized += reconcilePlatform(SocialPlatform.YOUTUBE, appProperties.getYoutube().getStatusPollBatchSize());
        }
        if (appProperties.getInstagram().isDirectPublishEnabled()) {
            finalized += reconcilePlatform(SocialPlatform.INSTAGRAM, appProperties.getInstagram().getStatusPollBatchSize());
        }
        return finalized;
    }

    private int reconcilePlatform(SocialPlatform platform, int batchSize) {
        List<VideoJob> pending = videoJobRepository.findPendingAsyncPublishes(
            PublishStatus.PUBLISHING, platform.key(), PageRequest.of(0, batchSize));

        int finalized = 0;
        for (VideoJob job : pending) {
            try {
                if (reconcileOne(platform, job)) {
                    finalized++;
                }
            } catch (Exception ex) {
                log.warn("event=social_status_poll_error platform={} jobId={} message={}",
                    platform.key(), job.getId(), ex.getMessage());
                safeTouch(job.getId());
            }
        }
        return finalized;
    }

    private boolean reconcileOne(SocialPlatform platform, VideoJob job) {
        String accessToken = socialConnectionService
            .findAccessToken(job.getUserId(), job.getChannelId(), platform)
            .orElse(null);
        if (!StringUtils.hasText(accessToken)) {
            videoPublishService.failAsyncPublish(job.getId(),
                platform.key() + " connection is no longer active", null);
            return true;
        }

        SocialPublishStatus status = platform == SocialPlatform.YOUTUBE
            ? youtubeApiClient.fetchUploadStatus(accessToken, job.getPublishExternalId())
            : instagramApiClient.fetchContainerStatus(accessToken, job.getPublishExternalId());

        if (status.isTerminalSuccess()) {
            if (platform == SocialPlatform.INSTAGRAM) {
                // Container is FINISHED — publish it to make the Reel live.
                SocialPublishInit published = instagramApiClient.publishContainer(
                    accessToken, job.getPublishTargetAccountId(), job.getPublishExternalId());
                videoPublishService.completeAsyncPublish(job.getId(), published.publishId(), published.rawResponseJson());
            } else {
                videoPublishService.completeAsyncPublish(job.getId(), status.publicPostId(), status.rawResponseJson());
            }
            return true;
        }
        if (status.isTerminalFailure()) {
            String reason = StringUtils.hasText(status.failReason())
                ? platform.key() + " publish failed: " + status.failReason()
                : platform.key() + " publish failed";
            videoPublishService.failAsyncPublish(job.getId(), reason, status.rawResponseJson());
            return true;
        }
        if (hasTimedOut(platform, job)) {
            videoPublishService.failAsyncPublish(job.getId(),
                platform.key() + " publish did not complete within the timeout window (last status: " + status.status() + ")",
                status.rawResponseJson());
            log.warn("event=social_status_poll_timeout platform={} jobId={} lastStatus={}",
                platform.key(), job.getId(), status.status());
            return true;
        }
        videoPublishService.touchPublishStatusCheck(job.getId());
        return false;
    }

    private boolean hasTimedOut(SocialPlatform platform, VideoJob job) {
        Instant startedAt = job.getPublishStartedAt();
        if (startedAt == null) {
            return false;
        }
        long minutes = platform == SocialPlatform.YOUTUBE
            ? appProperties.getYoutube().getStatusTimeoutMinutes()
            : appProperties.getInstagram().getStatusTimeoutMinutes();
        return startedAt.plus(Duration.ofMinutes(minutes)).isBefore(Instant.now());
    }

    private void safeTouch(UUID jobId) {
        try {
            videoPublishService.touchPublishStatusCheck(jobId);
        } catch (Exception ex) {
            log.debug("event=social_status_touch_failed jobId={} message={}", jobId, ex.getMessage());
        }
    }
}
