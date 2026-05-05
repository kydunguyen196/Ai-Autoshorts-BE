package com.autoshorts.ai.ffmpeg;

import com.autoshorts.ai.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
public class AudioDurationResolver {

    private static final Logger log = LoggerFactory.getLogger(AudioDurationResolver.class);

    private final AppProperties appProperties;

    public AudioDurationResolver(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public int resolveDurationSeconds(Path audioPath, int fallbackSeconds) {
        int fallback = Math.max(1, fallbackSeconds);
        if (audioPath == null) {
            return fallback;
        }

        String ffprobe = resolveFfprobeBinary(appProperties.getFfmpeg().getBinary());
        if (!StringUtils.hasText(ffprobe)) {
            return fallback;
        }

        List<String> command = List.of(
            ffprobe,
            "-v", "error",
            "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1",
            audioPath.toAbsolutePath().toString()
        );

        try {
            Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        lines.add(line.trim());
                    }
                }
            }

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished || process.exitValue() != 0 || lines.isEmpty()) {
                return fallback;
            }

            double durationSeconds = Double.parseDouble(lines.get(0));
            if (durationSeconds <= 0) {
                return fallback;
            }

            int resolved = Math.max(1, (int) Math.ceil(durationSeconds));
            log.info(
                "event=audio_duration_resolved path={} durationSeconds={} fallbackSeconds={}",
                audioPath.toAbsolutePath(),
                resolved,
                fallback
            );
            return resolved;
        } catch (Exception ex) {
            log.warn(
                "event=audio_duration_resolve_failed path={} message={}",
                audioPath.toAbsolutePath(),
                ex.getMessage()
            );
            return fallback;
        }
    }

    private String resolveFfprobeBinary(String ffmpegBinary) {
        if (!StringUtils.hasText(ffmpegBinary)) {
            return null;
        }

        String trimmed = ffmpegBinary.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.endsWith("ffmpeg.exe")) {
            return trimmed.substring(0, trimmed.length() - "ffmpeg.exe".length()) + "ffprobe.exe";
        }
        if (lower.endsWith("ffmpeg")) {
            return trimmed.substring(0, trimmed.length() - "ffmpeg".length()) + "ffprobe";
        }
        return "ffprobe";
    }
}

