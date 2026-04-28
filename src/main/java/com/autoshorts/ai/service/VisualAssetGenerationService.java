package com.autoshorts.ai.service;

import com.autoshorts.ai.client.VisualGenerationClient;
import com.autoshorts.ai.client.model.GeneratedVisualImage;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.entity.VisualGenerationMode;
import com.autoshorts.ai.ffmpeg.SceneMediaSegment;
import com.autoshorts.ai.storage.StorageClient;
import com.autoshorts.ai.visual.DeterministicPreviewVisualGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VisualAssetGenerationService {

    private static final Logger log = LoggerFactory.getLogger(VisualAssetGenerationService.class);

    private final VisualGenerationClient visualGenerationClient;
    private final DeterministicPreviewVisualGenerator deterministicPreviewVisualGenerator;
    private final StorageClient storageClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public VisualAssetGenerationService(
        VisualGenerationClient visualGenerationClient,
        DeterministicPreviewVisualGenerator deterministicPreviewVisualGenerator,
        StorageClient storageClient,
        AppProperties appProperties,
        ObjectMapper objectMapper
    ) {
        this.visualGenerationClient = visualGenerationClient;
        this.deterministicPreviewVisualGenerator = deterministicPreviewVisualGenerator;
        this.storageClient = storageClient;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    public GeneratedVisualAssets generateAndUploadSceneAssets(VideoJob job, Path jobDir) throws IOException {
        if (!appProperties.getVisual().isEnabled()) {
            log.info("event=visual_generation_skipped reason=disabled jobId={}", job.getId());
            return new GeneratedVisualAssets(
                List.of(),
                "[]",
                VisualGenerationMode.FALLBACK,
                "visual_disabled",
                null,
                "disabled",
                null
            );
        }

        List<ScenePlan> plans = buildScenePlans(job);
        List<SceneMediaSegment> compositionSegments = new ArrayList<>();
        List<Map<String, Object>> sceneAssets = new ArrayList<>();
        List<GeneratedVisualImage> generated = new ArrayList<>();

        for (ScenePlan plan : plans) {
            String prompt = buildVisualPrompt(job, plan);
            GeneratedVisualImage image = generateVisualSafely(prompt, plan.index());
            generated.add(image);

            String fileName = "scene-%02d.%s".formatted(plan.index() + 1, image.getFileExtension());
            Path localPath = jobDir.resolve(fileName);
            Files.write(localPath, image.getData());

            String objectKey = buildObjectKey(job.getId(), fileName);
            String assetUrl = storageClient.upload(localPath, objectKey, image.getContentType());

            compositionSegments.add(new SceneMediaSegment(localPath, plan.startSec(), plan.endSec()));
            sceneAssets.add(buildSceneAssetMetadata(plan, prompt, image, assetUrl));
        }

        VisualGenerationMode mode = resolveOverallMode(generated);
        String provider = resolveProvider(generated);
        String modelId = resolveModelId(generated);
        String failureReason = resolveFailureReason(generated);
        String failureDetails = resolveFailureDetails(generated);
        String sceneAssetsJson = toJson(sceneAssets);

        log.info(
            "event=visual_assets_ready jobId={} scenes={} mode={} provider={} modelId={} failureReason={}",
            job.getId(),
            compositionSegments.size(),
            mode,
            provider,
            modelId,
            failureReason
        );

        return new GeneratedVisualAssets(
            compositionSegments,
            sceneAssetsJson,
            mode,
            provider,
            modelId,
            failureReason,
            failureDetails
        );
    }

    private List<ScenePlan> buildScenePlans(VideoJob job) {
        int maxScenes = Math.max(1, appProperties.getVisual().getMaxScenes());
        List<String> sceneDescriptions = extractSceneDescriptions(job.getSceneBreakdownJson());
        int inferredSceneCount = Math.max(2, Math.min(maxScenes, Math.max(1, job.getDurationSeconds() / 8)));
        int sceneCount = sceneDescriptions.isEmpty()
            ? inferredSceneCount
            : Math.max(1, Math.min(maxScenes, sceneDescriptions.size()));
        int totalDuration = Math.max(5, job.getDurationSeconds());

        List<ScenePlan> plans = new ArrayList<>();
        for (int index = 0; index < sceneCount; index++) {
            int startSec = Math.max(0, (index * totalDuration) / sceneCount);
            int endSec = Math.max(startSec + 1, ((index + 1) * totalDuration) / sceneCount);
            if (index == sceneCount - 1) {
                endSec = totalDuration;
            }
            String description = index < sceneDescriptions.size()
                ? sceneDescriptions.get(index)
                : "story beat %d".formatted(index + 1);
            plans.add(new ScenePlan(index, startSec, endSec, description));
        }
        return plans;
    }

    private List<String> extractSceneDescriptions(String sceneBreakdownJson) {
        if (!StringUtils.hasText(sceneBreakdownJson)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(sceneBreakdownJson);
            JsonNode sceneArray = root;
            if (root != null && root.isObject()) {
                JsonNode maybeScenes = root.get("scenes");
                if (maybeScenes != null && maybeScenes.isArray()) {
                    sceneArray = maybeScenes;
                }
            }
            if (sceneArray == null || !sceneArray.isArray()) {
                return List.of();
            }

            List<String> descriptions = new ArrayList<>();
            for (JsonNode node : sceneArray) {
                String description = textAny(node, "visual", "description", "line", "text", "prompt");
                if (!StringUtils.hasText(description)) {
                    continue;
                }
                descriptions.add(sanitizePrompt(description));
            }
            return descriptions;
        } catch (JsonProcessingException ex) {
            log.warn("event=visual_scene_parse_failed reason=invalid_scene_breakdown_json message={}", ex.getMessage());
            return List.of();
        }
    }

    private String buildVisualPrompt(VideoJob job, ScenePlan plan) {
        List<String> parts = new ArrayList<>();
        parts.add("Cinematic vertical 9:16 short-form video frame");
        parts.add("Scene focus: " + plan.description());
        if (StringUtils.hasText(job.getTopic())) {
            parts.add("Topic context: " + sanitizePrompt(job.getTopic()));
        }
        if (StringUtils.hasText(job.getStoryAngle())) {
            parts.add("Story angle: " + sanitizePrompt(job.getStoryAngle()));
        }
        if (StringUtils.hasText(job.getStyle())) {
            parts.add("Visual tone for style: " + sanitizePrompt(job.getStyle()));
        }
        if (StringUtils.hasText(job.getProductPlacementMode())) {
            parts.add("Product placement: " + sanitizePrompt(job.getProductPlacementMode()));
        }
        parts.add("No text overlay, no watermark, high contrast subject, dynamic composition");
        return String.join(". ", parts) + ".";
    }

    private GeneratedVisualImage generateVisualSafely(String prompt, int sceneIndex) {
        try {
            return visualGenerationClient.generateSceneImage(prompt, sceneIndex);
        } catch (Exception ex) {
            log.warn(
                "event=visual_generation_fallback_triggered sceneIndex={} reason=provider_exception message={}",
                sceneIndex,
                ex.getMessage()
            );
            return deterministicPreviewVisualGenerator.generateFallbackImage(
                prompt,
                sceneIndex,
                "provider_exception",
                trimToLength(ex.getMessage(), 500)
            );
        }
    }

    private Map<String, Object> buildSceneAssetMetadata(
        ScenePlan plan,
        String prompt,
        GeneratedVisualImage image,
        String assetUrl
    ) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("index", plan.index() + 1);
        row.put("startSec", plan.startSec());
        row.put("endSec", plan.endSec());
        row.put("durationSec", Math.max(1, plan.endSec() - plan.startSec()));
        row.put("prompt", prompt);
        row.put("assetUrl", assetUrl);
        row.put("mode", image.getGenerationMode().name());
        row.put("provider", image.getProvider());
        row.put("modelId", image.getModelId());
        row.put("contentType", image.getContentType());
        row.put("failureReason", image.getFailureReason());
        return row;
    }

    private VisualGenerationMode resolveOverallMode(List<GeneratedVisualImage> generated) {
        if (generated.stream().anyMatch(item -> item.getGenerationMode() == VisualGenerationMode.FALLBACK)) {
            return VisualGenerationMode.FALLBACK;
        }
        if (generated.stream().anyMatch(item -> item.getGenerationMode() == VisualGenerationMode.MOCK)) {
            return VisualGenerationMode.MOCK;
        }
        return VisualGenerationMode.REAL;
    }

    private String resolveProvider(List<GeneratedVisualImage> generated) {
        return generated.stream()
            .map(GeneratedVisualImage::getProvider)
            .filter(StringUtils::hasText)
            .distinct()
            .reduce((left, right) -> left + "," + right)
            .orElse("unknown");
    }

    private String resolveModelId(List<GeneratedVisualImage> generated) {
        return generated.stream()
            .map(GeneratedVisualImage::getModelId)
            .filter(StringUtils::hasText)
            .findFirst()
            .orElse(null);
    }

    private String resolveFailureReason(List<GeneratedVisualImage> generated) {
        return generated.stream()
            .map(GeneratedVisualImage::getFailureReason)
            .filter(StringUtils::hasText)
            .findFirst()
            .orElse(null);
    }

    private String resolveFailureDetails(List<GeneratedVisualImage> generated) {
        return generated.stream()
            .map(GeneratedVisualImage::getFailureDetails)
            .filter(StringUtils::hasText)
            .findFirst()
            .orElse(null);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private String textAny(JsonNode node, String... fields) {
        if (node == null || node.isNull()) {
            return null;
        }
        for (String field : fields) {
            JsonNode child = node.get(field);
            if (child != null && !child.isNull()) {
                String text = child.isTextual() ? child.asText() : child.toString();
                if (StringUtils.hasText(text)) {
                    return text.trim();
                }
            }
        }
        if (node.isTextual()) {
            return node.asText();
        }
        return null;
    }

    private String sanitizePrompt(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text
            .replaceAll("[\\r\\n\\t]+", " ")
            .replaceAll("\\s+", " ")
            .replaceAll("[^\\p{L}\\p{N} ,.?!:'\\-]", " ")
            .trim();
    }

    private String buildObjectKey(UUID jobId, String fileName) {
        return "jobs/" + jobId + "/scenes/" + fileName;
    }

    private String trimToLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    public record GeneratedVisualAssets(
        List<SceneMediaSegment> sceneSegments,
        String sceneAssetsJson,
        VisualGenerationMode mode,
        String provider,
        String modelId,
        String failureReason,
        String failureDetails
    ) {
    }

    private record ScenePlan(int index, int startSec, int endSec, String description) {
    }
}
