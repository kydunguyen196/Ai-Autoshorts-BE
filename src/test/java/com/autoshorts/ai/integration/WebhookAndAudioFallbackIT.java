package com.autoshorts.ai.integration;

import com.autoshorts.ai.client.ElevenLabsClient;
import com.autoshorts.ai.entity.AudioGenerationMode;
import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.entity.WebhookDelivery;
import com.autoshorts.ai.entity.WebhookDeliveryStatus;
import com.autoshorts.ai.integration.support.IntegrationTestBase;
import com.autoshorts.ai.integration.support.IntegrationTestConfiguration;
import com.autoshorts.ai.repository.WebhookDeliveryRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
@TestPropertySource(properties = {
    "app.webhook.enabled=true",
    "app.webhook.endpoint-url=http://127.0.0.1:1/hook",
    "app.webhook.dispatch-batch-size=10",
    "app.webhook.dispatch-fixed-delay-ms=200",
    "app.webhook.max-attempts=2",
    "app.webhook.retry-initial-delay-ms=100",
    "app.webhook.retry-multiplier=1.0",
    "app.webhook.retry-max-delay-ms=100",
    "app.webhook.request-timeout-seconds=1",
    "spring.datasource.hikari.connection-timeout=1000"
})
class WebhookAndAudioFallbackIT extends IntegrationTestBase {

    @Autowired
    private WebhookDeliveryRepository webhookDeliveryRepository;

    @SpyBean
    private ElevenLabsClient elevenLabsClient;

    @AfterEach
    void resetSpies() {
        Mockito.reset(elevenLabsClient);
    }

    @Test
    void shouldNotBlockMainFlowWhenWebhookDeliveryFails() throws Exception {
        AuthSession session = registerUser("webhook-failure");
        UUID jobId = submitGenerate(session, "Webhook failure resilience", "facts", 24, null);

        VideoJob completed = awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
        assertThat(completed.getFinalVideoUrl()).isNotBlank();

        Awaitility.await()
            .atMost(20, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .until(() -> webhookDeliveryRepository.findAllByJobIdOrderByCreatedAtDesc(jobId).stream()
                .anyMatch(delivery -> delivery.getStatus() == WebhookDeliveryStatus.FAILED));

        List<WebhookDelivery> deliveries = webhookDeliveryRepository.findAllByJobIdOrderByCreatedAtDesc(jobId);
        assertThat(deliveries).isNotEmpty();
        assertThat(deliveries.stream().map(WebhookDelivery::getEventType)).contains("job.completed");
        assertThat(deliveries.stream().allMatch(delivery -> delivery.getAttemptCount() >= 1)).isTrue();
    }

    @Test
    void shouldUseFallbackAudioModeWhenTtsClientThrows() throws Exception {
        doThrow(new RuntimeException("forced_tts_failure"))
            .when(elevenLabsClient)
            .synthesizeSpeech(anyString(), any(), anyInt());

        AuthSession session = registerUser("tts-fallback");
        UUID jobId = submitGenerate(session, "Audio fallback mode", "storytelling", 26, null);

        VideoJob completed = awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));
        assertThat(completed.getAudioUrl()).isNotBlank();
        assertThat(completed.getAudioGenerationMode()).isEqualTo(AudioGenerationMode.FALLBACK);
        assertThat(completed.getAudioProvider()).isEqualTo("deterministic_preview");
        assertThat(getBinary(completed.getAudioUrl(), null, 200)).isNotEmpty();
    }

    @Test
    void shouldNotBlockApproveAndPublishFlowWhenWebhookEndpointIsDown() throws Exception {
        AuthSession session = registerUser("webhook-approve-publish");
        UUID jobId = submitGenerate(session, "Webhook events for approve and publish", "motivation", 24, null);
        awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));

        postJson("/api/videos/" + jobId + "/approve", Map.of(), session.token(), 200);
        postJson(
            "/api/videos/" + jobId + "/publish",
            Map.of("publishPlatform", "mock-social"),
            session.token(),
            200
        );

        Awaitility.await()
            .atMost(20, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .until(() -> webhookDeliveryRepository.findAllByJobIdOrderByCreatedAtDesc(jobId).stream()
                .filter(delivery -> delivery.getEventType().startsWith("publish.") || delivery.getEventType().startsWith("job.approved"))
                .allMatch(delivery -> delivery.getStatus() == WebhookDeliveryStatus.FAILED || delivery.getStatus() == WebhookDeliveryStatus.DELIVERED));

        List<String> eventTypes = webhookDeliveryRepository.findAllByJobIdOrderByCreatedAtDesc(jobId)
            .stream()
            .map(WebhookDelivery::getEventType)
            .toList();
        assertThat(eventTypes).contains("job.approved");
        assertThat(eventTypes).contains("publish.requested");
        assertThat(eventTypes).contains("publish.succeeded");
    }
}
