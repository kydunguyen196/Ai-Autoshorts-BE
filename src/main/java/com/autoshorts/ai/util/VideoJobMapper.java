package com.autoshorts.ai.util;

import com.autoshorts.ai.dto.VideoJobResponse;
import com.autoshorts.ai.dto.VideoPublishStatusResponse;
import com.autoshorts.ai.entity.VideoJob;

import java.util.Arrays;
import java.util.List;

public final class VideoJobMapper {

    private VideoJobMapper() {
    }

    public static VideoJobResponse toResponse(VideoJob job) {
        VideoJobResponse response = new VideoJobResponse();
        response.setJobId(job.getId());
        response.setChannelId(job.getChannelId());
        response.setStatus(job.getStatus());
        response.setTopic(job.getTopic());
        response.setStyle(job.getStyle());
        response.setVoiceId(job.getVoiceId());
        response.setDurationSeconds(job.getDurationSeconds());
        response.setScriptText(job.getScriptText());
        response.setHookText(job.getHookText());
        response.setCtaText(job.getCtaText());
        response.setCaptionText(job.getCaptionText());
        response.setReviewBeforeRender(Boolean.TRUE.equals(job.getReviewBeforeRender()));
        response.setTemplateId(job.getTemplateId());
        response.setHashtags(parseHashtags(job.getHashtags()));
        response.setSceneBreakdownJson(job.getSceneBreakdownJson());
        response.setSceneAssetsJson(job.getSceneAssetsJson());
        response.setResolvedStyle(job.getResolvedStyle());
        response.setPromptTemplateId(job.getPromptTemplateId());
        response.setContentGenerationMode(job.getContentGenerationMode());
        response.setContentVariantKey(job.getContentVariantKey());
        response.setHookStrategy(job.getHookStrategy());
        response.setCtaStrategy(job.getCtaStrategy());
        response.setStructureStrategy(job.getStructureStrategy());
        response.setHookStrengthScore(job.getHookStrengthScore());
        response.setEngagementScore(job.getEngagementScore());
        response.setEngagementTagsJson(job.getEngagementTagsJson());
        response.setGenerationBatchId(job.getGenerationBatchId());
        response.setGenerationGroupId(job.getGenerationGroupId());
        response.setCharacterProfileId(job.getCharacterProfileId());
        response.setCharacterCampaignId(job.getCharacterCampaignId());
        response.setStoryAngle(job.getStoryAngle());
        response.setProductPlacementMode(job.getProductPlacementMode());
        response.setAdDisclosureMode(job.getAdDisclosureMode());
        response.setSceneCountTarget(job.getSceneCountTarget());
        response.setCharacterConsistencyMode(job.getCharacterConsistencyMode());
        response.setNiche(job.getNiche());
        response.setPlatform(job.getPlatform());
        response.setSubtitleStyle(job.getSubtitleStyle());
        response.setVisualMode(job.getVisualMode());
        response.setVoiceProvider(job.getVoiceProvider());
        response.setVoicePersona(job.getVoicePersona());
        response.setQualityPreset(job.getQualityPreset());
        response.setVariantIndex(job.getVariantIndex());
        response.setVariantCount(job.getVariantCount());
        response.setRankingScore(job.getRankingScore());
        response.setIsTopCandidate(job.getTopCandidate());
        response.setTopCandidateRank(job.getTopCandidateRank());
        response.setReviewStatus(job.getReviewStatus());
        response.setReviewedAt(job.getReviewedAt());
        response.setReviewedBy(job.getReviewedBy());
        response.setRejectionReason(job.getRejectionReason());
        response.setSelectedForPublish(job.getSelectedForPublish());
        response.setPublishStatus(job.getPublishStatus());
        response.setScheduledPublishAt(job.getScheduledPublishAt());
        response.setPublishPlatform(job.getPublishPlatform());
        response.setPublishReadyAt(job.getPublishReadyAt());
        response.setPublishRequestedAt(job.getPublishRequestedAt());
        response.setPublishStartedAt(job.getPublishStartedAt());
        response.setPublishedAt(job.getPublishedAt());
        response.setPublishAttemptCount(job.getPublishAttemptCount());
        response.setPublishProvider(job.getPublishProvider());
        response.setPublishExternalId(job.getPublishExternalId());
        response.setPublishTargetAccountId(job.getPublishTargetAccountId());
        response.setPublishRequestPayloadJson(job.getPublishRequestPayloadJson());
        response.setPublishResponsePayloadJson(job.getPublishResponsePayloadJson());
        response.setPublishFailureReason(job.getPublishFailureReason());
        response.setPublishFailureDetails(job.getPublishFailureDetails());
        response.setPublishLastErrorAt(job.getPublishLastErrorAt());
        response.setPublishLastStatusCheckAt(job.getPublishLastStatusCheckAt());
        response.setExportStatus(job.getExportStatus());
        response.setDownloadUrl(job.getDownloadUrl());
        response.setEstimatedCostCredits(job.getEstimatedCostCredits());
        response.setAudioUrl(job.getAudioUrl());
        response.setAudioGenerationMode(job.getAudioGenerationMode());
        response.setAudioProvider(job.getAudioProvider());
        response.setAudioVoiceId(job.getAudioVoiceId());
        response.setAudioModelId(job.getAudioModelId());
        response.setAudioOutputFormat(job.getAudioOutputFormat());
        response.setAudioProviderRequestDurationMs(job.getAudioProviderRequestDurationMs());
        response.setAudioFailureReason(job.getAudioFailureReason());
        response.setAudioFailureDetails(job.getAudioFailureDetails());
        response.setVisualGenerationMode(job.getVisualGenerationMode());
        response.setVisualProvider(job.getVisualProvider());
        response.setVisualModelId(job.getVisualModelId());
        response.setVisualFailureReason(job.getVisualFailureReason());
        response.setVisualFailureDetails(job.getVisualFailureDetails());
        response.setProviderModes(buildProviderModes(job));
        response.setSubtitleUrl(job.getSubtitleUrl());
        response.setFinalVideoUrl(job.getFinalVideoUrl());
        response.setErrorMessage(job.getErrorMessage());
        response.setCurrentStep(job.getCurrentStep());
        response.setStepErrorDetails(job.getStepErrorDetails());
        response.setAttemptCount(job.getAttemptCount());
        response.setStartedAt(job.getStartedAt());
        response.setCompletedAt(job.getCompletedAt());
        response.setLastErrorAt(job.getLastErrorAt());
        response.setCreatedAt(job.getCreatedAt());
        response.setUpdatedAt(job.getUpdatedAt());
        return response;
    }

    public static VideoPublishStatusResponse toPublishStatusResponse(VideoJob job) {
        VideoPublishStatusResponse response = new VideoPublishStatusResponse();
        response.setJobId(job.getId());
        response.setPublishStatus(job.getPublishStatus());
        response.setPublishPlatform(job.getPublishPlatform());
        response.setPublishProvider(job.getPublishProvider());
        response.setPublishExternalId(job.getPublishExternalId());
        response.setPublishTargetAccountId(job.getPublishTargetAccountId());
        response.setPublishRequestPayloadJson(job.getPublishRequestPayloadJson());
        response.setPublishResponsePayloadJson(job.getPublishResponsePayloadJson());
        response.setPublishReadyAt(job.getPublishReadyAt());
        response.setPublishRequestedAt(job.getPublishRequestedAt());
        response.setPublishStartedAt(job.getPublishStartedAt());
        response.setPublishedAt(job.getPublishedAt());
        response.setPublishAttemptCount(job.getPublishAttemptCount());
        response.setPublishFailureReason(job.getPublishFailureReason());
        response.setPublishFailureDetails(job.getPublishFailureDetails());
        response.setPublishLastErrorAt(job.getPublishLastErrorAt());
        response.setPublishLastStatusCheckAt(job.getPublishLastStatusCheckAt());
        response.setReviewStatus(job.getReviewStatus());
        response.setSelectedForPublish(job.getSelectedForPublish());
        return response;
    }

    private static List<String> parseHashtags(String hashtags) {
        if (hashtags == null || hashtags.isBlank()) {
            return null;
        }
        return Arrays.stream(hashtags.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
    }

    private static String buildProviderModes(VideoJob job) {
        return "content=%s,audio=%s,visual=%s,publish=%s".formatted(
            job.getContentGenerationMode() == null ? "UNKNOWN" : job.getContentGenerationMode(),
            job.getAudioGenerationMode() == null ? "UNKNOWN" : job.getAudioGenerationMode(),
            job.getVisualGenerationMode() == null ? "UNKNOWN" : job.getVisualGenerationMode(),
            job.getPublishProvider() == null ? "none" : job.getPublishProvider()
        );
    }
}
