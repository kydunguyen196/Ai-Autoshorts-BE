package com.autoshorts.ai.client.model;

import com.autoshorts.ai.entity.VisualGenerationMode;

public class GeneratedVisualImage {

    private final byte[] data;
    private final String fileExtension;
    private final String contentType;
    private final VisualGenerationMode generationMode;
    private final String provider;
    private final String modelId;
    private final Long providerRequestDurationMs;
    private final String failureReason;
    private final String failureDetails;

    public GeneratedVisualImage(
        byte[] data,
        String fileExtension,
        String contentType,
        VisualGenerationMode generationMode,
        String provider,
        String modelId,
        Long providerRequestDurationMs,
        String failureReason,
        String failureDetails
    ) {
        this.data = data;
        this.fileExtension = fileExtension;
        this.contentType = contentType;
        this.generationMode = generationMode;
        this.provider = provider;
        this.modelId = modelId;
        this.providerRequestDurationMs = providerRequestDurationMs;
        this.failureReason = failureReason;
        this.failureDetails = failureDetails;
    }

    public byte[] getData() {
        return data;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getContentType() {
        return contentType;
    }

    public VisualGenerationMode getGenerationMode() {
        return generationMode;
    }

    public String getProvider() {
        return provider;
    }

    public String getModelId() {
        return modelId;
    }

    public Long getProviderRequestDurationMs() {
        return providerRequestDurationMs;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getFailureDetails() {
        return failureDetails;
    }
}
