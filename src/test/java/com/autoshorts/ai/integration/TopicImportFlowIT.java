package com.autoshorts.ai.integration;

import com.autoshorts.ai.entity.TopicIdeaStatus;
import com.autoshorts.ai.integration.support.IntegrationTestBase;
import com.autoshorts.ai.integration.support.IntegrationTestConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
class TopicImportFlowIT extends IntegrationTestBase {

    @Test
    void shouldImportTopicsAndSkipPayloadDuplicates() throws Exception {
        AuthSession session = registerUser("topic-import-dedupe");

        createTopic(
            session,
            "Already active topic",
            "facts",
            2,
            "manual",
            Instant.now().minusSeconds(1)
        );

        JsonNode response = postJson(
            "/api/topics/import",
            Map.of(
                "defaultSource", "bulk-import",
                "defaultChannelId", session.defaultChannelId().toString(),
                "topics", List.of(
                    Map.of("topic", "Fresh topic one", "contentStyle", "motivation", "priority", 10),
                    Map.of("topic", "Fresh topic one", "contentStyle", "motivation", "priority", 10),
                    Map.of("topic", "Already active topic", "contentStyle", "facts", "priority", 8),
                    Map.of("topic", "Fresh topic two", "contentStyle", "storytelling", "priority", 5)
                )
            ),
            session.token(),
            200
        );

        assertThat(response.path("totalRequested").asInt()).isEqualTo(4);
        assertThat(response.path("totalImported").asInt()).isEqualTo(2);
        assertThat(response.path("topics").size()).isEqualTo(2);

        JsonNode listResponse = getJson("/api/topics?limit=50", session.token(), 200);
        List<String> topicNames = java.util.stream.StreamSupport.stream(listResponse.spliterator(), false)
            .map(node -> node.path("topic").asText())
            .toList();
        assertThat(topicNames).contains("Already active topic", "Fresh topic one", "Fresh topic two");
    }

    @Test
    void shouldListImportedTopics() throws Exception {
        AuthSession session = registerUser("topic-import-list");

        JsonNode importResponse = postJson(
            "/api/topics/import",
            Map.of(
                "defaultSource", "csv",
                "defaultChannelId", session.defaultChannelId().toString(),
                "topics", List.of(
                    Map.of("topic", "Topic list A", "contentStyle", "facts", "priority", 1),
                    Map.of("topic", "Topic list B", "contentStyle", "self-improvement", "priority", 3)
                )
            ),
            session.token(),
            200
        );
        assertThat(importResponse.path("totalImported").asInt()).isEqualTo(2);

        JsonNode list = getJson("/api/topics?limit=20", session.token(), 200);
        assertThat(list.isArray()).isTrue();
        assertThat(list.size()).isGreaterThanOrEqualTo(2);

        JsonNode first = list.get(0);
        assertThat(first.path("id").asText()).isNotBlank();
        assertThat(first.path("topic").asText()).isNotBlank();
        assertThat(first.path("status").asText()).isIn(
            TopicIdeaStatus.PENDING.name(),
            TopicIdeaStatus.PROCESSING.name(),
            TopicIdeaStatus.USED.name(),
            TopicIdeaStatus.FAILED.name()
        );
        assertThat(first.path("createdAt").asText()).isNotBlank();
    }
}
