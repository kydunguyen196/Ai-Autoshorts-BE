package com.autoshorts.ai.integration;

import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.integration.support.IntegrationTestBase;
import com.autoshorts.ai.integration.support.IntegrationTestConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
class AnalyticsIT extends IntegrationTestBase {

    @Test
    void shouldReturnAnalyticsSummaryForUserJobs() throws Exception {
        AuthSession session = registerUser("analytics-user");
        UUID jobId = submitGenerate(session, "Analytics summary topic", "facts", 24, null);
        awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));

        JsonNode summary = getJson("/api/analytics/summary?windowDays=14", session.token(), 200);

        assertThat(summary.path("windowDays").asInt()).isEqualTo(14);
        assertThat(summary.path("totalJobs").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(summary.path("completedJobs").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(summary.path("jobsByStatus").isArray()).isTrue();
        // 14-day window => 14 daily points seeded.
        assertThat(summary.path("timeseries").size()).isEqualTo(14);
        assertThat(summary.path("successRate").asDouble()).isBetween(0.0, 1.0);
    }

    @Test
    void shouldRequireAuthenticationForAnalytics() throws Exception {
        getJson("/api/analytics/summary", null, 401);
    }
}
