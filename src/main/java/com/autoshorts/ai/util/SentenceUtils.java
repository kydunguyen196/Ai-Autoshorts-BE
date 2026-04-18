package com.autoshorts.ai.util;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class SentenceUtils {

    private static final Pattern SENTENCE_PATTERN = Pattern.compile("(?<=[.!?])\\s+");

    private SentenceUtils() {
    }

    public static List<String> splitIntoSubtitleSegments(String text) {
        List<String> segments = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return segments;
        }

        String normalized = text.trim().replaceAll("\\s+", " ");
        for (String sentence : SENTENCE_PATTERN.split(normalized)) {
            String trimmed = sentence.trim();
            if (!trimmed.isEmpty()) {
                segments.add(trimmed);
            }
        }

        if (segments.isEmpty()) {
            segments.add(normalized);
        }

        // If there is only one very long segment, split it to improve readability.
        if (segments.size() == 1 && normalized.length() > 120) {
            return splitByWordCount(normalized, 10);
        }

        return segments;
    }

    private static List<String> splitByWordCount(String text, int wordsPerSegment) {
        List<String> segments = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        int count = 0;

        for (String word : words) {
            if (count > 0) {
                current.append(' ');
            }
            current.append(word);
            count++;

            if (count >= wordsPerSegment) {
                segments.add(current.toString().trim());
                current.setLength(0);
                count = 0;
            }
        }

        if (current.length() > 0) {
            segments.add(current.toString().trim());
        }

        return segments;
    }
}
