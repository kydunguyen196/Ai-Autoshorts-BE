package com.autoshorts.ai.integration;

import com.autoshorts.ai.client.OpenAiClient;
import com.autoshorts.ai.client.model.OpenAiGenerationResult;
import com.autoshorts.ai.entity.ContentGenerationMode;
import com.autoshorts.ai.entity.GenerationStep;
import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.integration.support.IntegrationTestBase;
import com.autoshorts.ai.integration.support.IntegrationTestConfiguration;
import com.autoshorts.ai.service.ContentGenerationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
class ContentGenerationLayerIT extends IntegrationTestBase {

    @SpyBean
    private OpenAiClient openAiClient;

    @SpyBean
    private ContentGenerationService contentGenerationService;

    @AfterEach
    void resetSpies() {
        Mockito.reset(openAiClient, contentGenerationService);
    }

    @Test
    void shouldPersistSuccessfulContentGenerationMetadata() throws Exception {
        AuthSession session = registerUser("content-success");
        UUID jobId = submitGenerate(session, "Content metadata persistence", "facts", 26, null);

        VideoJob completed = awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
        assertThat(completed.getResolvedStyle()).isNotBlank();
        assertThat(completed.getPromptTemplateId()).isNotNull();
        assertThat(completed.getHookText()).isNotBlank();
        assertThat(completed.getScriptText()).isNotBlank();
        assertThat(completed.getCtaText()).isNotBlank();
        assertThat(completed.getCaptionText()).isNotBlank();
        assertThat(completed.getHashtags()).isNotBlank();
        assertThat(completed.getSceneBreakdownJson()).isNotBlank();
        assertThat(completed.getContentVariantKey()).isNotBlank();
        assertThat(completed.getHookStrategy()).isNotBlank();
        assertThat(completed.getCtaStrategy()).isNotBlank();
        assertThat(completed.getStructureStrategy()).isNotBlank();
        assertThat(completed.getHookStrengthScore()).isBetween(0, 100);
        assertThat(completed.getEngagementScore()).isBetween(0, 100);
        assertThat(completed.getEngagementTagsJson()).isNotBlank();
        assertThat(completed.getContentGenerationMode()).isIn(ContentGenerationMode.MOCK, ContentGenerationMode.FALLBACK);
    }

    @Test
    void shouldUseMockModeForNormalGenerationInMockProfile() throws Exception {
        AuthSession session = registerUser("content-mock-mode");
        UUID jobId = submitGenerate(session, "Mock mode generation behavior", "motivation", 28, null);

        VideoJob completed = awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
        assertThat(completed.getContentGenerationMode()).isEqualTo(ContentGenerationMode.MOCK);
    }

    @Test
    void shouldFallbackWhenStructuredContentGenerationFails() throws Exception {
        AuthSession session = registerUser("content-fallback");
        doReturn(new OpenAiGenerationResult(
            "NOT_JSON_RESPONSE",
            ContentGenerationMode.MOCK,
            "mock_openai",
            "mock-v1"
        )).when(openAiClient).generateFromPrompts(anyString(), anyString());

        UUID jobId = submitGenerate(session, "Fallback trigger topic", "storytelling", 24, null);
        VideoJob completed = awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));

        assertThat(completed.getContentGenerationMode()).isEqualTo(ContentGenerationMode.FALLBACK);
        assertThat(completed.getHookText()).isNotBlank();
        assertThat(completed.getScriptText()).isNotBlank();
        assertThat(completed.getCtaText()).isNotBlank();
        assertThat(completed.getCaptionText()).isNotBlank();
        assertThat(completed.getHashtags()).isNotBlank();
        assertThat(completed.getSceneBreakdownJson()).isNotBlank();
    }

    @Test
    void shouldRetryAfterFailedContentGenerationAndRepopulateMetadata() throws Exception {
        AuthSession session = registerUser("content-retry-failure");

        doThrow(new RuntimeException("forced content preparation failure"))
            .doCallRealMethod()
            .when(contentGenerationService)
            .generateForJob(any(VideoJob.class));

        UUID jobId = submitGenerate(session, "Retry after content failure", "facts", 25, null);
        VideoJob failed = awaitJobStatus(jobId, JobStatus.FAILED, Duration.ofSeconds(30));
        assertThat(failed.getCurrentStep()).isEqualTo(GenerationStep.CONTENT_PREPARATION);
        assertThat(failed.getStepErrorDetails()).contains("step=CONTENT_PREPARATION");

        JsonNode retryResponse = postJson("/api/videos/" + jobId + "/retry", Map.of(), session.token(), 202);
        assertThat(retryResponse.path("status").asText()).isEqualTo("PENDING");
        assertThat(retryResponse.hasNonNull("scriptText")).isFalse();

        VideoJob completed = awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
        assertThat(completed.getScriptText()).isNotBlank();
        assertThat(completed.getHookText()).isNotBlank();
        assertThat(completed.getContentGenerationMode()).isIn(ContentGenerationMode.MOCK, ContentGenerationMode.FALLBACK);
    }

    @Test
    void shouldProduceContentVariationAcrossRequestsForSameStyle() throws Exception {
        AuthSession session = registerUser("content-variation");

        JsonNode response = postJson(
            "/api/videos/generate",
            Map.of(
                "topic", "Variation for same style",
                "style", "motivation",
                "durationSeconds", 30,
                "variantCount", 5,
                "channelId", session.defaultChannelId().toString()
            ),
            session.token(),
            202
        );

        UUID groupId = UUID.fromString(response.path("generationGroupId").asText());

        Awaitility.await()
            .atMost(45, TimeUnit.SECONDS)
            .pollInterval(250, TimeUnit.MILLISECONDS)
            .until(() -> videoJobRepository.findAllByUserIdAndGenerationGroupId(session.userId(), groupId).stream()
                .filter(job -> job.getStatus() == JobStatus.COMPLETED)
                .count() == 5);

        List<VideoJob> jobs = videoJobRepository.findAllByUserIdAndGenerationGroupId(session.userId(), groupId);
        assertThat(jobs).hasSize(5);
        Set<String> uniqueVariantKeys = jobs.stream()
            .map(VideoJob::getContentVariantKey)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Set<String> uniqueHooks = jobs.stream()
            .map(VideoJob::getHookText)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        assertThat(uniqueVariantKeys.size() > 1 || uniqueHooks.size() > 1).isTrue();
        assertThat(jobs).allMatch(job -> job.getHookStrengthScore() != null && job.getEngagementScore() != null);
    }
}
