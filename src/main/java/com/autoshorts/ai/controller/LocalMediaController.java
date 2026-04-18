package com.autoshorts.ai.controller;

import com.autoshorts.ai.exception.ResourceNotFoundException;
import com.autoshorts.ai.storage.impl.LocalMediaPathService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@RestController
@RequestMapping("/api/media")
@ConditionalOnProperty(name = "app.storage.mock", havingValue = "true")
public class LocalMediaController {

    private final LocalMediaPathService localMediaPathService;

    public LocalMediaController(LocalMediaPathService localMediaPathService) {
        this.localMediaPathService = localMediaPathService;
    }

    @GetMapping("/{*objectKey}")
    public ResponseEntity<Resource> readMedia(@PathVariable String objectKey) {
        Path path;
        try {
            path = localMediaPathService.resolveObjectPath(objectKey);
        } catch (IllegalArgumentException ex) {
            throw new ResourceNotFoundException("Media asset not found");
        }

        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new ResourceNotFoundException("Media asset not found");
        }

        String contentType = probeContentType(path);
        Resource resource = new FileSystemResource(path.toFile());
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
            .contentType(MediaType.parseMediaType(contentType))
            .body(resource);
    }

    private String probeContentType(Path path) {
        try {
            String contentType = Files.probeContentType(path);
            if (StringUtils.hasText(contentType)) {
                return contentType;
            }
        } catch (Exception ignored) {
            // fallback below
        }

        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".mp4")) {
            return "video/mp4";
        }
        if (name.endsWith(".srt")) {
            return "application/x-subrip";
        }
        if (name.endsWith(".wav")) {
            return "audio/wav";
        }
        if (name.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
