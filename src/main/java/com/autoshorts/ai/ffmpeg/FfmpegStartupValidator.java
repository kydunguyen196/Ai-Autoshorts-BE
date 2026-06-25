package com.autoshorts.ai.ffmpeg;

import com.autoshorts.ai.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Probes the FFmpeg binary once at startup so a missing/misconfigured binary is reported
 * immediately rather than failing the first video-composition job. Non-fatal by default;
 * set {@code app.ffmpeg.fail-on-startup=true} to abort startup when FFmpeg is unavailable.
 */
@Component
public class FfmpegStartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FfmpegStartupValidator.class);

    private final FfmpegVideoComposer ffmpegVideoComposer;
    private final AppProperties appProperties;
    private final boolean failOnStartup;

    public FfmpegStartupValidator(
        FfmpegVideoComposer ffmpegVideoComposer,
        AppProperties appProperties,
        @Value("${app.ffmpeg.fail-on-startup:false}") boolean failOnStartup
    ) {
        this.ffmpegVideoComposer = ffmpegVideoComposer;
        this.appProperties = appProperties;
        this.failOnStartup = failOnStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        String binary = appProperties.getFfmpeg().getBinary();
        try {
            ffmpegVideoComposer.verifyBinaryAvailable();
            log.info("event=ffmpeg_startup_check_ok binary={}", binary);
        } catch (RuntimeException ex) {
            if (failOnStartup) {
                log.error("event=ffmpeg_startup_check_failed binary={} failOnStartup=true message={}", binary, ex.getMessage());
                throw ex;
            }
            log.error(
                "event=ffmpeg_startup_check_failed binary={} failOnStartup=false message={} "
                    + "(video composition will fail until FFmpeg is resolvable via APP_FFMPEG_BINARY/FFMPEG_PATH)",
                binary,
                ex.getMessage()
            );
        }
    }
}
