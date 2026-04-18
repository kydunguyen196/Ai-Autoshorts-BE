package com.autoshorts.ai.subtitles;

import java.nio.file.Path;

public interface SubtitleGeneratorService {

    void generateSrt(String scriptText, int durationSeconds, Path outputPath);
}
