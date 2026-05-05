package com.autoshorts.ai.subtitles;

import com.autoshorts.ai.util.SentenceUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class NarrationTimelinePlanner {

    private NarrationTimelinePlanner() {
    }

    public static List<Cue> buildCues(String scriptText, int durationSeconds) {
        List<String> segments = SentenceUtils.splitIntoSubtitleSegments(scriptText);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Cannot build timeline from empty script");
        }

        long totalMs = Math.max(1, durationSeconds) * 1000L;
        long minSegmentMs = 900;
        int maxSegments = (int) Math.max(1, totalMs / minSegmentMs);
        if (segments.size() > maxSegments) {
            segments = mergeSegments(segments, maxSegments);
        }

        List<Integer> weights = segments.stream()
            .map(NarrationTimelinePlanner::estimateWeight)
            .toList();
        int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) {
            totalWeight = segments.size();
        }

        List<Cue> cues = new ArrayList<>(segments.size());
        long cursor = 0;
        for (int i = 0; i < segments.size(); i++) {
            long start = cursor;
            long allocated = Math.max(
                minSegmentMs,
                Math.round((weights.get(i) * 1.0d / totalWeight) * totalMs)
            );
            long end = (i == segments.size() - 1) ? totalMs : Math.min(totalMs, start + allocated);
            if (end <= start) {
                end = Math.min(totalMs, start + minSegmentMs);
            }
            cursor = end;
            cues.add(new Cue(i + 1, segments.get(i), start, end));
        }

        if (!cues.isEmpty()) {
            Cue last = cues.get(cues.size() - 1);
            cues.set(cues.size() - 1, new Cue(last.index(), last.text(), last.startMs(), totalMs));
        }
        return cues;
    }

    private static int estimateWeight(String text) {
        if (!StringUtils.hasText(text)) {
            return 1;
        }
        int words = text.trim().split("\\s+").length;
        int punctuationBoost = (int) text.chars().filter(ch -> ch == ',' || ch == ';' || ch == ':').count();
        return Math.max(1, words + punctuationBoost);
    }

    private static List<String> mergeSegments(List<String> input, int targetCount) {
        if (input.size() <= targetCount) {
            return input;
        }
        List<String> merged = new ArrayList<>(targetCount);
        int chunkSize = (int) Math.ceil(input.size() / (double) targetCount);
        StringBuilder builder = new StringBuilder();
        int countInChunk = 0;
        for (String segment : input) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(segment);
            countInChunk++;
            if (countInChunk >= chunkSize) {
                merged.add(builder.toString());
                builder.setLength(0);
                countInChunk = 0;
            }
        }
        if (builder.length() > 0) {
            merged.add(builder.toString());
        }
        return merged;
    }

    public record Cue(int index, String text, long startMs, long endMs) {
    }
}

