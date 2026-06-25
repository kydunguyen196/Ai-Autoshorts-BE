package com.autoshorts.ai.integration;

import com.autoshorts.ai.dto.GenerateVideoRequest;
import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.TopicIdea;
import com.autoshorts.ai.entity.TopicIdeaStatus;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.integration.support.IntegrationTestBase;
import com.autoshorts.ai.integration.support.IntegrationTestConfiguration;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.orchestration.TopicAutomationScheduler;
import com.autoshorts.ai.service.SettingsService;
import com.autoshorts.ai.service.TopicAutomationService;
import com.autoshorts.ai.service.VideoJobService;
import com.fasterxml.jackson.databind.JsonNode;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
class SchedulerAutomationIT extends IntegrationTestBase {

    @Autowired
    private TopicAutomationService topicAutomationService;

    @Autowired
    private VideoJobService videoJobService;

    @Test
    void shouldDispatchPendingTopicsOnSchedulerCycle() throws Exception {
        AuthSession session = registerUser("scheduler-dispatch");
        JsonNode topicResponse = createTopic(
            session,
            "Scheduler cycle dispatch topic",
            "facts",
            20,
            "manual",
            Instant.now().minusSeconds(5)
        );
        UUID topicId = UUID.fromString(topicResponse.path("id").asText());

        int dispatched = topicAutomationService.dispatchPendingTopics();
        assertThat(dispatched).isEqualTo(1);

        TopicIdea usedTopic = awaitTopicStatus(topicId, TopicIdeaStatus.USED, Duration.ofSeconds(20));
        assertThat(usedTopic.getLastUsedAt()).isNotNull();

        VideoJob created = findJobsForUserAndTopic(session.userId(), "Scheduler cycle dispatch topic").stream()
            .max(Comparator.comparing(VideoJob::getCreatedAt))
            .orElseThrow();
        VideoJob completed = awaitJobStatus(created.getId(), JobStatus.COMPLETED, Duration.ofSeconds(40));
        assertThat(completed.getFinalVideoUrl()).isNotBlank();
    }

    @Test
    void shouldSkipDuplicateClaimsAcrossSchedulerCycles() throws Exception {
        AuthSession session = registerUser("scheduler-idempotent");
        createTopic(
            session,
            "Scheduler duplicate cycle topic",
            "motivation",
            7,
            "manual",
            Instant.now().minusSeconds(2)
        );

        int firstDispatch = topicAutomationService.dispatchPendingTopics();
        assertThat(firstDispatch).isEqualTo(1);

        Awaitility.await()
            .atMost(40, TimeUnit.SECONDS)
            .pollInterval(250, TimeUnit.MILLISECONDS)
            .until(() -> findJobsForUserAndTopic(session.userId(), "Scheduler duplicate cycle topic").stream()
                .anyMatch(job -> job.getStatus() == JobStatus.COMPLETED));

        int secondDispatch = topicAutomationService.dispatchPendingTopics();
        assertThat(secondDispatch).isEqualTo(0);

        List<VideoJob> jobs = findJobsForUserAndTopic(session.userId(), "Scheduler duplicate cycle topic");
        assertThat(jobs).hasSize(1);
    }

    @Test
    void shouldPreventOverlappingSchedulerExecutionsInSingleNode() throws Exception {
        TopicAutomationService mockService = mock(TopicAutomationService.class);
        CountDownLatch runningLatch = new CountDownLatch(1);
        CountDownLatch releaseLatch = new CountDownLatch(1);

        when(mockService.dispatchPendingTopics()).thenAnswer(invocation -> {
            runningLatch.countDown();
            releaseLatch.await(5, TimeUnit.SECONDS);
            return 1;
        });

        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getBoolean(anyString(), anyBoolean())).thenReturn(true);
        TopicAutomationScheduler scheduler =
            new TopicAutomationScheduler(mockService, settingsService, new AppProperties());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(scheduler::runCycle);
            assertThat(runningLatch.await(2, TimeUnit.SECONDS)).isTrue();

            Future<?> second = executor.submit(scheduler::runCycle);
            Awaitility.await()
                .atMost(2, TimeUnit.SECONDS)
                .pollInterval(50, TimeUnit.MILLISECONDS)
                .until(second::isDone);

            verify(mockService, times(1)).dispatchPendingTopics();
            releaseLatch.countDown();

            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldSkipDispatchWhenActiveVideoJobAlreadyExistsForTopicStyle() throws Exception {
        AuthSession session = registerUser("scheduler-active-dup");

        GenerateVideoRequest request = new GenerateVideoRequest();
        request.setTopic("Scheduler active duplicate topic");
        request.setStyle("motivation");
        request.setDurationSeconds(30);
        request.setChannelId(session.defaultChannelId());
        videoJobService.createPendingJob(request, session.userId(), session.defaultChannelId());

        JsonNode topicResponse = createTopic(
            session,
            "Scheduler active duplicate topic",
            "motivation",
            10,
            "manual",
            Instant.now().minusSeconds(2)
        );
        UUID topicId = UUID.fromString(topicResponse.path("id").asText());

        int dispatched = topicAutomationService.dispatchPendingTopics();
        assertThat(dispatched).isEqualTo(0);

        TopicIdea requeued = awaitTopicStatus(topicId, TopicIdeaStatus.PENDING, Duration.ofSeconds(10));
        assertThat(requeued.getScheduledFor()).isNotNull();

        List<VideoJob> jobs = findJobsForUserAndTopic(session.userId(), "Scheduler active duplicate topic");
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getStatus()).isEqualTo(JobStatus.PENDING);
    }
}
