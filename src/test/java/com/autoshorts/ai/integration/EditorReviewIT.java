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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
class EditorReviewIT extends IntegrationTestBase {

    @Test
    void shouldPauseAtAwaitingReviewThenFinalizeWithEditedScript() throws Exception {
        AuthSession session = registerUser("editor-review");

        var payload = new LinkedHashMap<String, Object>();
        payload.put("topic", "Review before render topic");
        payload.put("style", "storytelling");
        payload.put("durationSeconds", 24);
        payload.put("channelId", session.defaultChannelId().toString());
        payload.put("reviewBeforeRender", true);

        JsonNode created = postJson("/api/videos/generate", payload, session.token(), 202);
        UUID jobId = UUID.fromString(created.path("jobId").asText());

        // Phase A: stops after content generation for review.
        awaitJobStatus(jobId, JobStatus.AWAITING_REVIEW, Duration.ofSeconds(40));

        JsonNode draftStatus = getJson("/api/videos/" + jobId, session.token(), 200);
        assertThat(draftStatus.path("status").asText()).isEqualTo("AWAITING_REVIEW");
        assertThat(draftStatus.path("reviewBeforeRender").asBoolean()).isTrue();
        assertThat(draftStatus.path("finalVideoUrl").asText("")).isBlank();

        // Edit the draft script + caption.
        JsonNode edited = patchJson(
            "/api/videos/" + jobId + "/draft",
            Map.of("scriptText", "An edited script ready for rendering.", "captionText", "Edited caption"),
            session.token(),
            200
        );
        assertThat(edited.path("scriptText").asText()).isEqualTo("An edited script ready for rendering.");
        assertThat(edited.path("captionText").asText()).isEqualTo("Edited caption");

        // Phase B: finalize → render pipeline → COMPLETED with the edited script.
        postJson("/api/videos/" + jobId + "/finalize", Map.of(), session.token(), 202);
        awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));

        JsonNode completed = getJson("/api/videos/" + jobId, session.token(), 200);
        assertThat(completed.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(completed.path("finalVideoUrl").asText("")).isNotBlank();
        assertThat(completed.path("scriptText").asText()).isEqualTo("An edited script ready for rendering.");
    }

    @Test
    void shouldRejectDraftEditWhenNotAwaitingReview() throws Exception {
        AuthSession session = registerUser("editor-review-guard");
        UUID jobId = submitGenerate(session, "Normal render topic", "facts", 24, null);
        awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(40));

        // A COMPLETED job (no review gate) cannot be edited as a draft.
        patchJson(
            "/api/videos/" + jobId + "/draft",
            Map.of("scriptText", "should fail"),
            session.token(),
            409
        );
    }
}
