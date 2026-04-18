package com.autoshorts.ai.integration.support;

import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.TopicIdea;
import com.autoshorts.ai.entity.TopicIdeaStatus;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.repository.TopicIdeaRepository;
import com.autoshorts.ai.repository.VideoJobRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.awaitility.Awaitility;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class IntegrationTestBase {

    private static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SYSTEM_CHANNEL_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:16-alpine")
    )
        .withDatabaseName("autoshorts")
        .withUsername("autoshorts")
        .withPassword("autoshorts");

    private static final EmbeddedPostgres EMBEDDED_POSTGRES;
    private static final String EMBEDDED_JDBC_URL;
    private static final String EMBEDDED_USERNAME;
    private static final String EMBEDDED_PASSWORD;
    private static final boolean USE_POSTGRES_CONTAINER;
    private static final boolean USE_EMBEDDED_POSTGRES;

    static {
        boolean container = false;
        boolean embedded = false;
        EmbeddedPostgres embeddedPostgres = null;
        String embeddedUrl = null;
        String embeddedUser = null;
        String embeddedPassword = null;
        String containerError = null;
        String embeddedError = null;

        try {
            POSTGRESQL.start();
            container = true;
        } catch (Throwable ex) {
            containerError = rootCauseMessage(ex);
            System.out.println("Testcontainers PostgreSQL unavailable, trying embedded PostgreSQL: " + containerError);
        }

        if (!container) {
            try {
                embeddedPostgres = EmbeddedPostgres.builder().start();
                String candidateUrl = "jdbc:postgresql://localhost:" + embeddedPostgres.getPort() + "/postgres";
                String[] users = {"postgres"};
                String[] passwords = {"postgres", ""};

                boolean connected = false;
                for (String user : users) {
                    for (String password : passwords) {
                        if (canConnect(candidateUrl, user, password)) {
                            embeddedUrl = candidateUrl;
                            embeddedUser = user;
                            embeddedPassword = password;
                            connected = true;
                            break;
                        }
                    }
                    if (connected) {
                        break;
                    }
                }

                if (!connected) {
                    throw new IllegalStateException(
                        "Embedded PostgreSQL started but no valid credential pair could connect to " + candidateUrl
                    );
                }
                embedded = true;
            } catch (Throwable embeddedEx) {
                embeddedError = rootCauseMessage(embeddedEx);
                System.out.println("Embedded PostgreSQL unavailable: " + embeddedError);
            }
        }

        if (!container && !embedded) {
            throw new ExceptionInInitializerError(
                "No PostgreSQL backend available for integration tests. "
                    + "Tried Testcontainers (" + containerError + ") and embedded postgres (" + embeddedError + ")."
            );
        }

        EMBEDDED_POSTGRES = embeddedPostgres;
        EMBEDDED_JDBC_URL = embeddedUrl;
        EMBEDDED_USERNAME = embeddedUser;
        EMBEDDED_PASSWORD = embeddedPassword;
        USE_POSTGRES_CONTAINER = container;
        USE_EMBEDDED_POSTGRES = embedded;

        if (USE_EMBEDDED_POSTGRES && EMBEDDED_POSTGRES != null) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    EMBEDDED_POSTGRES.close();
                } catch (Exception ignored) {
                    // best effort shutdown for test process
                }
            }));
        }
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        return message == null ? cursor.getClass().getSimpleName() : message;
    }

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        if (USE_POSTGRES_CONTAINER) {
            registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRESQL::getUsername);
            registry.add("spring.datasource.password", POSTGRESQL::getPassword);
        } else if (USE_EMBEDDED_POSTGRES) {
            registry.add("spring.datasource.url", () -> EMBEDDED_JDBC_URL);
            registry.add("spring.datasource.username", () -> EMBEDDED_USERNAME);
            registry.add("spring.datasource.password", () -> EMBEDDED_PASSWORD);
        }

        registry.add("app.working-dir", () -> "./build/test-work/" + UUID.randomUUID());
        registry.add("app.security.jwt-secret", () -> "integration-test-jwt-secret-minimum-32-characters-123456");
        registry.add("app.openai.mock", () -> "true");
        registry.add("app.elevenlabs.mock", () -> "true");
        registry.add("app.storage.mock", () -> "true");
        registry.add("app.redis.cache-enabled", () -> "false");
        registry.add("app.queue.enabled", () -> "true");
        registry.add("app.queue.consumer-concurrency", () -> "1");
        registry.add("app.queue.max-processing-attempts", () -> "2");
        registry.add("app.queue.retry-initial-interval-ms", () -> "100");
        registry.add("app.queue.retry-multiplier", () -> "1.0");
        registry.add("app.queue.retry-max-interval-ms", () -> "200");
        registry.add("app.scheduler.enabled", () -> "false");
        registry.add("app.cleanup.delete-temp-files", () -> "true");
        registry.add("app.cleanup.keep-failed-job-files", () -> "false");
    }

    private static boolean canConnect(String jdbcUrl, String username, String password) {
        try (Connection ignored = DriverManager.getConnection(jdbcUrl, username, password)) {
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected RabbitAdmin rabbitAdmin;

    @Autowired
    protected RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    @Autowired
    protected AppProperties appProperties;

    @Autowired
    protected VideoJobRepository videoJobRepository;

    @Autowired
    protected TopicIdeaRepository topicIdeaRepository;

    @Autowired
    protected DeterministicFfmpegVideoComposer deterministicFfmpegVideoComposer;

    @org.junit.jupiter.api.BeforeEach
    void resetIntegrationState() {
        deterministicFfmpegVideoComposer.resetFailure();
        resumeQueueConsumers();
        purgeQueues();
        purgeDomainData();
    }

    protected void purgeDomainData() {
        jdbcTemplate.update("DELETE FROM video_jobs");
        jdbcTemplate.update("DELETE FROM topic_ideas");
        jdbcTemplate.update("DELETE FROM channels WHERE id <> ?", SYSTEM_CHANNEL_ID);
        jdbcTemplate.update("DELETE FROM app_users WHERE id <> ?", SYSTEM_USER_ID);
    }

    protected void purgeQueues() {
        try {
            rabbitAdmin.purgeQueue(appProperties.getQueue().getQueue(), true);
        } catch (Exception ignored) {
            // queue may not exist yet in early startup edge-cases
        }
        try {
            rabbitAdmin.purgeQueue(appProperties.getQueue().getDeadLetterQueue(), true);
        } catch (Exception ignored) {
            // queue may not exist yet in early startup edge-cases
        }
    }

    protected AuthSession registerUser(String displayPrefix) throws Exception {
        String email = "it+" + UUID.randomUUID() + "@autoshorts.test";
        String password = "Password123!";
        String displayName = displayPrefix + "-" + UUID.randomUUID().toString().substring(0, 8);

        JsonNode registerPayload = objectMapper.valueToTree(Map.of(
            "email", email,
            "password", password,
            "displayName", displayName
        ));
        JsonNode registerResponse = postJson("/api/auth/register", registerPayload, null, 200);

        return new AuthSession(
            UUID.fromString(registerResponse.path("user").path("id").asText()),
            email,
            password,
            registerResponse.path("accessToken").asText(),
            UUID.fromString(registerResponse.path("defaultChannel").path("id").asText())
        );
    }

    protected JsonNode login(String email, String password) throws Exception {
        JsonNode loginPayload = objectMapper.valueToTree(Map.of("email", email, "password", password));
        return postJson("/api/auth/login", loginPayload, null, 200);
    }

    protected JsonNode getJson(String uri, String bearerToken, int expectedStatus) throws Exception {
        var requestBuilder = get(uri).accept(MediaType.APPLICATION_JSON);
        if (bearerToken != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }

        MvcResult result = mockMvc.perform(requestBuilder)
            .andExpect(status().is(expectedStatus))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected JsonNode postJson(String uri, Object body, String bearerToken, int expectedStatus) throws Exception {
        var requestBuilder = post(uri)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(body));

        if (bearerToken != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }

        MvcResult result = mockMvc.perform(requestBuilder)
            .andExpect(status().is(expectedStatus))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected JsonNode putJson(String uri, Object body, String bearerToken, int expectedStatus) throws Exception {
        var requestBuilder = put(uri)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(body));

        if (bearerToken != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }

        MvcResult result = mockMvc.perform(requestBuilder)
            .andExpect(status().is(expectedStatus))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected void deleteRequest(String uri, String bearerToken, int expectedStatus) throws Exception {
        var requestBuilder = delete(uri);
        if (bearerToken != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }

        mockMvc.perform(requestBuilder)
            .andExpect(status().is(expectedStatus));
    }

    protected byte[] getBinary(String absoluteOrRelativeUri, String bearerToken, int expectedStatus) throws Exception {
        String path = absoluteOrRelativeUri;
        if (absoluteOrRelativeUri.startsWith("http://") || absoluteOrRelativeUri.startsWith("https://")) {
            path = URI.create(absoluteOrRelativeUri).getRawPath();
        }

        var requestBuilder = get(path);
        if (bearerToken != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }

        MvcResult result = mockMvc.perform(requestBuilder)
            .andExpect(status().is(expectedStatus))
            .andReturn();
        return result.getResponse().getContentAsByteArray();
    }

    protected UUID submitGenerate(AuthSession session, String topic, String style, int durationSeconds, Integer variantCount) throws Exception {
        var payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("topic", topic);
        payload.put("style", style);
        payload.put("durationSeconds", durationSeconds);
        payload.put("channelId", session.defaultChannelId().toString());
        if (variantCount != null) {
            payload.put("variantCount", variantCount);
        }

        JsonNode response = postJson("/api/videos/generate", payload, session.token(), 202);
        return UUID.fromString(response.path("jobId").asText());
    }

    protected JsonNode createTopic(
        AuthSession session,
        String topic,
        String contentStyle,
        int priority,
        String source,
        Instant scheduledFor
    ) throws Exception {
        var payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("topic", topic);
        payload.put("contentStyle", contentStyle);
        payload.put("priority", priority);
        payload.put("source", source);
        payload.put("channelId", session.defaultChannelId().toString());
        if (scheduledFor != null) {
            payload.put("scheduledFor", scheduledFor.toString());
        }
        return postJson("/api/topics", payload, session.token(), 200);
    }

    protected VideoJob awaitJobStatus(UUID jobId, JobStatus expectedStatus, Duration timeout) {
        return awaitJob(jobId, job -> job.getStatus() == expectedStatus, timeout);
    }

    protected VideoJob awaitJob(UUID jobId, Predicate<VideoJob> condition, Duration timeout) {
        Awaitility.await()
            .atMost(timeout)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .until(() -> {
                Optional<VideoJob> maybe = videoJobRepository.findById(jobId);
                return maybe.filter(condition).isPresent();
            });
        return videoJobRepository.findById(jobId).orElseThrow();
    }

    protected TopicIdea awaitTopicStatus(UUID topicId, TopicIdeaStatus expectedStatus, Duration timeout) {
        Awaitility.await()
            .atMost(timeout)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .until(() -> {
                Optional<TopicIdea> maybe = topicIdeaRepository.findById(topicId);
                return maybe.filter(topic -> topic.getStatus() == expectedStatus).isPresent();
            });
        return topicIdeaRepository.findById(topicId).orElseThrow();
    }

    protected void pauseQueueConsumers() {
        rabbitListenerEndpointRegistry.getListenerContainers().forEach(container -> {
            if (container.isRunning()) {
                container.stop();
            }
        });
    }

    protected void resumeQueueConsumers() {
        rabbitListenerEndpointRegistry.getListenerContainers().forEach(container -> {
            if (!container.isRunning()) {
                container.start();
            }
        });
    }

    protected long queueMessageCount(String queueName) {
        Properties props = rabbitAdmin.getQueueProperties(queueName);
        if (props == null) {
            return 0;
        }
        Object count = props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
        return count instanceof Number number ? number.longValue() : 0L;
    }

    protected List<VideoJob> findJobsForUserAndTopic(UUID userId, String topic) {
        return videoJobRepository.findAll().stream()
            .filter(job -> Objects.equals(job.getUserId(), userId))
            .filter(job -> Objects.equals(job.getTopic(), topic))
            .toList();
    }

    protected void assertTopCandidateRanksAreContiguous(List<VideoJob> candidates) {
        List<Integer> sortedRanks = candidates.stream()
            .map(VideoJob::getTopCandidateRank)
            .filter(Objects::nonNull)
            .sorted()
            .toList();
        for (int i = 0; i < sortedRanks.size(); i++) {
            assertThat(sortedRanks.get(i)).isEqualTo(i + 1);
        }
    }

    protected record AuthSession(
        UUID userId,
        String email,
        String password,
        String token,
        UUID defaultChannelId
    ) {
    }
}
