package com.autoshorts.ai.integration;

import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.PublishStatus;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.integration.support.IntegrationTestBase;
import com.autoshorts.ai.integration.support.IntegrationTestConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
class PublishPipelineIT extends IntegrationTestBase {

    @Test
    void shouldPublishCompletedJobAndExposePublishStatusEndpoint() throws Exception {
        AuthSession session = registerUser("publish-success");
        UUID jobId = submitGenerate(session, "Publish success topic", "storytelling", 24, null);

        VideoJob completed = awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
        assertThat(completed.getPublishStatus()).isEqualTo(PublishStatus.READY_TO_PUBLISH);
        assertThat(completed.getReviewStatus().name()).isEqualTo("GENERATED");

        postJson("/api/videos/" + jobId + "/approve", Map.of(), session.token(), 200);

        JsonNode publishResponse = postJson(
            "/api/videos/" + jobId + "/publish",
            Map.of("publishPlatform", "mock-social"),
            session.token(),
            200
        );

        assertThat(publishResponse.path("publishStatus").asText()).isEqualTo("PUBLISHED");
        assertThat(publishResponse.path("publishPlatform").asText()).isEqualTo("mock-social");
        assertThat(publishResponse.path("publishProvider").asText()).isEqualTo("mock");
        assertThat(publishResponse.path("publishExternalId").asText()).startsWith("mock-");
        assertThat(publishResponse.path("publishAttemptCount").asInt()).isEqualTo(1);
        assertThat(publishResponse.path("publishedAt").asText()).isNotBlank();

        JsonNode statusResponse = getJson("/api/videos/" + jobId + "/publish", session.token(), 200);
        assertThat(statusResponse.path("publishStatus").asText()).isEqualTo("PUBLISHED");
        assertThat(statusResponse.path("publishPlatform").asText()).isEqualTo("mock-social");
        assertThat(statusResponse.path("publishProvider").asText()).isEqualTo("mock");
        assertThat(statusResponse.path("publishAttemptCount").asInt()).isEqualTo(1);
        assertThat(statusResponse.path("publishable").asBoolean()).isTrue();

        JsonNode idempotentResponse = postJson(
            "/api/videos/" + jobId + "/publish",
            Map.of("publishPlatform", "youtube"),
            session.token(),
            200
        );
        assertThat(idempotentResponse.path("publishStatus").asText()).isEqualTo("PUBLISHED");
        assertThat(idempotentResponse.path("publishAttemptCount").asInt()).isEqualTo(1);
    }

    @Test
    void shouldEnforcePublishOwnershipAndStateGuards() throws Exception {
        AuthSession owner = registerUser("publish-owner");
        AuthSession otherUser = registerUser("publish-other");

        pauseQueueConsumers();
        UUID pendingJobId;
        try {
            pendingJobId = submitGenerate(owner, "Publish pending guard", "facts", 20, null);
            postJson("/api/videos/" + pendingJobId + "/publish", Map.of(), owner.token(), 409);
        } finally {
            resumeQueueConsumers();
        }

        awaitJobStatus(pendingJobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
        postJson("/api/videos/" + pendingJobId + "/approve", Map.of(), owner.token(), 200);
        postJson("/api/videos/" + pendingJobId + "/publish", Map.of(), otherUser.token(), 404);

        JsonNode ownerPublish = postJson("/api/videos/" + pendingJobId + "/publish", Map.of(), owner.token(), 200);
        assertThat(ownerPublish.path("publishStatus").asText()).isEqualTo("PUBLISHED");
    }

    @Test
    void shouldPersistPublishFailureWhenMockPublisherRejects() throws Exception {
        AuthSession session = registerUser("publish-failure");
        UUID jobId = submitGenerate(session, "Topic [publish-fail] should trigger failure", "motivation", 24, null);

        awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
        postJson("/api/videos/" + jobId + "/approve", Map.of(), session.token(), 200);
        postJson("/api/videos/" + jobId + "/publish", Map.of(), session.token(), 502);

        VideoJob failedPublish = awaitJob(
            jobId,
            job -> job.getPublishStatus() == PublishStatus.PUBLISH_FAILED,
            Duration.ofSeconds(10)
        );
        assertThat(failedPublish.getPublishAttemptCount()).isEqualTo(1);
        assertThat(failedPublish.getPublishFailureReason()).isNotBlank();
        assertThat(failedPublish.getPublishLastErrorAt()).isNotNull();

        JsonNode statusResponse = getJson("/api/videos/" + jobId + "/publish", session.token(), 200);
        assertThat(statusResponse.path("publishStatus").asText()).isEqualTo("PUBLISH_FAILED");
        assertThat(statusResponse.path("publishAttemptCount").asInt()).isEqualTo(1);
    }

    @Test
    void shouldRequireApprovalAndTikTokConnectionForTikTokPublish() throws Exception {
        AuthSession session = registerUser("publish-tiktok-guard");
        UUID jobId = submitGenerate(session, "TikTok publish guard", "facts", 24, null);
        awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));

        postJson(
            "/api/videos/" + jobId + "/publish",
            Map.of("publishPlatform", "tiktok"),
            session.token(),
            409
        );

        postJson("/api/videos/" + jobId + "/approve", Map.of(), session.token(), 200);
        postJson(
            "/api/videos/" + jobId + "/publish",
            Map.of("publishPlatform", "tiktok"),
            session.token(),
            409
        );
    }

    @Test
    void shouldNotMarkTikTokScaffoldAsPublishedWhenConnectionExists() throws Exception {
        AuthSession session = registerUser("publish-tiktok-success");
        UUID jobId = submitGenerate(session, "TikTok publish success", "storytelling", 24, null);
        awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
        postJson("/api/videos/" + jobId + "/approve", Map.of(), session.token(), 200);

        postJson(
            "/api/integrations/tiktok/connection",
            Map.of(
                "channelId", session.defaultChannelId().toString(),
                "platformAccountId", "acct-123",
                "platformUsername", "creator_demo",
                "accessToken", "access-token",
                "refreshToken", "refresh-token",
                "status", "ACTIVE"
            ),
            session.token(),
            200
        );

        postJson(
            "/api/videos/" + jobId + "/publish",
            Map.of("publishPlatform", "tiktok"),
            session.token(),
            502
        );

        JsonNode statusResponse = getJson("/api/videos/" + jobId + "/publish", session.token(), 200);
        assertThat(statusResponse.path("publishStatus").asText()).isEqualTo("PUBLISH_FAILED");
        assertThat(statusResponse.path("publishProvider").asText()).isEqualTo("tiktok");
        assertThat(statusResponse.path("publishTargetAccountId").asText()).isEqualTo("acct-123");
        assertThat(statusResponse.path("publishFailureReason").asText()).contains("scaffold mode");
    }
}
