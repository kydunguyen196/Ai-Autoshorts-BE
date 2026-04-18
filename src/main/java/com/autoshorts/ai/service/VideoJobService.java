package com.autoshorts.ai.service;

import com.autoshorts.ai.cache.VideoJobStateCache;
import com.autoshorts.ai.dto.GenerationGroupReviewSummaryResponse;
import com.autoshorts.ai.dto.GenerateVideoRequest;
import com.autoshorts.ai.dto.PagedResponse;
import com.autoshorts.ai.dto.VideoJobResponse;
import com.autoshorts.ai.entity.CharacterCampaign;
import com.autoshorts.ai.entity.CharacterProfile;
import com.autoshorts.ai.entity.GenerationStep;
import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.PublishStatus;
import com.autoshorts.ai.entity.ReviewStatus;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.exception.BadRequestException;
import com.autoshorts.ai.exception.InvalidJobStateException;
import com.autoshorts.ai.exception.ResourceNotFoundException;
import com.autoshorts.ai.repository.CharacterCampaignRepository;
import com.autoshorts.ai.repository.CharacterProfileRepository;
import com.autoshorts.ai.repository.VideoJobRepository;
import com.autoshorts.ai.util.VideoJobMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class VideoJobService {

    private static final Logger log = LoggerFactory.getLogger(VideoJobService.class);
    private static final int MIN_VARIANT_COUNT = 1;
    private static final int MAX_VARIANT_COUNT = 10;

    private final VideoJobRepository videoJobRepository;
    private final VideoJobStateCache videoJobStateCache;
    private final ChannelService channelService;
    private final CharacterProfileRepository characterProfileRepository;
    private final CharacterCampaignRepository characterCampaignRepository;

    public VideoJobService(
        VideoJobRepository videoJobRepository,
        VideoJobStateCache videoJobStateCache,
        ChannelService channelService,
        CharacterProfileRepository characterProfileRepository,
        CharacterCampaignRepository characterCampaignRepository
    ) {
        this.videoJobRepository = videoJobRepository;
        this.videoJobStateCache = videoJobStateCache;
        this.channelService = channelService;
        this.characterProfileRepository = characterProfileRepository;
        this.characterCampaignRepository = characterCampaignRepository;
    }

    @Transactional
    public VideoJobResponse createPendingJob(GenerateVideoRequest request, UUID userId) {
        UUID channelId = channelService.resolveOwnedChannelIdOrDefault(userId, request.getChannelId());
        return createPendingJob(request, userId, channelId);
    }

    @Transactional
    public VideoJobResponse createPendingJob(GenerateVideoRequest request, UUID userId, UUID channelId) {
        List<VideoJobResponse> created = createPendingJobVariants(
            request,
            userId,
            channelId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            MIN_VARIANT_COUNT
        );
        return created.get(0);
    }

    @Transactional
    public List<VideoJobResponse> createPendingJobVariants(GenerateVideoRequest request, UUID userId) {
        UUID channelId = channelService.resolveOwnedChannelIdOrDefault(userId, request.getChannelId());
        return createPendingJobVariants(
            request,
            userId,
            channelId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            request.getVariantCount()
        );
    }

    @Transactional
    public List<VideoJobResponse> createPendingJobVariants(
        GenerateVideoRequest request,
        UUID userId,
        UUID channelId,
        UUID generationBatchId,
        UUID generationGroupId,
        Integer variantCountOverride
    ) {
        int variantCount = normalizeVariantCount(variantCountOverride);
        UUID batchId = generationBatchId != null ? generationBatchId : UUID.randomUUID();
        UUID groupId = generationGroupId != null ? generationGroupId : UUID.randomUUID();
        CharacterGenerationBinding characterBinding = resolveCharacterBinding(
            userId,
            channelId,
            request.getCharacterProfileId(),
            request.getCharacterCampaignId()
        );

        List<VideoJobResponse> created = new ArrayList<>();
        for (int index = 1; index <= variantCount; index++) {
            VideoJob job = new VideoJob();
            job.setId(UUID.randomUUID());
            job.setUserId(userId);
            job.setChannelId(channelId);
            job.setTopic(request.getTopic().trim());
            job.setStyle(resolveRequestedStyle(request));
            job.setVoiceId(trimToNull(request.getVoiceId()));
            job.setDurationSeconds(request.getDurationSeconds());
            job.setStatus(JobStatus.PENDING);
            job.setCurrentStep(GenerationStep.QUEUED);
            job.setAttemptCount(0);

            job.setGenerationBatchId(batchId);
            job.setGenerationGroupId(groupId);
            job.setCharacterProfileId(characterBinding.characterProfileId());
            job.setCharacterCampaignId(characterBinding.characterCampaignId());
            job.setStoryAngle(trimToNull(request.getStoryAngle()));
            job.setProductPlacementMode(trimToNull(request.getProductPlacementMode()));
            job.setAdDisclosureMode(trimToNull(request.getAdDisclosureMode()));
            job.setSceneCountTarget(request.getSceneCountTarget());
            job.setCharacterConsistencyMode(trimToNull(request.getCharacterConsistencyMode()));
            job.setVariantIndex(index);
            job.setVariantCount(variantCount);
            job.setRankingScore(null);
            job.setTopCandidate(false);
            job.setTopCandidateRank(null);
            job.setReviewStatus(ReviewStatus.DRAFT);
            job.setReviewedAt(null);
            job.setReviewedBy(null);
            job.setRejectionReason(null);
            job.setSelectedForPublish(false);
            job.setPublishStatus(PublishStatus.NOT_PUBLISHED);

            VideoJob saved = videoJobRepository.save(job);
            VideoJobResponse response = VideoJobMapper.toResponse(saved);
            videoJobStateCache.put(response);
            created.add(response);

            log.info(
                "event=job_created jobId={} userId={} channelId={} topic={} durationSeconds={} generationBatchId={} generationGroupId={} variantIndex={} variantCount={}",
                saved.getId(),
                saved.getUserId(),
                saved.getChannelId(),
                saved.getTopic(),
                saved.getDurationSeconds(),
                saved.getGenerationBatchId(),
                saved.getGenerationGroupId(),
                saved.getVariantIndex(),
                saved.getVariantCount()
            );
        }
        return created;
    }

    @Transactional(readOnly = true)
    public VideoJob getEntityOrThrow(UUID jobId) {
        return videoJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
    }

    @Transactional(readOnly = true)
    public VideoJob getEntityForUserOrThrow(UUID jobId, UUID userId) {
        return videoJobRepository.findByIdAndUserId(jobId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
    }

    @Transactional(readOnly = true)
    public List<VideoJobResponse> listRecentJobs(UUID userId, int limit, JobStatus status) {
        return listJobsPage(userId, 0, limit, status).getItems();
    }

    @Transactional(readOnly = true)
    public PagedResponse<VideoJobResponse> listJobsPage(UUID userId, int page, int limit, JobStatus status) {
        PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<VideoJob> jobs = status == null
            ? videoJobRepository.findAllByUserId(userId, pageRequest)
            : videoJobRepository.findAllByUserIdAndStatus(userId, status, pageRequest);
        return PagedResponse.from(jobs.map(VideoJobMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<VideoJobResponse> listJobsByGroup(UUID userId, UUID generationGroupId, int page, int limit, JobStatus status) {
        PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<VideoJob> jobs = status == null
            ? videoJobRepository.findAllByUserIdAndGenerationGroupId(userId, generationGroupId, pageRequest)
            : videoJobRepository.findAllByUserIdAndGenerationGroupIdAndStatus(userId, generationGroupId, status, pageRequest);
        return PagedResponse.from(jobs.map(VideoJobMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<VideoJobResponse> listJobsByBatch(UUID userId, UUID generationBatchId, int page, int limit, JobStatus status) {
        PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<VideoJob> jobs = status == null
            ? videoJobRepository.findAllByUserIdAndGenerationBatchId(userId, generationBatchId, pageRequest)
            : videoJobRepository.findAllByUserIdAndGenerationBatchIdAndStatus(userId, generationBatchId, status, pageRequest);
        return PagedResponse.from(jobs.map(VideoJobMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<VideoJobResponse> listTopCandidatesByGroup(UUID userId, UUID generationGroupId, int page, int limit) {
        PageRequest pageRequest = PageRequest.of(
            page,
            limit,
            Sort.by(
                Sort.Order.asc("topCandidateRank"),
                Sort.Order.desc("rankingScore"),
                Sort.Order.desc("createdAt")
            )
        );
        Page<VideoJob> jobs = videoJobRepository.findAllByUserIdAndGenerationGroupIdAndTopCandidateTrue(
            userId,
            generationGroupId,
            pageRequest
        );
        return PagedResponse.from(jobs.map(VideoJobMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public VideoJobResponse getJobResponse(UUID jobId, UUID userId) {
        VideoJob job = getEntityForUserOrThrow(jobId, userId);
        VideoJobResponse cached = videoJobStateCache.get(jobId).orElse(null);
        if (cached != null && job.getUpdatedAt() != null && job.getUpdatedAt().equals(cached.getUpdatedAt())) {
            return cached;
        }
        VideoJobResponse response = VideoJobMapper.toResponse(job);
        videoJobStateCache.put(response);
        return response;
    }

    @Transactional(readOnly = true)
    public boolean hasActiveJobFor(String topic, String style, UUID userId, UUID channelId) {
        if (!StringUtils.hasText(topic) || !StringUtils.hasText(style)) {
            return false;
        }
        Collection<JobStatus> activeStatuses = List.of(JobStatus.PENDING, JobStatus.PROCESSING);
        return videoJobRepository.existsByTopicIgnoreCaseAndStyleIgnoreCaseAndUserIdAndChannelIdAndStatusIn(
            topic.trim(),
            normalizeStyle(style),
            userId,
            channelId,
            activeStatuses
        );
    }

    @Transactional
    public VideoJob updateJob(UUID jobId, Consumer<VideoJob> mutator) {
        VideoJob job = getEntityOrThrow(jobId);
        mutator.accept(job);
        return saveAndCache(job);
    }

    @Transactional
    public void recomputeRankingForGroup(UUID userId, UUID generationGroupId) {
        if (userId == null || generationGroupId == null) {
            return;
        }

        List<VideoJob> groupJobs = videoJobRepository.findAllByUserIdAndGenerationGroupId(userId, generationGroupId);
        if (groupJobs.isEmpty()) {
            return;
        }

        for (VideoJob job : groupJobs) {
            job.setRankingScore(computeRankingScore(job));
            job.setTopCandidate(false);
            job.setTopCandidateRank(null);
        }

        List<VideoJob> rankedJobs = groupJobs.stream()
            .filter(job -> job.getRankingScore() != null)
            .sorted(
                Comparator.comparing(VideoJob::getRankingScore, Comparator.reverseOrder())
                    .thenComparing(VideoJob::getEngagementScore, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(VideoJob::getHookStrengthScore, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(VideoJob::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(VideoJob::getId)
            )
            .toList();

        int topCandidateCount = resolveTopCandidateCount(groupJobs, rankedJobs.size());
        for (int i = 0; i < topCandidateCount; i++) {
            VideoJob candidate = rankedJobs.get(i);
            candidate.setTopCandidate(true);
            candidate.setTopCandidateRank(i + 1);
        }

        List<VideoJob> saved = videoJobRepository.saveAll(groupJobs);
        saved.stream().map(VideoJobMapper::toResponse).forEach(videoJobStateCache::put);

        String selected = rankedJobs.stream()
            .limit(topCandidateCount)
            .map(job -> job.getId() + ":" + job.getRankingScore())
            .collect(Collectors.joining(","));

        log.info(
            "event=ranking_decision generationGroupId={} totalJobs={} rankedJobs={} topCandidateCount={} selected={} formula=0.70*engagement+0.30*hook",
            generationGroupId,
            groupJobs.size(),
            rankedJobs.size(),
            topCandidateCount,
            selected
        );
    }

    @Transactional
    public VideoJob approveJobForUser(UUID jobId, UUID userId) {
        VideoJob job = getEntityForUserUpdateOrThrow(jobId, userId);
        ensureReviewableState(job, "approve");
        job.setReviewStatus(ReviewStatus.APPROVED);
        job.setReviewedAt(Instant.now());
        job.setReviewedBy(userId);
        job.setRejectionReason(null);
        VideoJob saved = saveAndCache(job);
        log.info("event=job_review_approved jobId={} userId={}", saved.getId(), userId);
        return saved;
    }

    @Transactional
    public VideoJob rejectJobForUser(UUID jobId, UUID userId, String rejectionReason) {
        VideoJob job = getEntityForUserUpdateOrThrow(jobId, userId);
        ensureReviewableState(job, "reject");
        if (!StringUtils.hasText(rejectionReason)) {
            throw new BadRequestException("rejectionReason is required");
        }
        job.setReviewStatus(ReviewStatus.REJECTED);
        job.setReviewedAt(Instant.now());
        job.setReviewedBy(userId);
        job.setRejectionReason(rejectionReason.trim());
        job.setSelectedForPublish(false);
        VideoJob saved = saveAndCache(job);
        log.info("event=job_review_rejected jobId={} userId={}", saved.getId(), userId);
        return saved;
    }

    @Transactional
    public VideoJob selectForPublishForUser(UUID jobId, UUID userId) {
        VideoJob job = getEntityForUserUpdateOrThrow(jobId, userId);
        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new InvalidJobStateException("Only COMPLETED jobs can be selected for publish");
        }
        if (job.getReviewStatus() != ReviewStatus.APPROVED) {
            throw new InvalidJobStateException("Only APPROVED jobs can be selected for publish");
        }

        if (job.getGenerationGroupId() != null) {
            List<VideoJob> groupSelections = videoJobRepository.findAllByUserIdAndGenerationGroupIdAndSelectedForPublishTrue(
                userId,
                job.getGenerationGroupId()
            );
            for (VideoJob selected : groupSelections) {
                if (!selected.getId().equals(job.getId())) {
                    selected.setSelectedForPublish(false);
                    saveAndCache(selected);
                }
            }
        }

        job.setSelectedForPublish(true);
        VideoJob saved = saveAndCache(job);
        log.info(
            "event=job_selected_for_publish jobId={} userId={} generationGroupId={}",
            saved.getId(),
            userId,
            saved.getGenerationGroupId()
        );
        return saved;
    }

    @Transactional(readOnly = true)
    public GenerationGroupReviewSummaryResponse getGroupReviewSummary(UUID userId, UUID generationGroupId) {
        List<VideoJob> jobs = videoJobRepository.findAllByUserIdAndGenerationGroupId(userId, generationGroupId);
        if (jobs.isEmpty()) {
            throw new ResourceNotFoundException("Generation group not found: " + generationGroupId);
        }

        GenerationGroupReviewSummaryResponse response = new GenerationGroupReviewSummaryResponse();
        response.setGenerationGroupId(generationGroupId);
        response.setTotalJobs(jobs.size());
        for (ReviewStatus status : ReviewStatus.values()) {
            response.getReviewStatusCounts().put(status, 0);
        }

        for (VideoJob job : jobs) {
            ReviewStatus status = job.getReviewStatus() == null ? ReviewStatus.DRAFT : job.getReviewStatus();
            response.getReviewStatusCounts().compute(status, (key, value) -> value == null ? 1 : value + 1);
            if (Boolean.TRUE.equals(job.getSelectedForPublish())) {
                response.setSelectedJobId(job.getId());
            }
        }
        return response;
    }

    @Transactional
    public VideoJob startProcessing(UUID jobId) {
        VideoJob job = getEntityForUpdateOrThrow(jobId);
        if (job.getStatus() != JobStatus.PENDING) {
            throw new InvalidJobStateException("Job cannot start processing from state: " + job.getStatus());
        }
        job.setStatus(JobStatus.PROCESSING);
        job.setCurrentStep(GenerationStep.QUEUED);
        job.setErrorMessage(null);
        job.setStepErrorDetails(null);
        job.setLastErrorAt(null);
        job.setStartedAt(Instant.now());
        job.setCompletedAt(null);
        job.setAttemptCount((job.getAttemptCount() == null ? 0 : job.getAttemptCount()) + 1);
        VideoJob saved = saveAndCache(job);
        log.info("event=job_started jobId={} attempt={}", jobId, saved.getAttemptCount());
        return saved;
    }

    @Transactional
    public VideoJob resetFailedForRetry(UUID jobId, UUID userId) {
        VideoJob job = getEntityForUserUpdateOrThrow(jobId, userId);
        if (job.getStatus() != JobStatus.FAILED) {
            throw new InvalidJobStateException("Only FAILED jobs can be retried. Current state: " + job.getStatus());
        }

        job.setStatus(JobStatus.PENDING);
        job.setCurrentStep(GenerationStep.QUEUED);
        job.setErrorMessage(null);
        job.setStepErrorDetails(null);
        job.setScriptText(null);
        job.setAudioUrl(null);
        job.setAudioGenerationMode(null);
        job.setAudioProvider(null);
        job.setAudioVoiceId(null);
        job.setAudioModelId(null);
        job.setAudioOutputFormat(null);
        job.setAudioProviderRequestDurationMs(null);
        job.setAudioFailureReason(null);
        job.setAudioFailureDetails(null);
        job.setSubtitleUrl(null);
        job.setFinalVideoUrl(null);
        job.setHookText(null);
        job.setCtaText(null);
        job.setCaptionText(null);
        job.setHashtags(null);
        job.setSceneBreakdownJson(null);
        job.setResolvedStyle(null);
        job.setPromptTemplateId(null);
        job.setContentGenerationMode(null);
        job.setContentVariantKey(null);
        job.setHookStrategy(null);
        job.setCtaStrategy(null);
        job.setStructureStrategy(null);
        job.setHookStrengthScore(null);
        job.setEngagementScore(null);
        job.setEngagementTagsJson(null);
        job.setRankingScore(null);
        job.setTopCandidate(false);
        job.setTopCandidateRank(null);
        job.setReviewStatus(ReviewStatus.DRAFT);
        job.setReviewedAt(null);
        job.setReviewedBy(null);
        job.setRejectionReason(null);
        job.setSelectedForPublish(false);
        job.setPublishStatus(PublishStatus.NOT_PUBLISHED);
        job.setScheduledPublishAt(null);
        job.setPublishPlatform(null);
        job.setPublishReadyAt(null);
        job.setPublishRequestedAt(null);
        job.setPublishStartedAt(null);
        job.setPublishedAt(null);
        job.setPublishAttemptCount(0);
        job.setPublishProvider(null);
        job.setPublishExternalId(null);
        job.setPublishTargetAccountId(null);
        job.setPublishRequestPayloadJson(null);
        job.setPublishResponsePayloadJson(null);
        job.setPublishFailureReason(null);
        job.setPublishFailureDetails(null);
        job.setPublishLastErrorAt(null);
        job.setPublishLastStatusCheckAt(null);
        job.setStartedAt(null);
        job.setCompletedAt(null);
        job.setLastErrorAt(null);
        VideoJob saved = saveAndCache(job);
        log.info(
            "event=job_retry_reset jobId={} clearedFields=script,audioUrl,audioGenerationMode,audioProvider,subtitleUrl,finalVideoUrl,hookText,ctaText,captionText,hashtags,sceneBreakdownJson,resolvedStyle,promptTemplateId,contentGenerationMode,contentVariantKey,hookStrategy,ctaStrategy,structureStrategy,hookStrengthScore,engagementScore,engagementTagsJson,rankingScore,topCandidate,topCandidateRank,publishStatus,scheduledPublishAt,publishPlatform,publishReadyAt,publishRequestedAt,publishStartedAt,publishedAt,publishAttemptCount,publishProvider,publishExternalId,publishFailureReason,publishFailureDetails,publishLastErrorAt,startedAt,completedAt,lastErrorAt retainedInputs=topic,style,voiceId,durationSeconds,attemptCount,generationBatchId,generationGroupId,variantIndex,variantCount",
            jobId
        );
        return saved;
    }

    @Transactional
    public VideoJob markCurrentStep(UUID jobId, GenerationStep step) {
        return updateJob(jobId, job -> {
            if (job.getStatus() != JobStatus.PROCESSING) {
                throw new InvalidJobStateException("Cannot update current step while job is in state: " + job.getStatus());
            }
            job.setCurrentStep(step);
            job.setStepErrorDetails(null);
        });
    }

    @Transactional
    public VideoJob markCompleted(UUID jobId, String finalVideoUrl) {
        VideoJob saved = updateJob(jobId, job -> {
            if (job.getStatus() != JobStatus.PROCESSING) {
                throw new InvalidJobStateException("Job cannot be completed from state: " + job.getStatus());
            }
            job.setStatus(JobStatus.COMPLETED);
            job.setCurrentStep(GenerationStep.COMPLETED);
            job.setFinalVideoUrl(finalVideoUrl);
            job.setErrorMessage(null);
            job.setStepErrorDetails(null);
            if (job.getReviewStatus() == null || job.getReviewStatus() == ReviewStatus.DRAFT || job.getReviewStatus() == ReviewStatus.REJECTED) {
                job.setReviewStatus(ReviewStatus.GENERATED);
            }
            job.setReviewedAt(null);
            job.setReviewedBy(null);
            job.setRejectionReason(null);
            if (job.getSelectedForPublish() == null) {
                job.setSelectedForPublish(false);
            }
            if (job.getPublishStatus() == null || job.getPublishStatus() == PublishStatus.NOT_PUBLISHED || job.getPublishStatus() == PublishStatus.PUBLISH_FAILED) {
                job.setPublishStatus(PublishStatus.READY_TO_PUBLISH);
            }
            if (job.getPublishReadyAt() == null) {
                job.setPublishReadyAt(Instant.now());
            }
            job.setPublishFailureReason(null);
            job.setPublishFailureDetails(null);
            job.setPublishLastErrorAt(null);
            job.setCompletedAt(Instant.now());
        });
        log.info("event=job_completed_persisted jobId={} finalVideoUrl={}", jobId, finalVideoUrl);
        return saved;
    }

    @Transactional
    public VideoJob markFailed(UUID jobId, GenerationStep failedStep, String errorMessage, String stepErrorDetails) {
        VideoJob saved = updateJob(jobId, job -> {
            if (job.getStatus() == JobStatus.COMPLETED) {
                return;
            }
            if (job.getStatus() != JobStatus.PROCESSING && job.getStatus() != JobStatus.PENDING) {
                throw new InvalidJobStateException("Job cannot be moved to FAILED from state: " + job.getStatus());
            }
            job.setStatus(JobStatus.FAILED);
            if (failedStep != null) {
                job.setCurrentStep(failedStep);
            }
            job.setErrorMessage(errorMessage);
            job.setStepErrorDetails(stepErrorDetails);
            job.setLastErrorAt(Instant.now());
            job.setCompletedAt(Instant.now());
        });
        log.warn("event=job_failed_persisted jobId={} step={} error={}", jobId, failedStep, errorMessage);
        return saved;
    }

    private VideoJob getEntityForUpdateOrThrow(UUID jobId) {
        return videoJobRepository.findByIdForUpdate(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
    }

    private VideoJob getEntityForUserUpdateOrThrow(UUID jobId, UUID userId) {
        return videoJobRepository.findByIdAndUserIdForUpdate(jobId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
    }

    private VideoJob saveAndCache(VideoJob job) {
        VideoJob saved = videoJobRepository.save(job);
        videoJobStateCache.put(VideoJobMapper.toResponse(saved));
        return saved;
    }

    private Integer computeRankingScore(VideoJob job) {
        if (job.getEngagementScore() == null && job.getHookStrengthScore() == null) {
            return null;
        }
        int engagement = clampScore(job.getEngagementScore());
        int hook = clampScore(job.getHookStrengthScore());
        return (int) Math.round((engagement * 0.70d) + (hook * 0.30d));
    }

    private void ensureReviewableState(VideoJob job, String action) {
        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new InvalidJobStateException("Cannot " + action + " job in state: " + job.getStatus());
        }
        if (job.getReviewStatus() == null || job.getReviewStatus() == ReviewStatus.DRAFT) {
            throw new InvalidJobStateException("Job is not ready for review yet");
        }
    }

    private CharacterGenerationBinding resolveCharacterBinding(
        UUID userId,
        UUID channelId,
        UUID requestedCharacterProfileId,
        UUID requestedCharacterCampaignId
    ) {
        CharacterProfile profile = null;
        CharacterCampaign campaign = null;

        if (requestedCharacterProfileId != null) {
            profile = characterProfileRepository.findByIdAndOwnerUserId(requestedCharacterProfileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Character profile not found: " + requestedCharacterProfileId));
            if (!Objects.equals(profile.getChannelId(), channelId)) {
                throw new BadRequestException("Character profile channel does not match requested channel");
            }
        }

        if (requestedCharacterCampaignId != null) {
            campaign = characterCampaignRepository.findByIdAndOwnerUserId(requestedCharacterCampaignId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Character campaign not found: " + requestedCharacterCampaignId));
            if (!Objects.equals(campaign.getChannelId(), channelId)) {
                throw new BadRequestException("Character campaign channel does not match requested channel");
            }
        }

        UUID resolvedProfileId = profile == null ? null : profile.getId();
        UUID resolvedCampaignId = campaign == null ? null : campaign.getId();

        if (campaign != null && campaign.getCharacterProfileId() != null) {
            if (resolvedProfileId != null && !Objects.equals(campaign.getCharacterProfileId(), resolvedProfileId)) {
                throw new BadRequestException("characterProfileId must match campaign.characterProfileId");
            }
            resolvedProfileId = campaign.getCharacterProfileId();
        }

        return new CharacterGenerationBinding(resolvedProfileId, resolvedCampaignId);
    }

    private int resolveTopCandidateCount(List<VideoJob> groupJobs, int rankedSize) {
        if (rankedSize <= 0) {
            return 0;
        }
        int groupVariantCount = groupJobs.stream()
            .map(VideoJob::getVariantCount)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(rankedSize);

        int target = Math.max(1, Math.min(3, (int) Math.ceil(groupVariantCount * 0.30d)));
        return Math.min(target, rankedSize);
    }

    private int clampScore(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, score));
    }

    private int normalizeVariantCount(Integer variantCount) {
        if (variantCount == null) {
            return MIN_VARIANT_COUNT;
        }
        return Math.max(MIN_VARIANT_COUNT, Math.min(MAX_VARIANT_COUNT, variantCount));
    }

    private String normalizeStyle(String style) {
        if (!StringUtils.hasText(style)) {
            return "motivation";
        }
        return style.trim()
            .toLowerCase()
            .replace('_', '-')
            .replace(' ', '-');
    }

    private String resolveRequestedStyle(GenerateVideoRequest request) {
        if (StringUtils.hasText(request.getContentStyle())) {
            return normalizeStyle(request.getContentStyle());
        }
        return normalizeStyle(request.getStyle());
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record CharacterGenerationBinding(UUID characterProfileId, UUID characterCampaignId) {
    }
}
