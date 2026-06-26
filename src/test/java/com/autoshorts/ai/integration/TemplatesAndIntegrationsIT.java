package com.autoshorts.ai.integration;

import com.autoshorts.ai.integration.support.IntegrationTestBase;
import com.autoshorts.ai.integration.support.IntegrationTestConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage for the Phase 3/5 additive subsystems: video templates CRUD, channel brand kit,
 * and the generic social (YouTube/Instagram) connection upsert.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
class TemplatesAndIntegrationsIT extends IntegrationTestBase {

    @Test
    void shouldCreateListAndDeleteTemplates() throws Exception {
        AuthSession session = registerUser("templates-user");

        JsonNode created = postJson(
            "/api/templates",
            Map.of("name", "Bold Captions", "captionPosition", "center", "primaryColor", "#fff", "makeDefault", true),
            session.token(),
            201
        );
        String templateId = created.path("id").asText();
        assertThat(created.path("name").asText()).isEqualTo("Bold Captions");
        assertThat(created.path("captionPosition").asText()).isEqualTo("CENTER");
        assertThat(created.path("default").asBoolean()).isTrue();

        JsonNode list = getJson("/api/templates", session.token(), 200);
        assertThat(list.isArray()).isTrue();
        assertThat(list.size()).isEqualTo(1);

        deleteRequest("/api/templates/" + templateId, session.token(), 204);
        JsonNode afterDelete = getJson("/api/templates", session.token(), 200);
        assertThat(afterDelete.size()).isEqualTo(0);
    }

    @Test
    void shouldUpdateChannelBrandKit() throws Exception {
        AuthSession session = registerUser("brandkit-user");

        JsonNode updated = putJson(
            "/api/channels/" + session.defaultChannelId() + "/brand-kit",
            Map.of("brandLogoUrl", "https://cdn.example.com/logo.png", "brandPrimaryColor", "#ff0066"),
            session.token(),
            200
        );
        assertThat(updated.path("brandLogoUrl").asText()).isEqualTo("https://cdn.example.com/logo.png");
        assertThat(updated.path("brandPrimaryColor").asText()).isEqualTo("#ff0066");
    }

    @Test
    void shouldUpsertAndReadSocialConnection() throws Exception {
        AuthSession session = registerUser("social-user");

        JsonNode upserted = postJson(
            "/api/integrations/youtube/connection",
            Map.of(
                "channelId", session.defaultChannelId().toString(),
                "platformAccountId", "UC_demo",
                "platformUsername", "demo-channel",
                "accessToken", "yt-access",
                "status", "ACTIVE"
            ),
            session.token(),
            200
        );
        assertThat(upserted.path("platform").asText()).isEqualTo("YOUTUBE");
        assertThat(upserted.path("platformAccountId").asText()).isEqualTo("UC_demo");
        assertThat(upserted.path("active").asBoolean()).isTrue();

        JsonNode status = getJson(
            "/api/integrations/youtube/connection?channelId=" + session.defaultChannelId(),
            session.token(),
            200
        );
        assertThat(status.path("platformUsername").asText()).isEqualTo("demo-channel");
    }

    @Test
    void shouldRejectUnsupportedSocialPlatform() throws Exception {
        AuthSession session = registerUser("social-bad-platform");
        getJson("/api/integrations/tumblr/connection", session.token(), 404);
    }
}
