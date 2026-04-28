package com.autoshorts.ai.ffmpeg;

import java.nio.file.Path;

public class SceneMediaSegment {

    private final Path mediaPath;
    private final int startSec;
    private final int endSec;

    public SceneMediaSegment(Path mediaPath, int startSec, int endSec) {
        this.mediaPath = mediaPath;
        this.startSec = startSec;
        this.endSec = endSec;
    }

    public Path getMediaPath() {
        return mediaPath;
    }

    public int getStartSec() {
        return startSec;
    }

    public int getEndSec() {
        return endSec;
    }

    public int getDurationSeconds() {
        return Math.max(1, endSec - startSec);
    }
}
