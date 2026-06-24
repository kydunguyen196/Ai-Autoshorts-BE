package com.autoshorts.ai.service;

import com.autoshorts.ai.client.OpenAiClient;
import com.autoshorts.ai.client.model.OpenAiGenerationResult;
import com.autoshorts.ai.entity.CharacterCampaign;
import com.autoshorts.ai.entity.CharacterProfile;
import com.autoshorts.ai.entity.ContentGenerationMode;
import com.autoshorts.ai.entity.PromptTemplate;
import com.autoshorts.ai.entity.VideoJob;
import com.autoshorts.ai.repository.CharacterCampaignRepository;
import com.autoshorts.ai.repository.CharacterProfileRepository;
import com.autoshorts.ai.service.model.CharacterGenerationContext;
import com.autoshorts.ai.service.model.GeneratedContent;
import com.autoshorts.ai.service.model.PromptBuildResult;
import com.autoshorts.ai.util.SentenceUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ContentGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ContentGenerationService.class);

    private static final Set<String> STOCK_OPENERS = Set.of(
        "nobody tells you",
        "here are three lessons",
        "you need to hear this",
        "start now before it's too late"
    );

    private static final Map<String, List<String>> STYLE_HASHTAGS = Map.of(
        "motivation", List.of("#mindset", "#discipline", "#motivation", "#growth"),
        "storytelling", List.of("#storytelling", "#creator", "#narrative", "#life-lessons"),
        "facts", List.of("#facts", "#didyouknow", "#learning", "#insights"),
        "self-improvement", List.of("#selfimprovement", "#habits", "#productivity", "#growthmindset")
    );

    private static final Map<String, Integer> HOOK_BASE_SCORE = Map.of(
        "curiosity-gap", 82,
        "controversial-statement", 84,
        "emotional-hook", 80,
        "story-hook", 78,
        "problem-hook", 76
    );

    private final PromptTemplateService promptTemplateService;
    private final PromptBuilderService promptBuilderService;
    private final OpenAiClient openAiClient;
    private final CharacterProfileRepository characterProfileRepository;
    private final CharacterCampaignRepository characterCampaignRepository;
    private final ObjectMapper objectMapper;

    public ContentGenerationService(
        PromptTemplateService promptTemplateService,
        PromptBuilderService promptBuilderService,
        OpenAiClient openAiClient,
        CharacterProfileRepository characterProfileRepository,
        CharacterCampaignRepository characterCampaignRepository,
        ObjectMapper objectMapper
    ) {
        this.promptTemplateService = promptTemplateService;
        this.promptBuilderService = promptBuilderService;
        this.openAiClient = openAiClient;
        this.characterProfileRepository = characterProfileRepository;
        this.characterCampaignRepository = characterCampaignRepository;
        this.objectMapper = objectMapper;
    }

    public GeneratedContent generate(String topic, String requestedStyle, int durationSeconds) {
        return generate(topic, requestedStyle, durationSeconds, null);
    }

    public GeneratedContent generateForJob(VideoJob job) {
        CharacterGenerationContext context = buildCharacterContext(job);
        String commercialTopic = buildCommercialTopic(job);
        if (context == null || !context.hasAnyContext()) {
            return generate(commercialTopic, job.getStyle(), job.getDurationSeconds());
        }
        return generate(commercialTopic, job.getStyle(), job.getDurationSeconds(), context);
    }

    private String buildCommercialTopic(VideoJob job) {
        StringBuilder sb = new StringBuilder(job.getTopic());
        appendCommercialField(sb, "target niche", job.getNiche());
        appendCommercialField(sb, "platform", job.getPlatform());
        appendCommercialField(sb, "quality preset", job.getQualityPreset());
        appendCommercialField(sb, "subtitle style", job.getSubtitleStyle());
        appendCommercialField(sb, "visual mode", job.getVisualMode());
        appendCommercialField(sb, "voice persona", job.getVoicePersona());
        if ("affiliate".equalsIgnoreCase(nullSafe(job.getNiche()))
            || "viral-faceless".equalsIgnoreCase(nullSafe(job.getQualityPreset()))) {
            sb.append("\nCommercial goal: create a faceless short that earns attention first, then makes the offer or affiliate angle feel useful rather than pushy.");
            sb.append("\nRetention requirement: include a curiosity hook, three fast value beats, and a save/comment/follow CTA.");
        }
        return sb.toString();
    }

    private void appendCommercialField(StringBuilder sb, String label, String value) {
        if (StringUtils.hasText(value)) {
            sb.append("\n").append(label).append(": ").append(value.trim());
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    public GeneratedContent generate(
        String topic,
        String requestedStyle,
        int durationSeconds,
        CharacterGenerationContext characterContext
    ) {
        PromptTemplate template = promptTemplateService.resolveTemplate(requestedStyle);
        PromptBuildResult promptBuild = promptBuilderService.build(template, topic, durationSeconds, characterContext);

        String hookStrategy = canonicalHookStrategy(promptBuild.selectedHookStrategy());
        String ctaStrategy = canonicalCtaStrategy(promptBuild.selectedCtaStrategy());
        String structureStrategy = canonicalStructureStrategy(promptBuild.selectedStructureStrategy());

        log.info(
            "event=content_template_resolved style={} templateId={} variantKey={} hookType={} ctaType={} structureType={}",
            promptBuild.resolvedStyle(),
            promptBuild.templateId(),
            promptBuild.styleVariantKey(),
            hookStrategy,
            ctaStrategy,
            structureStrategy
        );

        OpenAiGenerationResult aiResult;
        try {
            aiResult = openAiClient.generateFromPrompts(promptBuild.systemPrompt(), promptBuild.userPrompt());
            log.info(
                "event=content_generation_source mode={} provider={} model={} style={} templateId={} variantKey={} hookType={} ctaType={} structureType={}",
                aiResult.mode(),
                aiResult.provider(),
                aiResult.model(),
                promptBuild.resolvedStyle(),
                promptBuild.templateId(),
                promptBuild.styleVariantKey(),
                hookStrategy,
                ctaStrategy,
                structureStrategy
            );
        } catch (Exception ex) {
            log.warn(
                "event=content_generation_fallback reason=provider_call_failed style={} templateId={} variantKey={} hookType={} ctaType={} structureType={} message={}",
                promptBuild.resolvedStyle(),
                promptBuild.templateId(),
                promptBuild.styleVariantKey(),
                hookStrategy,
                ctaStrategy,
                structureStrategy,
                ex.getMessage()
            );
            return fallbackContent(topic, durationSeconds, promptBuild, template, hookStrategy, ctaStrategy, structureStrategy, "provider_call_failed");
        }

        GeneratedContent generated = parseGeneratedContent(
            aiResult == null ? null : aiResult.content(),
            topic,
            durationSeconds,
            promptBuild,
            template,
            aiResult,
            hookStrategy,
            ctaStrategy,
            structureStrategy
        );

        log.info(
            "event=content_generation_ready mode={} style={} templateId={} variantKey={} hookType={} ctaType={} structureType={} hookScore={} engagementScore={}",
            generated.generationMode(),
            generated.resolvedStyle(),
            generated.promptTemplateId(),
            generated.styleVariantKey(),
            generated.hookStrategy(),
            generated.ctaStrategy(),
            generated.structureStrategy(),
            generated.hookStrengthScore(),
            generated.engagementScore()
        );
        return generated;
    }

    private CharacterGenerationContext buildCharacterContext(VideoJob job) {
        if (job == null) {
            return null;
        }
        CharacterProfile profile = null;
        CharacterCampaign campaign = null;
        if (job.getCharacterProfileId() != null) {
            profile = characterProfileRepository.findById(job.getCharacterProfileId()).orElse(null);
        }
        if (job.getCharacterCampaignId() != null) {
            campaign = characterCampaignRepository.findById(job.getCharacterCampaignId()).orElse(null);
        }

        return new CharacterGenerationContext(
            job.getCharacterProfileId(),
            profile == null ? null : profile.getName(),
            profile == null ? null : profile.getArchetype(),
            profile == null ? null : profile.getPersonality(),
            profile == null ? null : profile.getToneOfVoice(),
            profile == null ? null : profile.getSpeakingStyle(),
            profile == null ? null : profile.getCatchphrases(),
            profile == null ? null : profile.getVisualStyle(),
            profile == null ? null : profile.getLanguage(),
            profile == null ? null : profile.getTargetAudience(),
            profile == null ? null : profile.getAllowedTopics(),
            profile == null ? null : profile.getForbiddenTopics(),
            profile == null ? null : profile.getDefaultVoiceProvider(),
            profile == null ? null : profile.getDefaultVoiceId(),
            job.getCharacterCampaignId(),
            campaign == null ? null : campaign.getProductName(),
            campaign == null ? null : campaign.getProductType(),
            campaign == null ? null : campaign.getProductDescription(),
            campaign == null ? null : campaign.getProductUrl(),
            campaign == null ? null : campaign.getTargetPlatform(),
            campaign == null ? null : campaign.getCampaignObjective(),
            campaign == null ? null : campaign.getCallToAction(),
            campaign == null ? null : campaign.getTargetAudience(),
            campaign == null ? null : campaign.getOfferSummary(),
            job.getStoryAngle(),
            job.getProductPlacementMode(),
            job.getAdDisclosureMode(),
            job.getSceneCountTarget(),
            job.getCharacterConsistencyMode()
        );
    }

    private GeneratedContent parseGeneratedContent(
        String raw,
        String topic,
        int durationSeconds,
        PromptBuildResult promptBuild,
        PromptTemplate template,
        OpenAiGenerationResult aiResult,
        String defaultHookStrategy,
        String defaultCtaStrategy,
        String defaultStructureStrategy
    ) {
        JsonNode root = parseJson(raw);
        if (root == null || !root.isObject()) {
            return fallbackContent(topic, durationSeconds, promptBuild, template, defaultHookStrategy, defaultCtaStrategy, defaultStructureStrategy, "invalid_json");
        }

        boolean fallbackUsed = false;

        String hookStrategy = canonicalHookStrategy(firstNonBlank(textAny(root, "hookType", "hook_type", "hookStrategy"), defaultHookStrategy));
        String ctaStrategy = canonicalCtaStrategy(firstNonBlank(textAny(root, "ctaType", "cta_type", "ctaStrategy"), defaultCtaStrategy));
        String structureStrategy = canonicalStructureStrategy(firstNonBlank(textAny(root, "structureType", "structure_type", "structureStrategy"), defaultStructureStrategy));

        String rawHook = textAny(root, "hook", "hook_text");
        String hook = StringUtils.hasText(rawHook)
            ? sanitizeShortText(rawHook, 220)
            : buildHook(topic, hookStrategy);
        if (!StringUtils.hasText(rawHook) || containsStockOpener(hook)) {
            hook = buildHook(topic, hookStrategy);
            fallbackUsed = true;
        }

        String rawCta = textAny(root, "cta", "cta_text");
        String cta = StringUtils.hasText(rawCta)
            ? sanitizeShortText(rawCta, 200)
            : buildCta(topic, promptBuild.resolvedStyle(), structureStrategy, ctaStrategy, template.getTargetCtaStyle());
        fallbackUsed |= !StringUtils.hasText(rawCta);

        String rawScript = textAny(root, "script", "full_script");
        String script = StringUtils.hasText(rawScript)
            ? rawScript
            : buildFallbackScript(topic, promptBuild.resolvedStyle(), structureStrategy, hook, cta);
        fallbackUsed |= !StringUtils.hasText(rawScript);
        script = normalizeNarrationScript(script, hook, cta, durationSeconds, promptBuild.resolvedStyle(), structureStrategy);

        String rawCaption = textAny(root, "caption", "caption_text");
        String caption = StringUtils.hasText(rawCaption)
            ? sanitizeShortText(rawCaption, 240)
            : buildCaption(topic, hook, ctaStrategy);
        fallbackUsed |= !StringUtils.hasText(rawCaption);

        HashtagResult hashtagResult = readHashtags(root, promptBuild.resolvedStyle(), topic);
        List<String> hashtags = normalizeHashtags(hashtagResult.hashtags());
        fallbackUsed |= hashtagResult.usedFallback();

        SceneBreakdownResult sceneResult = readSceneBreakdownJson(root, script, durationSeconds, promptBuild.resolvedStyle(), structureStrategy);
        fallbackUsed |= sceneResult.usedFallback();

        int hookStrengthScore = scoreHookStrength(hook, hookStrategy);
        int engagementScore = scoreEngagement(script, cta, structureStrategy, ctaStrategy, hashtags.size());
        String engagementTagsJson = buildTagsJson(hookStrengthScore, engagementScore, hookStrategy, structureStrategy, ctaStrategy);

        ContentGenerationMode finalMode = fallbackUsed ? ContentGenerationMode.FALLBACK : aiResult.mode();
        if (fallbackUsed) {
            log.warn(
                "event=content_parse_partial_fallback sourceMode={} finalMode={} style={} templateId={} variantKey={} hookType={} ctaType={} structureType={}",
                aiResult.mode(),
                finalMode,
                promptBuild.resolvedStyle(),
                promptBuild.templateId(),
                promptBuild.styleVariantKey(),
                hookStrategy,
                ctaStrategy,
                structureStrategy
            );
        }

        return new GeneratedContent(
            promptBuild.templateId(),
            promptBuild.resolvedStyle(),
            promptBuild.styleVariantKey(),
            hookStrategy,
            ctaStrategy,
            structureStrategy,
            hookStrengthScore,
            engagementScore,
            engagementTagsJson,
            finalMode,
            hook,
            script,
            cta,
            caption,
            hashtags,
            sceneResult.sceneBreakdownJson()
        );
    }

    private GeneratedContent fallbackContent(
        String topic,
        int durationSeconds,
        PromptBuildResult promptBuild,
        PromptTemplate template,
        String hookStrategy,
        String ctaStrategy,
        String structureStrategy,
        String reason
    ) {
        String hook = buildHook(topic, hookStrategy);
        String cta = buildCta(topic, promptBuild.resolvedStyle(), structureStrategy, ctaStrategy, template.getTargetCtaStyle());
        String script = normalizeNarrationScript(
            buildFallbackScript(topic, promptBuild.resolvedStyle(), structureStrategy, hook, cta),
            hook,
            cta,
            durationSeconds,
            promptBuild.resolvedStyle(),
            structureStrategy
        );
        String caption = buildCaption(topic, hook, ctaStrategy);
        List<String> hashtags = normalizeHashtags(defaultHashtags(promptBuild.resolvedStyle(), topic));
        String sceneBreakdownJson = defaultSceneBreakdownJson(script, durationSeconds, promptBuild.resolvedStyle(), structureStrategy);

        int hookStrengthScore = scoreHookStrength(hook, hookStrategy);
        int engagementScore = scoreEngagement(script, cta, structureStrategy, ctaStrategy, hashtags.size());
        String engagementTagsJson = buildTagsJson(hookStrengthScore, engagementScore, hookStrategy, structureStrategy, ctaStrategy);

        log.warn(
            "event=content_fallback_applied reason={} mode={} style={} templateId={} variantKey={} hookType={} ctaType={} structureType={}",
            reason,
            ContentGenerationMode.FALLBACK,
            promptBuild.resolvedStyle(),
            promptBuild.templateId(),
            promptBuild.styleVariantKey(),
            hookStrategy,
            ctaStrategy,
            structureStrategy
        );

        return new GeneratedContent(
            promptBuild.templateId(),
            promptBuild.resolvedStyle(),
            promptBuild.styleVariantKey(),
            hookStrategy,
            ctaStrategy,
            structureStrategy,
            hookStrengthScore,
            engagementScore,
            engagementTagsJson,
            ContentGenerationMode.FALLBACK,
            hook,
            script,
            cta,
            caption,
            hashtags,
            sceneBreakdownJson
        );
    }

    private String buildHook(String topic, String hookStrategy) {
        String cleanTopic = sanitizeShortText(topic, 140);
        return switch (canonicalHookStrategy(hookStrategy)) {
            case "curiosity-gap" -> "What changes when you treat " + cleanTopic + " like a system, not a mood?";
            case "controversial-statement" -> "Most advice about " + cleanTopic + " is exactly why people stay stuck.";
            case "emotional-hook" -> "If " + cleanTopic + " has been draining your confidence, hear this.";
            case "story-hook" -> "A creator I know almost quit, then one shift in " + cleanTopic + " flipped everything.";
            case "problem-hook" -> "The problem with " + cleanTopic + " is not effort. It is hidden friction.";
            default -> "There is a smarter way to approach " + cleanTopic + ".";
        };
    }

    private String buildCta(String topic, String style, String structureStrategy, String ctaStrategy, String targetCtaStyle) {
        return switch (canonicalCtaStrategy(ctaStrategy)) {
            case "follow-cta" -> "Follow for more creator systems you can use this week.";
            case "save-cta" -> "Save this and test it in your next content sprint.";
            case "comment-cta" -> "Comment the part you will actually apply.";
            case "reflection-cta" -> "Which line felt most true for your current season?";
            default -> defaultCtaByContext(topic, style, structureStrategy, targetCtaStyle);
        };
    }

    private String defaultCtaByContext(String topic, String style, String structureStrategy, String targetCtaStyle) {
        String normalizedStyle = normalize(style);
        String normalizedStructure = canonicalStructureStrategy(structureStrategy);
        if (StringUtils.hasText(targetCtaStyle) && targetCtaStyle.toLowerCase(Locale.ROOT).contains("comment")) {
            return "Comment your takeaway so it becomes action.";
        }
        if ("storytelling".equals(normalizedStyle) || "narrative-lesson".equals(normalizedStructure)) {
            return "Comment if you want more story-led creator breakdowns.";
        }
        if ("facts".equals(normalizedStyle)) {
            return "Save this for your next research-backed short.";
        }
        return "Follow for more ideas on " + sanitizeShortText(topic, 70) + ".";
    }

    private String buildFallbackScript(String topic, String style, String structureStrategy, String hook, String cta) {
        String styleLine = styleSpecificLine(style, topic);
        return switch (canonicalStructureStrategy(structureStrategy)) {
            case "mini-story" -> joinLines(
                hook,
                "Setup: a creator kept delaying uploads because every draft had to feel perfect.",
                "Conflict: perfection killed momentum and confidence.",
                "Resolution: one tiny daily publish target brought consistency back.",
                styleLine,
                cta
            );
            case "problem-solution-insight" -> joinLines(
                hook,
                "Problem: most people push hard for two days, then disappear.",
                "Solution: define a minimum action you can do even on low-energy days.",
                "Insight: identity follows repeated actions, not motivation spikes.",
                styleLine,
                cta
            );
            case "narrative-lesson" -> joinLines(
                hook,
                "At first it looked like slow progress and constant self-doubt.",
                "Then one routine change made execution automatic.",
                "The lesson is simple: systems quietly beat intensity.",
                styleLine,
                cta
            );
            default -> joinLines(
                hook,
                "List-style content still works when you add real transitions and context.",
                "Start with one measurable action tied to your existing workflow.",
                "Then review weekly and adjust one variable at a time.",
                styleLine,
                cta
            );
        };
    }

    private String styleSpecificLine(String style, String topic) {
        return switch (normalize(style)) {
            case "storytelling" -> "The story changes when the behavior changes.";
            case "facts" -> "Across high-performing creators, this pattern repeats consistently.";
            case "self-improvement" -> "Make it practical enough to repeat when life gets noisy.";
            default -> "Momentum compounds when your system survives imperfect days.";
        };
    }

    private String buildCaption(String topic, String hook, String ctaStrategy) {
        String suffix = switch (canonicalCtaStrategy(ctaStrategy)) {
            case "save-cta" -> "Save this for your next planning session.";
            case "comment-cta" -> "Drop your takeaway below.";
            case "reflection-cta" -> "Pause and reflect before your next upload.";
            default -> "Follow for more short-form systems.";
        };
        return sanitizeShortText(sanitizeShortText(hook, 120) + " " + sanitizeShortText(topic, 80) + ". " + suffix, 230);
    }

    private String normalizeNarrationScript(String script, String hook, String cta, int durationSeconds, String style, String structureStrategy) {
        String normalized = StringUtils.hasText(script) ? script.replaceAll("\\s+", " ").trim() : "";
        if (!StringUtils.hasText(normalized)) {
            normalized = hook;
        }

        normalized = softenMechanicalPhrasing(normalized)
            .replaceAll("(?i)\\bfirst,?\\b", "Start here,")
            .replaceAll("(?i)\\bsecond,?\\b", "Then,")
            .replaceAll("(?i)\\bthird,?\\b", "After that,");

        List<String> deduped = dedupeSentences(SentenceUtils.splitIntoSubtitleSegments(normalized));
        if (deduped.isEmpty()) {
            deduped.add(hook);
        }
        String stitched = stitchWithTransitions(varySentenceLengths(deduped), structureStrategy);

        if (!stitched.toLowerCase(Locale.ROOT).startsWith(hook.toLowerCase(Locale.ROOT))) {
            stitched = hook + " " + stitched;
        }

        int minWords = Math.max(48, durationSeconds * 2);
        int maxWords = Math.max(78, durationSeconds * 3);
        if (wordCount(stitched) < minWords) {
            stitched = stitched + " " + styleSpecificLine(style, "your current focus");
        }
        stitched = trimToWordLimit(stitched, maxWords);

        if (StringUtils.hasText(cta) && !stitched.toLowerCase(Locale.ROOT).contains(cta.toLowerCase(Locale.ROOT))) {
            stitched = stitched + " " + cta;
        }

        return stitched.replaceAll("\\s+", " ").trim();
    }

    private List<String> dedupeSentences(List<String> sentences) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String sentence : sentences) {
            String key = sentence.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", "").trim();
            if (!StringUtils.hasText(key) || seen.contains(key)) {
                continue;
            }
            result.add(sentence.trim());
            seen.add(key);
        }
        return result;
    }

    private List<String> varySentenceLengths(List<String> sentences) {
        List<String> result = new ArrayList<>();
        for (String sentence : sentences) {
            String clean = sentence.replaceAll("\\s+", " ").trim();
            if (!StringUtils.hasText(clean)) {
                continue;
            }
            if (wordCount(clean) > 22 && ThreadLocalRandom.current().nextDouble() < 0.4) {
                int pivot = clean.indexOf(',');
                if (pivot > 8 && pivot < clean.length() - 8) {
                    result.add(clean.substring(0, pivot).trim() + ".");
                    result.add(clean.substring(pivot + 1).trim());
                    continue;
                }
            }
            if (wordCount(clean) < 7 && !result.isEmpty() && ThreadLocalRandom.current().nextDouble() < 0.3) {
                clean = "And honestly, " + Character.toLowerCase(clean.charAt(0)) + clean.substring(1);
            }
            result.add(clean);
        }
        return result.isEmpty() ? sentences : result;
    }

    private String stitchWithTransitions(List<String> sentences, String structureStrategy) {
        List<String> transitions = switch (canonicalStructureStrategy(structureStrategy)) {
            case "mini-story" -> List.of("Then", "But here's the turn", "After that", "By the end");
            case "problem-solution-insight" -> List.of("So", "That is why", "Instead", "The insight");
            case "narrative-lesson" -> List.of("Then", "The shift", "What changed", "The lesson");
            default -> List.of("Then", "From there", "Next", "Finally");
        };

        List<String> stitched = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String current = sentences.get(i).trim();
            if (i > 0 && ThreadLocalRandom.current().nextDouble() < 0.35 && !startsWithTransition(current)) {
                current = transitions.get((i - 1) % transitions.size()) + ", " + Character.toLowerCase(current.charAt(0)) + current.substring(1);
            }
            stitched.add(current);
        }
        return String.join(" ", stitched).replaceAll("\\s+", " ").trim();
    }

    private boolean startsWithTransition(String sentence) {
        String lower = sentence.toLowerCase(Locale.ROOT);
        return lower.startsWith("then") || lower.startsWith("so") || lower.startsWith("but") || lower.startsWith("next") || lower.startsWith("after") || lower.startsWith("finally");
    }

    private String softenMechanicalPhrasing(String text) {
        String out = text;
        for (String opener : STOCK_OPENERS) {
            out = out.replaceAll("(?i)" + Pattern.quote(opener), "");
        }
        return out.replaceAll("\\s+", " ").trim();
    }

    private boolean containsStockOpener(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return STOCK_OPENERS.stream().anyMatch(lower::contains);
    }

    private int scoreHookStrength(String hook, String hookStrategy) {
        int score = HOOK_BASE_SCORE.getOrDefault(canonicalHookStrategy(hookStrategy), 72);
        int words = wordCount(hook);
        score += (words >= 7 && words <= 18) ? 8 : -8;
        if (hook != null && hook.contains("?")) {
            score += 4;
        }
        if (containsStockOpener(hook)) {
            score -= 18;
        }
        return Math.max(0, Math.min(100, score));
    }

    private int scoreEngagement(String script, String cta, String structureStrategy, String ctaStrategy, int hashtagCount) {
        int score = 60;
        score += switch (canonicalStructureStrategy(structureStrategy)) {
            case "mini-story" -> 12;
            case "problem-solution-insight" -> 11;
            case "narrative-lesson" -> 10;
            default -> 8;
        };
        score += switch (canonicalCtaStrategy(ctaStrategy)) {
            case "reflection-cta" -> 8;
            case "comment-cta" -> 7;
            case "save-cta" -> 6;
            default -> 5;
        };

        int words = wordCount(script);
        score += (words >= 55 && words <= 125) ? 8 : -6;
        score += (hashtagCount >= 4 && hashtagCount <= 8) ? 4 : 0;
        score += (StringUtils.hasText(cta) && wordCount(cta) >= 5 && wordCount(cta) <= 16) ? 4 : 0;

        String lower = script == null ? "" : script.toLowerCase(Locale.ROOT);
        if (lower.contains("first,") && lower.contains("second,") && lower.contains("third,")) {
            score -= 10;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String buildTagsJson(int hookStrengthScore, int engagementScore, String hookStrategy, String structureStrategy, String ctaStrategy) {
        Map<String, Object> tags = new LinkedHashMap<>();
        tags.put("hookStrength", hookStrengthScore >= 80 ? "high" : hookStrengthScore >= 60 ? "medium" : "low");
        tags.put("engagementPotential", engagementScore >= 80 ? "high" : engagementScore >= 60 ? "medium" : "low");
        tags.put("tags", List.of(
            "hook:" + canonicalHookStrategy(hookStrategy),
            "structure:" + canonicalStructureStrategy(structureStrategy),
            "cta:" + canonicalCtaStrategy(ctaStrategy)
        ));
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private HashtagResult readHashtags(JsonNode root, String style, String topic) {
        JsonNode hashtagsNode = root.get("hashtags");
        if (hashtagsNode == null || hashtagsNode.isNull()) {
            hashtagsNode = root.get("hash_tags");
        }
        if (hashtagsNode == null || hashtagsNode.isNull()) {
            return new HashtagResult(defaultHashtags(style, topic), true);
        }

        List<String> hashtags = new ArrayList<>();
        if (hashtagsNode.isArray()) {
            hashtagsNode.forEach(node -> {
                if (node != null && node.isTextual()) {
                    hashtags.add(node.asText());
                }
            });
        } else if (hashtagsNode.isTextual()) {
            for (String token : hashtagsNode.asText().split("[,\\s]+")) {
                if (!token.isBlank()) {
                    hashtags.add(token);
                }
            }
        }

        if (hashtags.isEmpty()) {
            return new HashtagResult(defaultHashtags(style, topic), true);
        }
        return new HashtagResult(hashtags, false);
    }

    private SceneBreakdownResult readSceneBreakdownJson(JsonNode root, String script, int durationSeconds, String style, String structureStrategy) {
        JsonNode sceneNode = root.get("sceneBreakdown");
        if (sceneNode == null || sceneNode.isNull()) {
            sceneNode = root.get("scene_breakdown");
        }
        if (sceneNode != null && sceneNode.isObject()) {
            JsonNode scenes = sceneNode.get("scenes");
            if (scenes != null && scenes.isArray()) {
                sceneNode = scenes;
            }
        }
        if (sceneNode == null || sceneNode.isNull() || (sceneNode.isArray() && sceneNode.isEmpty())) {
            return new SceneBreakdownResult(defaultSceneBreakdownJson(script, durationSeconds, style, structureStrategy), true);
        }
        try {
            return new SceneBreakdownResult(objectMapper.writeValueAsString(sceneNode), false);
        } catch (JsonProcessingException ex) {
            return new SceneBreakdownResult(defaultSceneBreakdownJson(script, durationSeconds, style, structureStrategy), true);
        }
    }

    private String defaultSceneBreakdownJson(String script, int durationSeconds, String style, String structureStrategy) {
        List<String> segments = SentenceUtils.splitIntoSubtitleSegments(script);
        if (segments.isEmpty()) {
            segments = List.of(script);
        }

        List<String> visuals = switch (canonicalStructureStrategy(structureStrategy)) {
            case "mini-story" -> List.of("setup moment", "conflict beat", "turning point", "resolution", "lesson close");
            case "problem-solution-insight" -> List.of("problem visual", "friction highlight", "solution action", "insight reveal", "closing prompt");
            case "narrative-lesson" -> List.of("opening scene", "pivot beat", "lesson frame", "application frame");
            default -> "facts".equals(normalize(style))
                ? List.of("curiosity hook overlay", "supporting fact visual", "proof card", "key takeaway frame")
                : List.of("hook overlay", "supporting b-roll", "insight frame", "closing beat");
        };

        int slice = Math.max(2, durationSeconds / Math.max(1, segments.size()));
        int cursor = 0;
        List<Map<String, Object>> scenes = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            int start = cursor;
            int end = (i == segments.size() - 1) ? durationSeconds : Math.min(durationSeconds, start + slice);
            cursor = end;

            Map<String, Object> scene = new LinkedHashMap<>();
            scene.put("startSec", start);
            scene.put("endSec", end);
            scene.put("visual", visuals.get(Math.min(i, visuals.size() - 1)));
            scene.put("line", segments.get(i));
            scenes.add(scene);
        }
        try {
            return objectMapper.writeValueAsString(scenes);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private List<String> defaultHashtags(String style, String topic) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add("#shorts");
        tags.add("#aivideo");
        tags.add("#autoshorts");
        tags.addAll(STYLE_HASHTAGS.getOrDefault(normalize(style), List.of("#creator", "#contentstrategy")));
        for (String token : topic.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (token.length() >= 4) {
                tags.add("#" + token);
            }
            if (tags.size() >= 8) {
                break;
            }
        }
        return new ArrayList<>(tags);
    }

    private List<String> normalizeHashtags(List<String> hashtags) {
        if (hashtags == null) {
            return List.of("#shorts", "#aivideo");
        }
        LinkedHashSet<String> deduped = new LinkedHashSet<>();
        for (String hashtag : hashtags) {
            if (!StringUtils.hasText(hashtag)) {
                continue;
            }
            String clean = hashtag.trim();
            if (!clean.startsWith("#")) {
                clean = "#" + clean;
            }
            clean = clean.replaceAll("[^#a-zA-Z0-9_\\-]", "").toLowerCase(Locale.ROOT);
            if (clean.length() > 1) {
                deduped.add(clean);
            }
            if (deduped.size() >= 8) {
                break;
            }
        }
        if (deduped.isEmpty()) {
            return List.of("#shorts", "#aivideo");
        }
        return new ArrayList<>(deduped);
    }

    private JsonNode parseJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readTree(cleanJsonCandidate(raw));
        } catch (JsonProcessingException ex) {
            int firstBrace = raw.indexOf('{');
            int lastBrace = raw.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                try {
                    return objectMapper.readTree(cleanJsonCandidate(raw.substring(firstBrace, lastBrace + 1)));
                } catch (JsonProcessingException ignored) {
                    return null;
                }
            }
            return null;
        }
    }

    private String textAny(JsonNode root, String... fields) {
        for (String field : fields) {
            JsonNode node = root.get(field);
            if (node != null && !node.isNull()) {
                String value = node.isTextual() ? node.asText() : node.toString();
                if (StringUtils.hasText(value)) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    private String cleanJsonCandidate(String raw) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-zA-Z0-9]*\\s*", "");
            cleaned = cleaned.replaceAll("\\s*```$", "");
        }
        if (cleaned.startsWith("\uFEFF")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned.trim();
    }

    private String joinLines(String... lines) {
        return Arrays.stream(lines)
            .filter(StringUtils::hasText)
            .map(String::trim)
            .collect(Collectors.joining(" "))
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String sanitizeShortText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String clean = value.replaceAll("\\s+", " ").trim();
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength).trim();
    }

    private int wordCount(String text) {
        return StringUtils.hasText(text) ? text.trim().split("\\s+").length : 0;
    }

    private String trimToWordLimit(String text, int maxWords) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String[] words = text.trim().split("\\s+");
        if (words.length <= maxWords) {
            return text.trim();
        }
        return Arrays.stream(words).limit(maxWords).collect(Collectors.joining(" "));
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "default";
        }
        return value.trim().toLowerCase(Locale.ROOT)
            .replace('_', '-')
            .replace(' ', '-')
            .replace('+', '-')
            .replace('/', '-');
    }

    private String canonicalHookStrategy(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "question" -> "curiosity-gap";
            case "bold-statement", "contrarian" -> "controversial-statement";
            case "story-intro" -> "story-hook";
            case "lesson-intro", "insight-hook" -> "problem-hook";
            case "curiosity-gap", "controversial-statement", "emotional-hook", "story-hook", "problem-hook" -> normalized;
            default -> "curiosity-gap";
        };
    }

    private String canonicalCtaStrategy(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "follow" -> "follow-cta";
            case "save" -> "save-cta";
            case "comment" -> "comment-cta";
            case "reflective", "no-cta" -> "reflection-cta";
            case "follow-cta", "save-cta", "comment-cta", "reflection-cta" -> normalized;
            default -> "follow-cta";
        };
    }

    private String canonicalStructureStrategy(String value) {
        String normalized = normalize(value);
        if (normalized.contains("narrative") && normalized.contains("lesson")) {
            return "narrative-lesson";
        }
        return switch (normalized) {
            case "list-based" -> "list-with-story-transitions";
            case "insight-based" -> "narrative-lesson";
            case "problem-solution" -> "problem-solution-insight";
            case "mini-story", "problem-solution-insight", "narrative-lesson", "list-with-story-transitions" -> normalized;
            default -> "mini-story";
        };
    }

    private String firstNonBlank(String first, String fallback) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return fallback;
    }

    private record HashtagResult(List<String> hashtags, boolean usedFallback) {
    }

    private record SceneBreakdownResult(String sceneBreakdownJson, boolean usedFallback) {
    }
}
