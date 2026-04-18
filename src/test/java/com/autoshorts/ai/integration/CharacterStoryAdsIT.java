package com.autoshorts.ai.integration;

import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.integration.support.IntegrationTestBase;
import com.autoshorts.ai.integration.support.IntegrationTestConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
class CharacterStoryAdsIT extends IntegrationTestBase {

    @Test
    void shouldEnforceCharacterProfileAndCampaignOwnershipCrud() throws Exception {
        AuthSession owner = registerUser("character-owner");
        AuthSession other = registerUser("character-other");

        JsonNode profile = postJson(
            "/api/characters/profiles",
            Map.of(
                "channelId", owner.defaultChannelId().toString(),
                "name", "Mika",
                "archetype", "bold-mentor",
                "toneOfVoice", "confident",
                "speakingStyle", "short punchy lines"
            ),
            owner.token(),
            200
        );
        UUID profileId = UUID.fromString(profile.path("id").asText());

        JsonNode campaign = postJson(
            "/api/characters/campaigns",
            Map.of(
                "channelId", owner.defaultChannelId().toString(),
                "characterProfileId", profileId.toString(),
                "productName", "Creator Course",
                "productType", "digital",
                "campaignObjective", "drive trial signups"
            ),
            owner.token(),
            200
        );
        UUID campaignId = UUID.fromString(campaign.path("id").asText());

        JsonNode ownerProfiles = getJson("/api/characters/profiles", owner.token(), 200);
        assertThat(ownerProfiles.isArray()).isTrue();
        List<String> ownerProfileIds = java.util.stream.StreamSupport.stream(ownerProfiles.spliterator(), false)
            .map(node -> node.path("id").asText())
            .toList();
        assertThat(ownerProfileIds).contains(profileId.toString());

        getJson("/api/characters/profiles/" + profileId, other.token(), 404);
        getJson("/api/characters/campaigns/" + campaignId, other.token(), 404);

        JsonNode updatedProfile = putJson(
            "/api/characters/profiles/" + profileId,
            Map.of(
                "channelId", owner.defaultChannelId().toString(),
                "name", "Mika Updated",
                "archetype", "bold-mentor",
                "toneOfVoice", "assertive",
                "speakingStyle", "narrative"
            ),
            owner.token(),
            200
        );
        assertThat(updatedProfile.path("name").asText()).isEqualTo("Mika Updated");

        deleteRequest("/api/characters/campaigns/" + campaignId, owner.token(), 204);
        getJson("/api/characters/campaigns/" + campaignId, owner.token(), 404);
    }

    @Test
    void shouldPersistCharacterAwareGenerationMetadataOnVideoJob() throws Exception {
        AuthSession session = registerUser("character-metadata");

        JsonNode profile = postJson(
            "/api/characters/profiles",
            Map.of(
                "channelId", session.defaultChannelId().toString(),
                "name", "Ari",
                "archetype", "founder-storyteller",
                "personality", "empathetic and practical",
                "toneOfVoice", "warm",
                "speakingStyle", "conversational"
            ),
            session.token(),
            200
        );
        UUID profileId = UUID.fromString(profile.path("id").asText());

        JsonNode campaign = postJson(
            "/api/characters/campaigns",
            Map.of(
                "channelId", session.defaultChannelId().toString(),
                "characterProfileId", profileId.toString(),
                "productName", "Skill Sprint",
                "productType", "course",
                "productDescription", "30-day storytelling sprint",
                "campaignObjective", "increase paid enrollments",
                "targetPlatform", "tiktok"
            ),
            session.token(),
            200
        );
        UUID campaignId = UUID.fromString(campaign.path("id").asText());

        JsonNode generateResponse = postJson(
            "/api/videos/generate",
            Map.ofEntries(
                Map.entry("topic", "Story ad for skill sprint"),
                Map.entry("style", "storytelling"),
                Map.entry("durationSeconds", 24),
                Map.entry("channelId", session.defaultChannelId().toString()),
                Map.entry("characterProfileId", profileId.toString()),
                Map.entry("characterCampaignId", campaignId.toString()),
                Map.entry("storyAngle", "before-after creator journey"),
                Map.entry("productPlacementMode", "soft"),
                Map.entry("adDisclosureMode", "clear"),
                Map.entry("sceneCountTarget", 4),
                Map.entry("characterConsistencyMode", "strict")
            ),
            session.token(),
            202
        );

        UUID jobId = UUID.fromString(generateResponse.path("jobId").asText());
        VideoJob completed = awaitJobStatus(jobId, JobStatus.COMPLETED, Duration.ofSeconds(45));

        assertThat(completed.getCharacterProfileId()).isEqualTo(profileId);
        assertThat(completed.getCharacterCampaignId()).isEqualTo(campaignId);
        assertThat(completed.getStoryAngle()).isEqualTo("before-after creator journey");
        assertThat(completed.getProductPlacementMode()).isEqualTo("soft");
        assertThat(completed.getAdDisclosureMode()).isEqualTo("clear");
        assertThat(completed.getSceneCountTarget()).isEqualTo(4);
        assertThat(completed.getCharacterConsistencyMode()).isEqualTo("strict");

        JsonNode detail = getJson("/api/videos/" + jobId, session.token(), 200);
        assertThat(detail.path("characterProfileId").asText()).isEqualTo(profileId.toString());
        assertThat(detail.path("characterCampaignId").asText()).isEqualTo(campaignId.toString());
        assertThat(detail.path("storyAngle").asText()).isEqualTo("before-after creator journey");
    }

    @Test
    void shouldApplyApprovalRejectAndPreferredSelectionRulesBeforePublish() throws Exception {
        AuthSession session = registerUser("review-flow");

        JsonNode response = postJson(
            "/api/videos/generate",
            Map.of(
                "topic", "Review and select winner",
                "style", "motivation",
                "durationSeconds", 24,
                "variantCount", 3,
                "channelId", session.defaultChannelId().toString()
            ),
            session.token(),
            202
        );

        UUID groupId = UUID.fromString(response.path("generationGroupId").asText());

        Awaitility.await()
            .atMost(45, TimeUnit.SECONDS)
            .pollInterval(250, TimeUnit.MILLISECONDS)
            .until(() -> videoJobRepository.findAllByUserIdAndGenerationGroupId(session.userId(), groupId).stream()
                .filter(job -> job.getStatus() == JobStatus.COMPLETED)
                .count() == 3);

        List<VideoJob> jobs = videoJobRepository.findAllByUserIdAndGenerationGroupId(session.userId(), groupId)
            .stream()
            .sorted(Comparator.comparing(VideoJob::getVariantIndex))
            .toList();

        UUID firstJobId = jobs.get(0).getId();
        UUID secondJobId = jobs.get(1).getId();
        UUID thirdJobId = jobs.get(2).getId();

        postJson("/api/videos/" + firstJobId + "/publish", Map.of(), session.token(), 409);

        postJson("/api/videos/" + firstJobId + "/approve", Map.of(), session.token(), 200);
        postJson("/api/videos/" + secondJobId + "/approve", Map.of(), session.token(), 200);
        postJson(
            "/api/videos/" + thirdJobId + "/reject",
            Map.of("rejectionReason", "Too generic for campaign"),
            session.token(),
            200
        );

        postJson("/api/videos/" + firstJobId + "/select-for-publish", Map.of(), session.token(), 200);
        postJson("/api/videos/" + secondJobId + "/select-for-publish", Map.of(), session.token(), 200);

        JsonNode firstJob = getJson("/api/videos/" + firstJobId, session.token(), 200);
        JsonNode secondJob = getJson("/api/videos/" + secondJobId, session.token(), 200);
        JsonNode thirdJob = getJson("/api/videos/" + thirdJobId, session.token(), 200);

        assertThat(firstJob.path("selectedForPublish").asBoolean()).isFalse();
        assertThat(secondJob.path("selectedForPublish").asBoolean()).isTrue();
        assertThat(thirdJob.path("reviewStatus").asText()).isEqualTo("REJECTED");

        JsonNode summary = getJson("/api/videos/group/" + groupId + "/review-summary", session.token(), 200);
        assertThat(summary.path("totalJobs").asInt()).isEqualTo(3);
        assertThat(summary.path("selectedJobId").asText()).isEqualTo(secondJobId.toString());
        assertThat(summary.path("reviewStatusCounts").path("APPROVED").asInt()).isEqualTo(2);
        assertThat(summary.path("reviewStatusCounts").path("REJECTED").asInt()).isEqualTo(1);

        postJson("/api/videos/" + firstJobId + "/publish", Map.of(), session.token(), 409);

        JsonNode publishSecond = postJson(
            "/api/videos/" + secondJobId + "/publish",
            Map.of("publishPlatform", "mock-social"),
            session.token(),
            200
        );
        assertThat(publishSecond.path("publishStatus").asText()).isEqualTo("PUBLISHED");
    }
}
