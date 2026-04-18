package com.autoshorts.ai.util;

import com.autoshorts.ai.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

@Component
public class WorkingDirectoryManager {

    private static final Logger log = LoggerFactory.getLogger(WorkingDirectoryManager.class);

    private final AppProperties appProperties;

    public WorkingDirectoryManager(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() {
        Path basePath = resolveBasePath();
        try {
            Files.createDirectories(basePath);
            if (!Files.isWritable(basePath)) {
                throw new IllegalStateException("Working directory is not writable: " + basePath);
            }
            log.info("event=working_dir_ready path={}", basePath);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize working directory: " + basePath, ex);
        }
    }

    public Path createJobDirectory(UUID jobId) throws IOException {
        Path basePath = resolveBasePath();
        Files.createDirectories(basePath);
        Path jobPath = basePath.resolve(jobId.toString()).normalize();
        Files.createDirectories(jobPath);
        log.info("event=job_workdir_created path={}", jobPath);
        return jobPath;
    }

    public void cleanupJobDirectory(Path jobPath) {
        cleanupJobDirectory(jobPath, false);
    }

    public void cleanupJobDirectory(Path jobPath, boolean failedJob) {
        if (jobPath == null || !appProperties.getCleanup().isDeleteTempFiles()) {
            return;
        }

        if (failedJob && appProperties.getCleanup().isKeepFailedJobFiles()) {
            log.info("event=cleanup_skipped reason=failed_job_retained path={}", jobPath.toAbsolutePath().normalize());
            return;
        }

        Path basePath = resolveBasePath();
        Path normalizedJobPath = jobPath.toAbsolutePath().normalize();

        if (!normalizedJobPath.startsWith(basePath)) {
            log.warn("event=cleanup_skipped reason=outside_working_dir path={}", normalizedJobPath);
            return;
        }

        if (!Files.exists(normalizedJobPath)) {
            return;
        }

        try (var paths = Files.walk(normalizedJobPath)) {
            paths.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        log.warn("event=cleanup_file_failed path={} message={}", path, e.getMessage());
                    }
                });
            log.info("event=cleanup_done path={}", normalizedJobPath);
        } catch (IOException e) {
            log.warn("event=cleanup_failed path={} message={}", normalizedJobPath, e.getMessage());
        }
    }

    private Path resolveBasePath() {
        return Path.of(appProperties.getWorkingDir()).toAbsolutePath().normalize();
    }
}
