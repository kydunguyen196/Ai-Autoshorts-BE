package com.autoshorts.ai.service;

import com.autoshorts.ai.entity.PromptTemplate;
import com.autoshorts.ai.service.model.CharacterGenerationContext;
import com.autoshorts.ai.service.model.PromptBuildResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PromptBuilderService {

    private static final List<String> DEFAULT_HOOK_STRATEGIES = List.of(
        "curiosity-gap",
        "controversial-statement",
        "emotional-hook",
        "story-hook",
        "problem-hook"
    );

    private static final List<String> DEFAULT_CTA_STRATEGIES = List.of(
        "follow-cta",
        "save-cta",
        "comment-cta",
        "reflection-cta"
    );

    private static final List<String> DEFAULT_STRUCTURE_STRATEGIES = List.of(
        "mini-story",
        "problem-solution-insight",
        "narrative-lesson",
        "list-with-story-transitions"
    );

    private static final Map<String, StylePlaybook> STYLE_PLAYBOOKS = Map.of(
        "motivation",
        new StylePlaybook(
            "high conviction, grounded urgency, no empty hype",
            "snap hook, quick momentum build, strong close",
            "lead with friction, urgency, or a hard truth in line one",
            "prefer follow/comment when momentum is high; save when content is tactical",
            "short spoken lines mixed with one longer payoff sentence",
            "quick cuts of progress moments, failures, and recovery beat"
        ),
        "storytelling",
        new StylePlaybook(
            "intimate, vivid, emotionally believable",
            "setup fast, hold tension briefly, close with clear payoff",
            "open on a specific moment rather than abstract advice",
            "prefer reflection/comment CTA tied to the lesson",
            "natural conversational cadence with occasional imperfect fragments",
            "scene progression: setup, conflict beat, pivot, lesson close"
        ),
        "facts",
        new StylePlaybook(
            "credible, curious, sharp",
            "fast information beats with clean transitions",
            "open with surprising fact tension or a curiosity gap",
            "prefer save/follow CTA, concise and non-promotional",
            "precise phrasing, tight claims, low adjective noise",
            "visual labels, overlays, timeline cards, source-style framing"
        ),
        "self-improvement",
        new StylePlaybook(
            "practical, honest, human",
            "steady pace, one practical beat at a time",
            "open with a real struggle or common behavior trap",
            "prefer save/reflection CTA when the advice is actionable",
            "spoken-first phrasing with concrete implementation language",
            "habit trigger visuals, checklist beats, progress checkpoints"
        )
    );

    private final ObjectMapper objectMapper;

    public PromptBuilderService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PromptBuildResult build(PromptTemplate template, String topic, int durationSeconds) {
        return build(template, topic, durationSeconds, null);
    }

    public PromptBuildResult build(
        PromptTemplate template,
        String topic,
        int durationSeconds,
        CharacterGenerationContext characterContext
    ) {
        String resolvedStyle = normalizeKey(template.getStyleKey());
        StylePlaybook playbook = STYLE_PLAYBOOKS.getOrDefault(
            resolvedStyle,
            new StylePlaybook(
                "clear and human",
                "balanced pace with a decisive ending",
                "open with a concrete hook line",
                "CTA should match value type and feel earned",
                "natural spoken rhythm with varied sentence length",
                "simple scene arc with visual progression"
            )
        );

        List<String> hookStrategies = mergeStrategies(
            parseStrategyJson(template.getHookPatternsJson()),
            DEFAULT_HOOK_STRATEGIES
        );
        List<String> ctaStrategies = mergeStrategies(
            parseStrategyJson(template.getCtaPatternsJson()),
            DEFAULT_CTA_STRATEGIES
        );
        List<String> structureStrategies = mergeStrategies(
            parseStrategyJson(template.getStructurePatternsJson()),
            DEFAULT_STRUCTURE_STRATEGIES
        );

        String selectedHookStrategy = canonicalHookStrategy(randomPick(hookStrategies, "curiosity-gap"));
        String selectedStructureStrategy = canonicalStructureStrategy(randomPick(structureStrategies, "mini-story"));
        String selectedCtaStrategy = canonicalCtaStrategy(
            selectCtaStrategy(resolvedStyle, selectedStructureStrategy, ctaStrategies)
        );

        String styleVariantKey = ("%s|hook=%s|cta=%s|structure=%s")
            .formatted(
                resolvedStyle,
                selectedHookStrategy,
                selectedCtaStrategy,
                selectedStructureStrategy
            );

        String userPrompt = template.getUserPromptPattern()
            .replace("{{topic}}", topic)
            .replace("{{style}}", template.getStyleKey())
            .replace("{{durationSeconds}}", String.valueOf(durationSeconds))
            .replace("{{targetTone}}", template.getTargetTone())
            .replace("{{targetCtaStyle}}", template.getTargetCtaStyle())
            .replace("{{hookPattern}}", selectedHookStrategy)
            .replace("{{ctaPattern}}", selectedCtaStrategy)
            .replace("{{structurePattern}}", selectedStructureStrategy);

        String styleGuidance = """

            Viral intent guidance for this generation:
            - Tone: %s
            - Pacing: %s
            - Hook behavior: %s
            - CTA behavior: %s
            - Sentence style: %s
            - Optional scene guidance: %s
            """.formatted(
            playbook.tone(),
            playbook.pacing(),
            playbook.hookBehavior(),
            playbook.ctaBehavior(),
            playbook.sentenceStyle(),
            playbook.sceneGuidance()
        );

        String variationGuidance = """

            Strategy lock for this generation:
            - Hook strategy: %s
            - Structure strategy: %s
            - CTA strategy: %s

            Diversity and anti-repetition rules:
            - Do not use stock openers like "Nobody tells you", "Here are three lessons", "You need to hear this".
            - Avoid repeating opening templates across requests.
            - Keep phrasing topic-specific and concrete.
            - Prefer one fresh line over three generic lines.
            """.formatted(selectedHookStrategy, selectedStructureStrategy, selectedCtaStrategy);

        String humanizationGuidance = """

            Human delivery rules:
            - Write for spoken delivery with varied sentence lengths.
            - Include natural transitions and occasional imperfect phrasing.
            - Avoid robotic rhythm or strict textbook structure.
            - Keep emotional cadence believable and creator-native.
            """;

        String characterGuidance = buildCharacterContextGuidance(characterContext);

        String jsonContract = """

            Return strictly valid JSON (no markdown) with this shape:
            {
              "hook": "string",
              "script": "string",
              "cta": "string",
              "caption": "string",
              "hashtags": ["#tag1", "#tag2"],
              "hookType": "string",
              "structureType": "string",
              "ctaType": "string",
              "sceneBreakdown": [
                {"startSec": 0, "endSec": 5, "visual": "string", "line": "string"}
              ]
            }

            Output constraints:
            - hook: 7-18 words, high-retention, no emojis.
            - script: 55-125 words, natural cadence for 20-45 seconds.
            - cta: concise and aligned with CTA strategy.
            - caption: concise social copy, not a script duplicate.
            - hashtags: 4-8 relevant tags, prefixed with #, no duplicates.
            - sceneBreakdown: 3-6 scenes preferred.
            - hookType/structureType/ctaType should match selected strategies.
            """;

        return new PromptBuildResult(
            template.getId(),
            resolvedStyle,
            styleVariantKey,
            selectedHookStrategy,
            selectedCtaStrategy,
            selectedStructureStrategy,
            template.getSystemPrompt(),
            userPrompt + styleGuidance + variationGuidance + humanizationGuidance + characterGuidance + jsonContract
        );
    }

    private String buildCharacterContextGuidance(CharacterGenerationContext context) {
        if (context == null || !context.hasAnyContext()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\nCharacter Story Ad guidance (optional mode enabled):\n");
        appendLine(sb, "Character profile id", context.characterProfileId());
        appendLine(sb, "Character campaign id", context.characterCampaignId());
        appendLine(sb, "Character name", context.characterName());
        appendLine(sb, "Archetype", context.characterArchetype());
        appendLine(sb, "Personality", context.personality());
        appendLine(sb, "Tone of voice", context.toneOfVoice());
        appendLine(sb, "Speaking style", context.speakingStyle());
        appendLine(sb, "Catchphrases", context.catchphrases());
        appendLine(sb, "Visual style", context.visualStyle());
        appendLine(sb, "Language", context.language());
        appendLine(sb, "Allowed topics", context.allowedTopics());
        appendLine(sb, "Forbidden topics", context.forbiddenTopics());
        appendLine(sb, "Profile target audience", context.profileTargetAudience());
        appendLine(sb, "Campaign target audience", context.campaignTargetAudience());
        appendLine(sb, "Product name", context.productName());
        appendLine(sb, "Product type", context.productType());
        appendLine(sb, "Product description", context.productDescription());
        appendLine(sb, "Product URL", context.productUrl());
        appendLine(sb, "Target platform", context.targetPlatform());
        appendLine(sb, "Campaign objective", context.campaignObjective());
        appendLine(sb, "Campaign CTA", context.campaignCallToAction());
        appendLine(sb, "Offer summary", context.offerSummary());
        appendLine(sb, "Story angle", context.storyAngle());
        appendLine(sb, "Product placement mode", context.productPlacementMode());
        appendLine(sb, "Ad disclosure mode", context.adDisclosureMode());
        appendLine(sb, "Scene count target", context.sceneCountTarget());
        appendLine(sb, "Character consistency mode", context.characterConsistencyMode());

        sb.append("""
            - Keep hook/script/cta aligned to the character persona and campaign objective.
            - Maintain voice consistency with persona tone and speaking style.
            - If ad disclosure mode is provided, include compliant wording in caption or CTA.
            - Keep product placement natural and story-led, not abrupt.
            """);
        return sb.toString();
    }

    private List<String> parseStrategyJson(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray()) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            root.forEach(node -> {
                if (node != null && node.isTextual() && StringUtils.hasText(node.asText())) {
                    values.add(normalizeKey(node.asText()));
                }
            });
            return values;
        } catch (Exception ignored) {
            List<String> fallback = new ArrayList<>();
            for (String token : rawJson.split(",")) {
                if (StringUtils.hasText(token)) {
                    fallback.add(normalizeKey(token));
                }
            }
            return fallback;
        }
    }

    private List<String> mergeStrategies(List<String> preferred, List<String> defaults) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (preferred != null) {
            preferred.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeKey)
                .forEach(merged::add);
        }
        if (merged.isEmpty()) {
            defaults.stream().map(this::normalizeKey).forEach(merged::add);
        }
        return List.copyOf(merged);
    }

    private String selectCtaStrategy(String style, String structure, List<String> availableStrategies) {
        List<String> available = availableStrategies.stream()
            .filter(StringUtils::hasText)
            .map(this::canonicalCtaStrategy)
            .distinct()
            .toList();

        List<String> preferred = new ArrayList<>();

        String normalizedStyle = normalizeKey(style);
        String normalizedStructure = normalizeKey(structure);

        if ("storytelling".equals(normalizedStyle)
            || "mini-story".equals(normalizedStructure)
            || "narrative-lesson".equals(normalizedStructure)) {
            preferred.add("reflection-cta");
            preferred.add("comment-cta");
        }

        if ("facts".equals(normalizedStyle) || "list-with-story-transitions".equals(normalizedStructure)) {
            preferred.add("save-cta");
            preferred.add("follow-cta");
        }

        if ("self-improvement".equals(normalizedStyle) || "problem-solution-insight".equals(normalizedStructure)) {
            preferred.add("save-cta");
            preferred.add("reflection-cta");
        }

        if ("motivation".equals(normalizedStyle)) {
            preferred.add("follow-cta");
            preferred.add("comment-cta");
        }

        if (preferred.isEmpty()) {
            preferred.add("follow-cta");
            preferred.add("save-cta");
            preferred.add("comment-cta");
            preferred.add("reflection-cta");
        }

        List<String> candidates = new ArrayList<>();
        for (String item : preferred) {
            if (available.contains(item)) {
                candidates.add(item);
            }
        }

        if (candidates.isEmpty()) {
            return randomPick(available, "follow-cta");
        }

        return randomPick(candidates, "follow-cta");
    }

    private String randomPick(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        int index = ThreadLocalRandom.current().nextInt(values.size());
        return values.get(index);
    }

    private String normalizeKey(String value) {
        if (!StringUtils.hasText(value)) {
            return "default";
        }
        return value.trim()
            .toLowerCase(Locale.ROOT)
            .replace('_', '-')
            .replace(' ', '-');
    }

    private String canonicalHookStrategy(String value) {
        String normalized = normalizeKey(value);
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
        String normalized = normalizeKey(value);
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
        String normalized = normalizeKey(value);
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

    private void appendLine(StringBuilder sb, String label, Object value) {
        if (value == null) {
            return;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return;
        }
        sb.append("- ").append(label).append(": ").append(text).append('\n');
    }

    private record StylePlaybook(
        String tone,
        String pacing,
        String hookBehavior,
        String ctaBehavior,
        String sentenceStyle,
        String sceneGuidance
    ) {
    }
}
