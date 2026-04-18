package com.autoshorts.ai.client.impl;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MockGenerationSupport {

    private static final List<String> DEFAULT_HOOK_STRATEGIES = List.of(
        "curiosity-gap",
        "controversial-statement",
        "emotional-hook",
        "story-hook",
        "problem-hook"
    );

    private static final List<String> DEFAULT_STRUCTURE_STRATEGIES = List.of(
        "mini-story",
        "problem-solution-insight",
        "narrative-lesson",
        "list-with-story-transitions"
    );

    private static final List<String> DEFAULT_CTA_STRATEGIES = List.of(
        "follow-cta",
        "save-cta",
        "comment-cta",
        "reflection-cta"
    );

    private MockGenerationSupport() {
    }

    static String script() {
        return """
            You can spot the creators who grow fast: they stop waiting to feel ready.
            One creator switched from chasing motivation to a tiny non-negotiable daily output.
            The result was less stress, better ideas, and steady momentum in two weeks.
            Save this and run it for seven days before you change anything else.
            """.replaceAll("\\s+", " ").trim();
    }

    static String contentJsonFromPrompt(String userPrompt) {
        String topic = firstNonBlank(extractFieldAny(userPrompt, "Topic"), "How creators build consistency when motivation disappears");
        String style = normalize(firstNonBlank(extractFieldAny(userPrompt, "Style"), "motivation"));
        String duration = firstNonBlank(extractFieldAny(userPrompt, "Duration"), "30");
        String hookStrategy = normalize(firstNonBlank(
            extractFieldAny(userPrompt, "Hook strategy", "Selected hook strategy", "Hook pattern"),
            randomPick(DEFAULT_HOOK_STRATEGIES)
        ));
        String ctaStrategy = normalize(firstNonBlank(
            extractFieldAny(userPrompt, "CTA strategy", "Selected CTA strategy", "CTA pattern"),
            randomPick(DEFAULT_CTA_STRATEGIES)
        ));
        String structureStrategy = normalize(firstNonBlank(
            extractFieldAny(userPrompt, "Structure strategy", "Selected structure strategy", "Structure pattern"),
            randomPick(DEFAULT_STRUCTURE_STRATEGIES)
        ));

        int durationSeconds = parseDuration(duration);
        String hook = buildHook(topic, hookStrategy);
        String cta = buildCta(style, structureStrategy, ctaStrategy);
        String generatedScript = buildScript(topic, style, structureStrategy, hook, cta);
        String caption = buildCaption(topic, hook, ctaStrategy);
        List<String> hashtags = buildHashtags(style, topic);

        return """
            {
              "hook": "%s",
              "script": "%s",
              "cta": "%s",
              "caption": "%s",
              "hashtags": [%s],
              "hookType": "%s",
              "structureType": "%s",
              "ctaType": "%s",
              "sceneBreakdown": [
                {"startSec": 0, "endSec": %d, "visual": "hook moment", "line": "%s"},
                {"startSec": %d, "endSec": %d, "visual": "narrative progression", "line": "Core idea"},
                {"startSec": %d, "endSec": %d, "visual": "closing beat", "line": "%s"}
              ]
            }
            """.formatted(
            escapeJson(hook),
            escapeJson(generatedScript),
            escapeJson(firstNonBlank(cta, "")),
            escapeJson(caption),
            hashtags.stream().map(tag -> "\"" + escapeJson(tag) + "\"").reduce((a, b) -> a + ", " + b).orElse("\"#shorts\""),
            escapeJson(hookStrategy),
            escapeJson(structureStrategy),
            escapeJson(ctaStrategy),
            Math.max(4, durationSeconds / 5),
            escapeJson(hook),
            Math.max(4, durationSeconds / 5),
            Math.max(8, durationSeconds - Math.max(4, durationSeconds / 5)),
            Math.max(8, durationSeconds - Math.max(4, durationSeconds / 5)),
            durationSeconds,
            escapeJson(String.valueOf(firstNonBlank(cta, "")))
        );
    }

    static byte[] sineWaveWav(int seconds, int sampleRate, double frequency) {
        int totalSamples = seconds * sampleRate;
        int bytesPerSample = 2;
        int dataSize = totalSamples * bytesPerSample;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            writeString(dos, "RIFF");
            writeIntLE(dos, 36 + dataSize);
            writeString(dos, "WAVE");
            writeString(dos, "fmt ");
            writeIntLE(dos, 16);
            writeShortLE(dos, (short) 1);
            writeShortLE(dos, (short) 1);
            writeIntLE(dos, sampleRate);
            writeIntLE(dos, sampleRate * bytesPerSample);
            writeShortLE(dos, (short) bytesPerSample);
            writeShortLE(dos, (short) 16);
            writeString(dos, "data");
            writeIntLE(dos, dataSize);

            for (int i = 0; i < totalSamples; i++) {
                double angle = 2.0 * Math.PI * frequency * i / sampleRate;
                short sample = (short) (Math.sin(angle) * 3000);
                writeShortLE(dos, sample);
            }
            dos.flush();
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate mock WAV audio", ex);
        }
    }

    private static String buildHook(String topic, String hookStrategy) {
        return switch (hookStrategy) {
            case "curiosity-gap" -> "The fastest shift in " + topic + " starts with one move most people skip.";
            case "controversial-statement" -> "Brute force is the worst strategy for " + topic + ".";
            case "emotional-hook" -> "If " + topic + " keeps draining you, this might be the reset you needed.";
            case "story-hook" -> "A quick story: one creator changed one habit and everything around " + topic + " clicked.";
            case "problem-hook" -> "The real problem with " + topic + " is not effort, it's direction.";
            default -> "There is a better way to approach " + topic + " than repeating the same routine.";
        };
    }

    private static String buildCta(String style, String structureStrategy, String ctaStrategy) {
        return switch (ctaStrategy) {
            case "follow-cta" -> "Follow for more creator-grade short scripts.";
            case "save-cta" -> "Save this and test it in your next video sprint.";
            case "comment-cta" -> "Comment the part you want to apply first.";
            case "reflection-cta" -> "Which line felt uncomfortably true for your workflow?";
            default -> {
                if ("storytelling".equals(style) || "narrative-lesson".equals(structureStrategy)) {
                    yield "Comment if you want more story-led breakdowns.";
                }
                if ("facts".equals(style)) {
                    yield "Save this as your next content reference.";
                }
                yield "Follow for more practical creator systems.";
            }
        };
    }

    private static String buildScript(String topic, String style, String structureStrategy, String hook, String cta) {
        String body = switch (structureStrategy) {
            case "mini-story" -> """
                A creator I know kept waiting for perfect conditions and stayed stuck.
                The conflict was simple: huge ambition, zero repeatable routine.
                The resolution was a tiny daily output target that survived low-energy days.
                """;
            case "problem-solution-insight" -> """
                Problem: people overestimate motivation and underestimate systems.
                Solution: define a minimum daily action and remove one friction point tonight.
                Insight: consistency feels easier once identity catches up to action.
                """;
            case "narrative-lesson" -> """
                It started with missed deadlines and silent frustration.
                Then one constraint changed everything: publish a smaller version daily.
                The lesson is that progress usually rewards repetition, not intensity.
                """;
            default -> """
                Here's what works in practice: start with one measurable action.
                Then connect it to a trigger you already do every day.
                Finally, review weekly so momentum compounds instead of disappearing.
                """;
        };

        String styleLine = switch (style) {
            case "storytelling" -> "That is the moment the story turns from pressure to clarity.";
            case "facts" -> "Across high-performing creators, this pattern shows up again and again.";
            case "self-improvement" -> "Make it boring enough to repeat and strong enough to matter.";
            default -> "The secret is keeping the standard realistic and the execution daily.";
        };

        return (hook + " " + body + " " + styleLine + " " + firstNonBlank(cta, ""))
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static String buildCaption(String topic, String hook, String ctaStrategy) {
        String tail = switch (ctaStrategy) {
            case "save-cta" -> "Save for your next upload plan.";
            case "comment-cta" -> "Comment your takeaway.";
            case "reflection-cta" -> "Reflect before your next post.";
            default -> "Follow for more short-form strategy.";
        };
        return (hook + " " + topic + ". " + tail).replaceAll("\\s+", " ").trim();
    }

    private static List<String> buildHashtags(String style, String topic) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add("#shorts");
        tags.add("#aivideo");
        tags.add("#creator");
        tags.add("#" + style);

        for (String token : topic.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (token.length() >= 4) {
                tags.add("#" + token);
            }
            if (tags.size() >= 7) {
                break;
            }
        }
        return new ArrayList<>(tags);
    }

    private static int parseDuration(String durationRaw) {
        String digits = durationRaw.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return 30;
        }
        return Integer.parseInt(digits);
    }

    private static String extractFieldAny(String prompt, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = extractField(prompt, fieldName);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String extractField(String prompt, String fieldName) {
        if (prompt == null) {
            return null;
        }
        Pattern pattern = Pattern.compile(fieldName + "\\s*:\\s*(.+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(prompt);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).trim();
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    private static void writeString(DataOutputStream dos, String value) throws IOException {
        dos.writeBytes(value);
    }

    private static void writeIntLE(DataOutputStream dos, int value) throws IOException {
        dos.writeByte(value & 0xFF);
        dos.writeByte((value >> 8) & 0xFF);
        dos.writeByte((value >> 16) & 0xFF);
        dos.writeByte((value >> 24) & 0xFF);
    }

    private static void writeShortLE(DataOutputStream dos, short value) throws IOException {
        dos.writeByte(value & 0xFF);
        dos.writeByte((value >> 8) & 0xFF);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
    }

    private static String randomPick(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }

    private static String firstNonBlank(String first, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return fallback;
    }
}
