package com.autoshorts.ai.storage.impl;

import com.autoshorts.ai.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "app.storage.mock", havingValue = "true")
public class LocalMediaPathService {

    private final Path basePath;
    private final String publicBaseUrl;

    public LocalMediaPathService(AppProperties appProperties) {
        this.basePath = Path.of(appProperties.getWorkingDir()).resolve("mock-storage").toAbsolutePath().normalize();
        String configuredBase = appProperties.getStorage().getLocalPublicBaseUrl();
        this.publicBaseUrl = StringUtils.hasText(configuredBase)
            ? stripTrailingSlash(configuredBase.trim())
            : "http://localhost:8080";
    }

    public Path resolveObjectPath(String objectKey) {
        String normalizedKey = normalizeObjectKey(objectKey);
        Path target = basePath.resolve(normalizedKey).normalize();
        if (!target.startsWith(basePath)) {
            throw new IllegalArgumentException("Invalid object key: " + objectKey);
        }
        return target;
    }

    public String buildPublicUrl(String objectKey) {
        return publicBaseUrl + "/api/media/" + normalizeObjectKey(objectKey);
    }

    public Path getBasePath() {
        return basePath;
    }

    private String normalizeObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new IllegalArgumentException("Object key must not be blank");
        }
        String normalized = objectKey.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("Object key must not be blank");
        }
        return normalized;
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
