package com.autoshorts.ai.integration;

import com.autoshorts.ai.dto.GenerateVideoRequest;
import com.autoshorts.ai.dto.VideoJobResponse;
import com.autoshorts.ai.entity.GenerationStep;
import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.TopicIdea;
import com.autoshorts.ai.entity.TopicIdeaStatus;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.integration.support.IntegrationTestBase;
import com.autoshorts.ai.integration.support.IntegrationTestConfiguration;
import com.autoshorts.ai.queue.VideoGenerationJobMessage;
import com.autoshorts.ai.queue.VideoJobQueuePublisher;
import com.autoshorts.ai.service.TopicAutomationService;
import com.autoshorts.ai.service.VideoJobService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
class QueueExecutionIT extends IntegrationTestBase {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private VideoJobService videoJobService;

    @Autowired
    private VideoJobQueuePublisher videoJobQueuePublisher;

    @Autowired
    private TopicAutomationService topicAutomationService;

    @SpyBean
    private VideoJobQueuePublisher queuePublisherSpy;

    @AfterEach
    void resetSpies() {
        Mockito.reset(queuePublisherSpy);
    }

    @Test
    void shouldPublishMessageWhenVideoJobIsCreated() throws Exception {
        AuthSession session = registerUser("queue-publish");
        pauseQueueConsumers();
        UUID jobId = null;

        try {
            var response = postJson(
                "/api/videos/generate",
                Map.of(
                    "topic", "Queue publish contract",
                    "style", "motivation",
                    "durationSeconds", 22,
                    "channelId", session.defaultChannelId().toString()
                ),
                session.token(),
                202
            );
            jobId = UUID.fromString(response.path("jobId").asText());

            VideoJob pendingJob = videoJobRepository.findById(jobId).orElseThrow();
            assertThat(pendingJob.getStatus()).isEqualTo(JobStatus.PENDING);
            assertThat(pendingJob.getCurrentStep()).isEqualTo(GenerationStep.QUEUED);
            verify(queuePublisherSpy, atLeastOnce()).publish(eq(jobId), eq("api_generate"));

            long queueDepth = queueMessageCount(appProperties.getQueue().getQueue());
            assertThat(queueDepth).isGreaterThanOrEqualTo(1L);
        } finally {
            resumeQueueConsumers();
        }

        if (jobId != null) {
            awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
        }
    }

    @Test
    void shouldConsumeMessageAndCompleteJobViaWorker() throws Exception {
        AuthSession session = registerUser("queue-consume");

        GenerateVideoRequest request = new GenerateVideoRequest();
        request.setTopic("Queue worker consumption");
        request.setStyle("facts");
        request.setDurationSeconds(24);
        request.setChannelId(session.defaultChannelId());
        request.setVariantCount(1);
        VideoJobResponse created = videoJobService.createPendingJob(request, session.userId(), session.defaultChannelId());

        videoJobQueuePublisher.publish(created.getJobId(), "integration_manual_publish");
        VideoJob completed = awaitJobStatus(created.getJobId(), JobStatus.COMPLETED, Duration.ofSeconds(40));
        assertThat(completed.getAttemptCount()).isEqualTo(1);
        assertThat(completed.getCurrentStep()).isEqualTo(GenerationStep.COMPLETED);
        assertThat(completed.getFinalVideoUrl()).isNotBlank();
    }

    @Test
    void shouldRouteToDeadLetterQueueAfterMaxMessageAttempts() {
        UUID unknownJobId = UUID.randomUUID();
        rabbitTemplate.convertAndSend(
            appProperties.getQueue().getExchange(),
            appProperties.getQueue().getRoutingKey(),
            new VideoGenerationJobMessage(unknownJobId, "integration_dlq_probe", Instant.now())
        );

        Awaitility.await()
            .atMost(20, TimeUnit.SECONDS)
            .pollInterval(250, TimeUnit.MILLISECONDS)
            .until(() -> queueMessageCount(appProperties.getQueue().getDeadLetterQueue()) >= 1L);

        assertThat(queueMessageCount(appProperties.getQueue().getDeadLetterQueue())).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void shouldUseQueueWhenSchedulerDispatchesTopics() throws Exception {
        AuthSession session = registerUser("queue-scheduler");
        var topicResponse = createTopic(
            session,
            "Scheduler queue topic",
            "motivation",
            9,
            "manual",
            Instant.now().minusSeconds(5)
        );
        UUID topicId = UUID.fromString(topicResponse.path("id").asText());

        int dispatched = topicAutomationService.dispatchPendingTopics();
        assertThat(dispatched).isEqualTo(1);

        verify(queuePublisherSpy, atLeastOnce()).publish(org.mockito.ArgumentMatchers.any(UUID.class), eq("scheduler_dispatch"));

        TopicIdea topic = awaitTopicStatus(topicId, TopicIdeaStatus.USED, Duration.ofSeconds(20));
        assertThat(topic.getLastUsedAt()).isNotNull();

        VideoJob generatedJob = findJobsForUserAndTopic(session.userId(), "Scheduler queue topic").stream()
            .max(Comparator.comparing(VideoJob::getCreatedAt))
            .orElseThrow();
        VideoJob completed = awaitJobStatus(generatedJob.getId(), JobStatus.COMPLETED, Duration.ofSeconds(40));
        assertThat(completed.getFinalVideoUrl()).isNotBlank();
    }

    @Test
    void shouldSkipNonPendingJobsDuringConsumption() throws Exception {
        AuthSession session = registerUser("queue-skip");
        UUID jobId = submitGenerate(session, "Skip non-pending consumer behavior", "storytelling", 24, null);
        VideoJob completed = awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
        Integer attemptBefore = completed.getAttemptCount();

        rabbitTemplate.convertAndSend(
            appProperties.getQueue().getExchange(),
            appProperties.getQueue().getRoutingKey(),
            new VideoGenerationJobMessage(jobId, "integration_skip_non_pending", Instant.now())
        );

        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .until(() -> {
                VideoJob current = videoJobRepository.findById(jobId).orElseThrow();
                return Objects.equals(current.getAttemptCount(), attemptBefore) && current.getStatus() == JobStatus.COMPLETED;
            });

        VideoJob unchanged = videoJobRepository.findById(jobId).orElseThrow();
        assertThat(unchanged.getAttemptCount()).isEqualTo(attemptBefore);
        assertThat(unchanged.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(unchanged.getCurrentStep()).isEqualTo(GenerationStep.COMPLETED);
    }
}
