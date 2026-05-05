package com.autoshorts.ai.subtitles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
@Service
public class SimpleSrtSubtitleGeneratorService implements SubtitleGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(SimpleSrtSubtitleGeneratorService.class);

    @Override
    public void generateSrt(String scriptText, int durationSeconds, Path outputPath) {
        var cues = NarrationTimelinePlanner.buildCues(scriptText, durationSeconds);
        StringBuilder sb = new StringBuilder();

        for (var cue : cues) {
            sb.append(cue.index())
                .append('\n')
                .append(formatTimestamp(cue.startMs()))
                .append(" --> ")
                .append(formatTimestamp(cue.endMs()))
                .append('\n')
                .append(cue.text())
                .append("\n\n");
        }

        try {
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, sb.toString(), StandardCharsets.UTF_8);
            log.info("event=subtitles_generated file={} segments={}", outputPath, cues.size());
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

}
