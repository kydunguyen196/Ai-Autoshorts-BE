package com.autoshorts.ai.integration;

import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.integration.support.IntegrationTestBase;
import com.autoshorts.ai.integration.support.IntegrationTestConfiguration;
import com.autoshorts.ai.queue.VideoJobQueuePublisher;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
class BatchGenerationIT extends IntegrationTestBase {

    @SpyBean
    private VideoJobQueuePublisher videoJobQueuePublisher;

    @AfterEach
    void resetSpies() {
        Mockito.reset(videoJobQueuePublisher);
    }

    @Test
    void shouldCreateBatchJobsAndReturnSummary() throws Exception {
        AuthSession session = registerUser("batch-summary");

        JsonNode response = postJson(
            "/api/videos/batch-generate",
            Map.of(
                "defaultChannelId", session.defaultChannelId().toString(),
                "defaultDurationSeconds", 30,
                "items", List.of(
                    Map.of("topic", "Batch summary - 1", "style", "motivation", "durationSeconds", 30),
                    Map.of("topic", "Batch summary - 2", "style", "facts", "durationSeconds", 30),
                    Map.of("topic", "Batch summary - 3", "style", "storytelling", "durationSeconds", 30)
                )
            ),
            session.token(),
            202
        );

        assertThat(response.path("batchId").asText()).isNotBlank();
        assertThat(response.path("totalRequested").asInt()).isEqualTo(3);
        assertThat(response.path("totalAccepted").asInt()).isEqualTo(3);
        assertThat(response.path("jobs").size()).isEqualTo(3);
        for (JsonNode job : response.path("jobs")) {
            assertThat(job.path("jobId").asText()).isNotBlank();
            assertThat(job.path("status").asText()).isEqualTo("PENDING");
            UUID jobId = UUID.fromString(job.path("jobId").asText());
            awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
        }
    }

    @Test
    void shouldReuseExistingPipelineForEachBatchItem() throws Exception {
        AuthSession session = registerUser("batch-pipeline");

        JsonNode response = postJson(
            "/api/videos/batch-generate",
            Map.of(
                "defaultChannelId", session.defaultChannelId().toString(),
                "items", List.of(
                    Map.of("topic", "Batch pipeline topic A", "style", "motivation", "durationSeconds", 24),
                    Map.of("topic", "Batch pipeline topic B", "style", "self-improvement", "durationSeconds", 24)
                )
            ),
            session.token(),
            202
        );

        List<UUID> jobIds = java.util.stream.StreamSupport.stream(response.path("jobs").spliterator(), false)
            .map(node -> UUID.fromString(node.path("jobId").asText()))
            .toList();
        assertThat(jobIds).hasSize(2);

        for (UUID jobId : jobIds) {
            VideoJob terminal = awaitJob(
                jobId,
                job -> job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.FAILED,
                Duration.ofSeconds(40)
            );
            assertThat(terminal.getStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(terminal.getHookText()).isNotBlank();
            assertThat(terminal.getScriptText()).isNotBlank();
            assertThat(terminal.getCaptionText()).isNotBlank();
            assertThat(terminal.getCurrentStep().name()).isEqualTo("COMPLETED");
        }
    }

    @Test
    void shouldSkipDuplicateItemsWithinSameBatchPayload() throws Exception {
        AuthSession session = registerUser("batch-dedupe");

        JsonNode response = postJson(
            "/api/videos/batch-generate",
            Map.of(
                "defaultChannelId", session.defaultChannelId().toString(),
                "items", List.of(
                    Map.of("topic", "Duplicate topic", "style", "motivation", "durationSeconds", 30),
                    Map.of("topic", "Duplicate topic", "style", "motivation", "durationSeconds", 30)
                )
            ),
            session.token(),
            202
        );

        assertThat(response.path("totalRequested").asInt()).isEqualTo(2);
        assertThat(response.path("totalAccepted").asInt()).isEqualTo(1);
        assertThat(response.path("jobs").size()).isEqualTo(2);

        JsonNode accepted = response.path("jobs").get(0);
        JsonNode duplicate = response.path("jobs").get(1);
        UUID acceptedJobId = UUID.fromString(accepted.path("jobId").asText());
        assertThat(duplicate.hasNonNull("jobId")).isFalse();
        assertThat(duplicate.path("errorMessage").asText()).contains("Duplicate topic/style item in batch request");
        awaitJobStatus(acceptedJobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
    }

    @Test
    void shouldMarkJobFailedIfQueuePublishFails() throws Exception {
        AuthSession session = registerUser("batch-dispatch-fail");
        doThrow(new RuntimeException("simulated publish failure"))
            .when(videoJobQueuePublisher)
            .publish(any(UUID.class), eq("batch_generate"));

        JsonNode response = postJson(
            "/api/videos/batch-generate",
            Map.of(
                "defaultChannelId", session.defaultChannelId().toString(),
                "items", List.of(
                    Map.of("topic", "Dispatch fail topic", "style", "facts", "durationSeconds", 25)
                )
            ),
            session.token(),
            202
        );

        JsonNode first = response.path("jobs").get(0);
        UUID jobId = UUID.fromString(first.path("jobId").asText());
        assertThat(first.path("errorMessage").asText()).contains("simulated publish failure");

        VideoJob failed = awaitJobStatus(jobId, JobStatus.FAILED, Duration.ofSeconds(10));
        assertThat(failed.getCurrentStep().name()).isEqualTo("QUEUED");
        assertThat(failed.getErrorMessage()).contains("Batch dispatch failed before queue publish");
    }

    @Test
    void shouldCreateVariantGroupAndComputeRankingAndTopCandidates() throws Exception {
        AuthSession session = registerUser("batch-variant-ranking");

        JsonNode response = postJson(
            "/api/videos/batch-generate",
            Map.of(
                "defaultChannelId", session.defaultChannelId().toString(),
                "items", List.of(
                    Map.of(
                        "topic", "Variant ranking topic",
                        "style", "motivation",
                        "durationSeconds", 30,
                        "variantCount", 5
                    )
                )
            ),
            session.token(),
            202
        );

        JsonNode first = response.path("jobs").get(0);
        UUID groupId = UUID.fromString(first.path("generationGroupId").asText());

        awaitJob(
            UUID.fromString(first.path("jobId").asText()),
            job -> findJobsForUserAndTopic(session.userId(), "Variant ranking topic").stream()
                .filter(j -> Objects.equals(j.getGenerationGroupId(), groupId))
                .filter(j -> j.getStatus() == JobStatus.COMPLETED)
                .count() == 5,
            Duration.ofSeconds(45)
        );

        List<VideoJob> groupJobs = videoJobRepository.findAllByUserIdAndGenerationGroupId(session.userId(), groupId);
        assertThat(groupJobs).hasSize(5);

        for (VideoJob job : groupJobs) {
            int engagement = Math.max(0, Math.min(100, job.getEngagementScore()));
            int hook = Math.max(0, Math.min(100, job.getHookStrengthScore()));
            int expectedRanking = (int) Math.round((engagement * 0.70d) + (hook * 0.30d));
            assertThat(job.getRankingScore()).isEqualTo(expectedRanking);
        }

        List<VideoJob> sorted = groupJobs.stream()
            .sorted(
                Comparator.comparing(VideoJob::getRankingScore, Comparator.reverseOrder())
                    .thenComparing(VideoJob::getEngagementScore, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(VideoJob::getHookStrengthScore, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(VideoJob::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(VideoJob::getId)
            )
            .toList();

        int expectedTopCount = 2;
        List<VideoJob> topCandidates = groupJobs.stream()
            .filter(VideoJob::getTopCandidate)
            .toList();
        assertThat(topCandidates).hasSize(expectedTopCount);
        assertTopCandidateRanksAreContiguous(topCandidates);

        for (int i = 0; i < expectedTopCount; i++) {
            VideoJob expectedTop = sorted.get(i);
            assertThat(expectedTop.getTopCandidate()).isTrue();
            assertThat(expectedTop.getTopCandidateRank()).isEqualTo(i + 1);
        }

        JsonNode topEndpoint = getJson(
            "/api/videos/group/" + groupId + "/top-candidates?page=0&limit=10",
            session.token(),
            200
        );
        assertThat(topEndpoint.path("items").size()).isEqualTo(expectedTopCount);
    }
}
