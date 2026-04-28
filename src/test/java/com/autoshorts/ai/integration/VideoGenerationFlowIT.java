package com.autoshorts.ai.integration;

import com.autoshorts.ai.entity.GenerationStep;
import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.integration.support.IntegrationTestBase;
import com.autoshorts.ai.integration.support.IntegrationTestConfiguration;
import com.autoshorts.ai.queue.VideoJobQueuePublisher;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
class VideoGenerationFlowIT extends IntegrationTestBase {

    @SpyBean
    private VideoJobQueuePublisher videoJobQueuePublisher;

    @AfterEach
    void resetSpies() {
        Mockito.reset(videoJobQueuePublisher);
    }

    @Test
    void shouldAcceptGenerateRequest() throws Exception {
        AuthSession session = registerUser("generate-accept");

        JsonNode response = postJson(
            "/api/videos/generate",
            Map.of(
                "topic", "Generate acceptance contract",
                "style", "storytelling",
                "contentStyle", "storytelling",
                "durationSeconds", 28,
                "channelId", session.defaultChannelId().toString()
            ),
            session.token(),
            202
        );

        UUID jobId = UUID.fromString(response.path("jobId").asText());
        assertThat(response.path("status").asText()).isEqualTo("PENDING");
        assertThat(response.path("topic").asText()).isEqualTo("Generate acceptance contract");
        assertThat(response.path("style").asText()).isEqualTo("storytelling");

        verify(videoJobQueuePublisher, atLeastOnce()).publish(eq(jobId), eq("api_generate"));
        awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
    }

    @Test
    void shouldPollJobUntilTerminalStatus() throws Exception {
        AuthSession session = registerUser("generate-terminal");
        UUID jobId = submitGenerate(session, "Terminal status flow", "facts", 30, null);

        VideoJob completed = awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
        assertThat(completed.getCurrentStep()).isEqualTo(GenerationStep.COMPLETED);
        assertThat(completed.getFinalVideoUrl()).isNotBlank();
        assertThat(completed.getAudioUrl()).isNotBlank();
        assertThat(completed.getSubtitleUrl()).isNotBlank();
        assertThat(completed.getSceneAssetsJson()).isNotBlank();
        assertThat(completed.getVisualGenerationMode()).isNotNull();
        assertThat(completed.getVisualProvider()).isNotBlank();
        assertThat(completed.getFinalVideoUrl()).startsWith("http://localhost:8080/api/media/jobs/");
        assertThat(completed.getAudioUrl()).startsWith("http://localhost:8080/api/media/jobs/");
        assertThat(completed.getSubtitleUrl()).startsWith("http://localhost:8080/api/media/jobs/");
        assertThat(completed.getAttemptCount()).isEqualTo(1);
        assertThat(completed.getPublishStatus().name()).isEqualTo("READY_TO_PUBLISH");

        assertThat(getBinary(completed.getAudioUrl(), null, 200)).isNotEmpty();
        assertThat(getBinary(completed.getSubtitleUrl(), null, 200)).isNotEmpty();
        assertThat(getBinary(completed.getFinalVideoUrl(), null, 200)).isNotEmpty();

        JsonNode detail = getJson("/api/videos/" + jobId, session.token(), 200);
        assertThat(detail.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(detail.path("currentStep").asText()).isEqualTo("COMPLETED");
        assertThat(detail.path("hookText").asText()).isNotBlank();
        assertThat(detail.path("scriptText").asText()).isNotBlank();
        assertThat(detail.path("captionText").asText()).isNotBlank();
        assertThat(detail.path("contentGenerationMode").asText()).isIn("MOCK", "FALLBACK");
        assertThat(detail.path("audioGenerationMode").asText()).isIn("MOCK", "FALLBACK", "REAL");
        assertThat(detail.path("visualGenerationMode").asText()).isIn("MOCK", "FALLBACK", "REAL");
        assertThat(detail.path("sceneAssetsJson").asText()).isNotBlank();
    }

    @Test
    void shouldRetryFailedJob() throws Exception {
        AuthSession session = registerUser("generate-retry");
        deterministicFfmpegVideoComposer.failNextComposition("ffmpeg failure on first attempt");

        UUID jobId = submitGenerate(session, "Retry flow through failed composition", "motivation", 27, null);
        VideoJob failed = awaitJobStatus(jobId, JobStatus.FAILED, Duration.ofSeconds(40));
        assertThat(failed.getCurrentStep()).isEqualTo(GenerationStep.VIDEO_COMPOSITION);
        assertThat(failed.getStepErrorDetails()).contains("step=VIDEO_COMPOSITION");
        assertThat(failed.getAttemptCount()).isEqualTo(1);

        JsonNode retryResponse = postJson("/api/videos/" + jobId + "/retry", Map.of(), session.token(), 202);
        assertThat(retryResponse.path("status").asText()).isEqualTo("PENDING");
        assertThat(retryResponse.path("currentStep").asText()).isEqualTo("QUEUED");
        assertThat(retryResponse.hasNonNull("audioUrl")).isFalse();
        assertThat(retryResponse.hasNonNull("subtitleUrl")).isFalse();
        assertThat(retryResponse.hasNonNull("finalVideoUrl")).isFalse();
        assertThat(retryResponse.hasNonNull("scriptText")).isFalse();

        VideoJob completed = awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
        assertThat(completed.getAttemptCount()).isEqualTo(2);
        assertThat(completed.getFinalVideoUrl()).isNotBlank();
        assertThat(completed.getTopic()).isEqualTo("Retry flow through failed composition");
        assertThat(completed.getStyle()).isEqualTo("motivation");
        assertThat(completed.getDurationSeconds()).isEqualTo(27);
    }

    @Test
    void shouldAllowRetryOnlyForFailedJobs() throws Exception {
        AuthSession session = registerUser("generate-retry-guard");

        UUID pendingJobId;
        pauseQueueConsumers();
        try {
            pendingJobId = submitGenerate(session, "Retry guard pending state", "facts", 24, null);
            postJson("/api/videos/" + pendingJobId + "/retry", Map.of(), session.token(), 409);
        } finally {
            resumeQueueConsumers();
        }

        VideoJob completedPending = awaitJobStatus(pendingJobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
        postJson("/api/videos/" + completedPending.getId() + "/retry", Map.of(), session.token(), 409);

        UUID completedJobId = submitGenerate(session, "Retry guard completed state", "storytelling", 24, null);
        VideoJob completed = awaitJobStatus(completedJobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
        postJson("/api/videos/" + completed.getId() + "/retry", Map.of(), session.token(), 409);
    }
}
