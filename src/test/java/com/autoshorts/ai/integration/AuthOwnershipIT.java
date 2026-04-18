package com.autoshorts.ai.integration;

import com.autoshorts.ai.entity.JobStatus;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
class AuthOwnershipIT extends IntegrationTestBase {

    @Test
    void shouldRegisterLoginAndReadCurrentUserProfile() throws Exception {
        String email = "it+" + UUID.randomUUID() + "@autoshorts.test";
        String password = "Password123!";
        String displayName = "owner-auth";

        JsonNode registerResponse = postJson(
            "/api/auth/register",
            Map.of("email", email, "password", password, "displayName", displayName),
            null,
            200
        );
        assertThat(registerResponse.path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(registerResponse.path("accessToken").asText()).isNotBlank();
        assertThat(registerResponse.path("defaultChannel").path("id").asText()).isNotBlank();

        JsonNode loginResponse = login(email, password);
        assertThat(loginResponse.path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(loginResponse.path("accessToken").asText()).isNotBlank();

        String token = loginResponse.path("accessToken").asText();
        JsonNode meResponse = getJson("/api/auth/me", token, 200);
        assertThat(meResponse.path("user").path("email").asText()).isEqualTo(email);
        assertThat(meResponse.path("user").path("displayName").asText()).isEqualTo(displayName);
        assertThat(meResponse.path("defaultChannel").path("id").asText()).isEqualTo(
            registerResponse.path("defaultChannel").path("id").asText()
        );
    }

    @Test
    void shouldRestrictProtectedEndpointsWithoutToken() throws Exception {
        postJson(
            "/api/videos/generate",
            Map.of(
                "topic", "Unauthorized request should fail",
                "style", "motivation",
                "durationSeconds", 30
            ),
            null,
            401
        );
    }

    @Test
    void shouldEnforcePerUserOwnershipForJobsAndTopics() throws Exception {
        AuthSession userA = registerUser("owner-A");
        AuthSession userB = registerUser("owner-B");

        UUID userAJobId = submitGenerate(
            userA,
            "Ownership private job",
            "motivation",
            25,
            null
        );

        JsonNode userATopic = createTopic(
            userA,
            "Ownership private topic",
            "facts",
            5,
            "manual",
            null
        );

        getJson("/api/videos/" + userAJobId, userB.token(), 404);

        JsonNode userBVideos = getJson("/api/videos?limit=50", userB.token(), 200);
        List<String> userBVideoIds = java.util.stream.StreamSupport.stream(userBVideos.spliterator(), false)
            .map(node -> node.path("jobId").asText())
            .toList();
        assertThat(userBVideoIds).doesNotContain(userAJobId.toString());

        JsonNode userBTopics = getJson("/api/topics?limit=50", userB.token(), 200);
        List<String> userBTopicIds = java.util.stream.StreamSupport.stream(userBTopics.spliterator(), false)
            .map(node -> node.path("id").asText())
            .toList();
        assertThat(userBTopicIds).doesNotContain(userATopic.path("id").asText());

        postJson(
            "/api/videos/generate",
            Map.of(
                "topic", "Cross-channel ownership violation",
                "style", "motivation",
                "durationSeconds", 30,
                "channelId", userA.defaultChannelId().toString()
            ),
            userB.token(),
            404
        );

        awaitJobStatus(userAJobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
    }

    @Test
    void shouldScopeBatchAndRetryOperationsToCurrentUser() throws Exception {
        AuthSession userA = registerUser("batch-A");
        AuthSession userB = registerUser("batch-B");

        JsonNode batchResponse = postJson(
            "/api/videos/batch-generate",
            Map.of(
                "defaultChannelId", userA.defaultChannelId().toString(),
                "items", List.of(
                    Map.of(
                        "topic", "User A scoped batch item",
                        "style", "facts",
                        "durationSeconds", 30
                    )
                )
            ),
            userA.token(),
            202
        );

        UUID firstBatchJobId = UUID.fromString(batchResponse.path("jobs").get(0).path("jobId").asText());
        awaitJobStatus(firstBatchJobId, JobStatus.COMPLETED, Duration.ofSeconds(30));

        JsonNode userBVideos = getJson("/api/videos?limit=50", userB.token(), 200);
        List<String> userBJobIds = java.util.stream.StreamSupport.stream(userBVideos.spliterator(), false)
            .map(node -> node.path("jobId").asText())
            .toList();
        assertThat(userBJobIds).doesNotContain(firstBatchJobId.toString());

        deterministicFfmpegVideoComposer.failNextComposition("forced-failure-for-retry-ownership-test");
        UUID failedJobId = submitGenerate(userA, "Retry ownership test", "motivation", 25, null);
        awaitJobStatus(failedJobId, JobStatus.FAILED, Duration.ofSeconds(10));

        postJson("/api/videos/" + failedJobId + "/retry", Map.of(), userB.token(), 404);

        JsonNode retryResponse = postJson("/api/videos/" + failedJobId + "/retry", Map.of(), userA.token(), 202);
        assertThat(retryResponse.path("jobId").asText()).isEqualTo(failedJobId.toString());
        assertThat(retryResponse.path("status").asText()).isEqualTo("PENDING");

        VideoJob retried = awaitJobStatus(failedJobId, JobStatus.COMPLETED, Duration.ofSeconds(30));
        assertThat(retried.getFinalVideoUrl()).isNotBlank();
    }
}
