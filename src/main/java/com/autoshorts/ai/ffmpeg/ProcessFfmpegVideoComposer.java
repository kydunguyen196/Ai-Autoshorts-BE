package com.autoshorts.ai.ffmpeg;

import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.exception.VideoProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ProcessFfmpegVideoComposer implements FfmpegVideoComposer {

    private static final Logger log = LoggerFactory.getLogger(ProcessFfmpegVideoComposer.class);
    private static final int MAX_CAPTURED_LOG_LINES = 400;

    private final AppProperties appProperties;
    private final AtomicBoolean ffmpegVerified = new AtomicBoolean(false);

    public ProcessFfmpegVideoComposer(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void verifyBinaryAvailable() {
        ensureFfmpegAvailability();
    }

    @Override
    public void compose(VideoCompositionRequest request) {
        ensureFfmpegAvailability();
        validateInput(request);
        ensureOutputDirectory(request.getOutputPath());

        List<String> command = buildCommand(request);
        log.info("event=ffmpeg_start output={}", request.getOutputPath().toAbsolutePath());
        log.debug("event=ffmpeg_command command={}", String.join(" ", command));

        Process process;
        try {
            Path outputParent = request.getOutputPath().toAbsolutePath().getParent();
            if (outputParent == null) {
                throw new VideoProcessingException("Output path must include a parent directory: " + request.getOutputPath());
            }
            process = new ProcessBuilder(command)
                .directory(outputParent.toFile())
                .redirectErrorStream(true)
                .start();
        } catch (IOException ex) {
            throw new VideoProcessingException("Failed to start ffmpeg process with binary: " + appProperties.getFfmpeg().getBinary(), ex);
        }

        CompletableFuture<List<String>> outputFuture = CompletableFuture.supplyAsync(() -> readOutput(process));

        boolean finished;
        try {
            finished = process.waitFor(appProperties.getFfmpeg().getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new VideoProcessingException("FFmpeg process interrupted", ex);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new VideoProcessingException("FFmpeg process timed out");
        }

        List<String> capturedOutput = getCapturedOutput(outputFuture);
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new VideoProcessingException(
                "FFmpeg failed with exit code %d. Output:\n%s".formatted(exitCode, String.join("\n", capturedOutput))
            );
        }

        if (!Files.exists(request.getOutputPath())) {
            throw new VideoProcessingException("FFmpeg completed but output file was not created");
        }

        log.info("event=ffmpeg_success output={}", request.getOutputPath().toAbsolutePath());
    }

    private void validateInput(VideoCompositionRequest request) {
        if (!Files.exists(request.getNarrationAudioPath())) {
            throw new VideoProcessingException("Narration audio file not found: " + request.getNarrationAudioPath());
        }
        if (!Files.exists(request.getSubtitlePath())) {
            throw new VideoProcessingException("Subtitle file not found: " + request.getSubtitlePath());
        }
        for (SceneMediaSegment segment : request.getSceneMediaSegments()) {
            if (segment.getMediaPath() == null || !Files.exists(segment.getMediaPath())) {
                throw new VideoProcessingException("Scene media file not found: " + (segment.getMediaPath() == null ? "null" : segment.getMediaPath()));
            }
        }
    }

    private List<String> buildCommand(VideoCompositionRequest request) {
        List<String> command = new ArrayList<>();
        command.add(appProperties.getFfmpeg().getBinary());
        command.add("-y");
        command.add("-hide_banner");
        command.add("-loglevel");
        command.add("warning");

        List<SceneMediaSegment> sceneSegments = request.getSceneMediaSegments();
        int audioInputIndex;
        boolean useSceneSegments = sceneSegments != null && !sceneSegments.isEmpty();

        if (useSceneSegments) {
            for (SceneMediaSegment segment : sceneSegments) {
                if (isImage(segment.getMediaPath())) {
                    command.add("-loop");
                    command.add("1");
                    command.add("-framerate");
                    command.add("30");
                    command.add("-t");
                    command.add(String.valueOf(segment.getDurationSeconds()));
                } else {
                    command.add("-stream_loop");
                    command.add("-1");
                    command.add("-t");
                    command.add(String.valueOf(segment.getDurationSeconds()));
                }
                command.add("-i");
                command.add(segment.getMediaPath().toAbsolutePath().toString());
            }
            audioInputIndex = sceneSegments.size();
        } else {
            Path backgroundPath = request.getBackgroundMediaPath();
            if (backgroundPath != null && Files.exists(backgroundPath)) {
                if (isImage(backgroundPath)) {
                    command.add("-loop");
                    command.add("1");
                    command.add("-framerate");
                    command.add("30");
                    command.add("-i");
                    command.add(backgroundPath.toAbsolutePath().toString());
                } else {
                    command.add("-stream_loop");
                    command.add("-1");
                    command.add("-i");
                    command.add(backgroundPath.toAbsolutePath().toString());
                }
            } else {
                log.warn("event=background_missing fallback=color_source configuredPath={}", backgroundPath);
                command.add("-f");
                command.add("lavfi");
                command.add("-i");
                command.add("color=c=0x1a1a1a:s=1080x1920:r=30");
            }
            audioInputIndex = 1;
        }

        command.add("-i");
        command.add(request.getNarrationAudioPath().toAbsolutePath().toString());

        // TODO: Add optional background music mixing for richer compositions.
        String subtitlePath = escapeForFfmpegFilter(request.getSubtitlePath().toAbsolutePath().toString());
        String subtitleFilter = "subtitles=filename='" + subtitlePath + "':force_style='Alignment=2,FontName=Arial,"
            + "FontSize=15,PrimaryColour=&H00FFFFFF&,OutlineColour=&H000000&,BorderStyle=3,Outline=2,Shadow=1,MarginV=70'";

        if (useSceneSegments) {
            command.add("-filter_complex");
            command.add(buildSceneSlideshowFilter(sceneSegments, subtitleFilter));
            command.add("-map");
            command.add("[vout]");
            command.add("-map");
            command.add(audioInputIndex + ":a");
        } else {
            String videoFilter = "scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920," + subtitleFilter;
            command.add("-vf");
            command.add(videoFilter);
        }

        command.add("-t");
        command.add(String.valueOf(request.getDurationSeconds()));
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("medium");
        command.add("-crf");
        command.add("18");
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("192k");
        command.add("-shortest");
        command.add("-movflags");
        command.add("+faststart");
        command.add(request.getOutputPath().toAbsolutePath().toString());

        return command;
    }

    private String buildSceneSlideshowFilter(List<SceneMediaSegment> sceneSegments, String subtitleFilter) {
        StringBuilder filter = new StringBuilder();
        for (int index = 0; index < sceneSegments.size(); index++) {
            SceneMediaSegment segment = sceneSegments.get(index);
            int frames = Math.max(1, segment.getDurationSeconds() * 30);

            filter.append('[').append(index).append(":v]");
            if (isImage(segment.getMediaPath())) {
                String xExpr = index % 2 == 0 ? "(iw-iw/zoom)*0.35" : "(iw-iw/zoom)*0.65";
                String yExpr = index % 3 == 0 ? "(ih-ih/zoom)*0.30" : "(ih-ih/zoom)*0.55";
                filter
                    .append("scale=1260:2240:force_original_aspect_ratio=increase,")
                    .append("crop=1080:1920,")
                    .append("zoompan=z='min(zoom+0.00085,1.12)':")
                    .append("x='").append(xExpr).append("':")
                    .append("y='").append(yExpr).append("':")
                    .append("d=").append(frames).append(":")
                    .append("s=1080x1920:fps=30,");
            } else {
                filter
                    .append("scale=1080:1920:force_original_aspect_ratio=increase,")
                    .append("crop=1080:1920,")
                    .append("trim=duration=").append(segment.getDurationSeconds()).append(',')
                    .append("setpts=PTS-STARTPTS,")
                    .append("fps=30,");
            }
            filter
                .append("format=yuv420p")
                .append("[v").append(index).append("];");
        }

        for (int index = 0; index < sceneSegments.size(); index++) {
            filter.append("[v").append(index).append(']');
        }
        filter
            .append("concat=n=").append(sceneSegments.size()).append(":v=1:a=0[vbase];")
            .append("[vbase]").append(subtitleFilter).append("[vout]");
        return filter.toString();
    }

    private List<String> readOutput(Process process) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (lines.size() < MAX_CAPTURED_LOG_LINES) {
                    lines.add(line);
                }
            }
        } catch (IOException ex) {
            lines.add("Failed to read ffmpeg output: " + ex.getMessage());
        }
        return lines;
    }

    private List<String> getCapturedOutput(CompletableFuture<List<String>> outputFuture) {
        try {
            return outputFuture.get(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of("Interrupted while collecting ffmpeg logs");
        } catch (ExecutionException | TimeoutException ex) {
            return List.of("Failed to collect ffmpeg logs: " + ex.getMessage());
        }
    }

    private void ensureOutputDirectory(Path outputPath) {
        try {
            Path parent = outputPath.toAbsolutePath().getParent();
            if (parent == null) {
                throw new VideoProcessingException("Output path must include a parent directory: " + outputPath);
            }
            Files.createDirectories(parent);
        } catch (IOException ex) {
            throw new VideoProcessingException("Failed to prepare output directory for ffmpeg: " + outputPath, ex);
        }
    }

    private void ensureFfmpegAvailability() {
        if (ffmpegVerified.get()) {
            return;
        }

        synchronized (ffmpegVerified) {
            if (ffmpegVerified.get()) {
                return;
            }

            List<String> checkCommand = List.of(appProperties.getFfmpeg().getBinary(), "-version");
            Process process;
            try {
                process = new ProcessBuilder(checkCommand)
                    .redirectErrorStream(true)
                    .start();
            } catch (IOException ex) {
                throw new VideoProcessingException("FFmpeg binary not found or not executable: " + appProperties.getFfmpeg().getBinary(), ex);
            }

            try {
                boolean finished = process.waitFor(15, TimeUnit.SECONDS);
                if (!finished || process.exitValue() != 0) {
                    throw new VideoProcessingException("FFmpeg check failed for binary: " + appProperties.getFfmpeg().getBinary());
                }
                ffmpegVerified.set(true);
                log.info("event=ffmpeg_binary_verified binary={}", appProperties.getFfmpeg().getBinary());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new VideoProcessingException("Interrupted while verifying ffmpeg binary", ex);
            }
        }
    }

    private boolean isImage(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".png")
            || fileName.endsWith(".jpg")
            || fileName.endsWith(".jpeg")
            || fileName.endsWith(".webp");
    }

    private String escapeForFfmpegFilter(String value) {
        return value
            .replace("\\", "/")
            .replace(":", "\\:")
            .replace("'", "\\'");
    }
}
