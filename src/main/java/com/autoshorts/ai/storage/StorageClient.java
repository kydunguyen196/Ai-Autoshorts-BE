package com.autoshorts.ai.storage;

import java.nio.file.Path;

public interface StorageClient {

    String upload(Path sourceFile, String objectKey, String contentType);
}
