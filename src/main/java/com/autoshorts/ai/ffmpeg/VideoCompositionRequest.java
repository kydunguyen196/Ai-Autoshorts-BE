package com.autoshorts.ai.ffmpeg;

import java.nio.file.Path;
import java.util.List;

public class VideoCompositionRequest {

    private final Path backgroundMediaPath;
    private final Path narrationAudioPath;
    private final Path subtitlePath;
    private final Path outputPath;
    private final int durationSeconds;
    private final Path backgroundMusicPath;
    private final List<SceneMediaSegment> sceneMediaSegments;

    public VideoCompositionRequest(
        Path backgroundMediaPath,
        Path narrationAudioPath,
        Path subtitlePath,
        Path outputPath,
        int durationSeconds,
        Path backgroundMusicPath
    ) {
        this(
            backgroundMediaPath,
            narrationAudioPath,
            subtitlePath,
            outputPath,
            durationSeconds,
            backgroundMusicPath,
            List.of()
        );
    }

    public VideoCompositionRequest(
        Path backgroundMediaPath,
        Path narrationAudioPath,
        Path subtitlePath,
        Path outputPath,
        int durationSeconds,
        Path backgroundMusicPath,
        List<SceneMediaSegment> sceneMediaSegments
    ) {
        this.backgroundMediaPath = backgroundMediaPath;
        this.narrationAudioPath = narrationAudioPath;
        this.subtitlePath = subtitlePath;
        this.outputPath = outputPath;
        this.durationSeconds = durationSeconds;
        this.backgroundMusicPath = backgroundMusicPath;
        this.sceneMediaSegments = sceneMediaSegments == null ? List.of() : List.copyOf(sceneMediaSegments);
    }

    public Path getBackgroundMediaPath() {
        return backgroundMediaPath;
    }

    public Path getNarrationAudioPath() {
        return narrationAudioPath;
    }

    public Path getSubtitlePath() {
        return subtitlePath;
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public Path getBackgroundMusicPath() {
        return backgroundMusicPath;
    }

    public List<SceneMediaSegment> getSceneMediaSegments() {
        return sceneMediaSegments;
    }
}
