package com.autoshorts.ai.integration.support;

import com.autoshorts.ai.exception.VideoProcessingException;
import com.autoshorts.ai.ffmpeg.FfmpegVideoComposer;
import com.autoshorts.ai.ffmpeg.VideoCompositionRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeterministicFfmpegVideoComposer implements FfmpegVideoComposer {

    private final AtomicBoolean failNext = new AtomicBoolean(false);
    private volatile String failureMessage = "Simulated ffmpeg failure for integration test";

    @Override
    public void compose(VideoCompositionRequest request) {
        if (failNext.getAndSet(false)) {
            throw new VideoProcessingException(failureMessage);
        }

        try {
            Files.createDirectories(request.getOutputPath().toAbsolutePath().getParent());
            Files.write(
                request.getOutputPath(),
                ("fake-mp4-for-" + request.getOutputPath().getFileName()).getBytes(StandardCharsets.UTF_8)
            );
        } catch (IOException ex) {
            throw new VideoProcessingException("Failed to write fake composed video", ex);
        }
    }

    public void failNextComposition(String message) {
        this.failureMessage = message;
        this.failNext.set(true);
    }

    public void resetFailure() {
        this.failNext.set(false);
        this.failureMessage = "Simulated ffmpeg failure for integration test";
    }
}
