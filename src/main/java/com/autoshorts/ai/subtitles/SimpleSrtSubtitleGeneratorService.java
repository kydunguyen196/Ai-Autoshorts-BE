package com.autoshorts.ai.subtitles;

import com.autoshorts.ai.util.SentenceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class SimpleSrtSubtitleGeneratorService implements SubtitleGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(SimpleSrtSubtitleGeneratorService.class);

    @Override
    public void generateSrt(String scriptText, int durationSeconds, Path outputPath) {
        List<String> segments = SentenceUtils.splitIntoSubtitleSegments(scriptText);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Cannot generate subtitles from empty script");
        }

        long totalMs = durationSeconds * 1000L;
        long minSegmentMs = 900;
        int maxSegments = (int) Math.max(1, totalMs / minSegmentMs);
        if (segments.size() > maxSegments) {
            segments = mergeSegments(segments, maxSegments);
        }
        long baseSegmentMs = Math.max(minSegmentMs, totalMs / segments.size());
        long cursor = 0;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < segments.size(); i++) {
            long start = cursor;
            long end = (i == segments.size() - 1) ? totalMs : Math.min(totalMs, start + baseSegmentMs);
            cursor = end;

            sb.append(i + 1)
                .append('\n')
                .append(formatTimestamp(start))
                .append(" --> ")
                .append(formatTimestamp(end))
                .append('\n')
                .append(segments.get(i))
                .append("\n\n");
        }

        try {
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, sb.toString(), StandardCharsets.UTF_8);
            log.info("event=subtitles_generated file={} segments={}", outputPath, segments.size());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write subtitle file: " + outputPath, ex);
        }
    }

    private String formatTimestamp(long millis) {
        long hours = millis / 3_600_000;
        long minutes = (millis % 3_600_000) / 60_000;
        long seconds = (millis % 60_000) / 1000;
        long milliseconds = millis % 1000;
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds);
    }

    private List<String> mergeSegments(List<String> input, int targetCount) {
        if (input.size() <= targetCount) {
            return input;
        }

        List<String> merged = new java.util.ArrayList<>(targetCount);
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
}
