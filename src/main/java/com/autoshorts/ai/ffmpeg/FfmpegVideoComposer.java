package com.autoshorts.ai.ffmpeg;

public interface FfmpegVideoComposer {

    void compose(VideoCompositionRequest request);

    /**
     * Verifies the FFmpeg binary is resolvable and executable. Called once at startup so a
     * misconfigured binary surfaces immediately instead of on the first video composition.
     * Default is a no-op (used by test/fake composers); the real process-based composer overrides it.
     */
    default void verifyBinaryAvailable() {
        // no-op
    }
}
