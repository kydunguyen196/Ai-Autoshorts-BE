package com.autoshorts.ai.storage.impl;

import com.autoshorts.ai.storage.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
@ConditionalOnProperty(name = "app.storage.mock", havingValue = "true")
public class LocalStorageClient implements StorageClient {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageClient.class);

    private final LocalMediaPathService localMediaPathService;

    public LocalStorageClient(LocalMediaPathService localMediaPathService) {
        this.localMediaPathService = localMediaPathService;
    }

    @Override
    public String upload(Path sourceFile, String objectKey, String contentType) {
        try {
            Path targetFile = localMediaPathService.resolveObjectPath(objectKey);
            Files.createDirectories(targetFile.getParent());
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            String url = localMediaPathService.buildPublicUrl(objectKey);
            log.info("event=local_storage_upload key={} contentType={} url={}", objectKey, contentType, url);
            return url;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to copy file to local storage: " + objectKey, ex);
        }
    }
}
