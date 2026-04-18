package com.autoshorts.ai.client.model;

import com.autoshorts.ai.entity.AudioGenerationMode;

public class SynthesizedAudio {

    private final byte[] data;
    private final String fileExtension;
    private final String contentType;
    private final AudioGenerationMode generationMode;
    private final String provider;
    private final String modeDetail;
    private final String voiceIdUsed;
    private final String modelIdUsed;
    private final String outputFormatUsed;
    private final Long providerRequestDurationMs;
    private final String failureReason;
    private final String failureDetails;

    public SynthesizedAudio(
        byte[] data,
        String fileExtension,
        String contentType,
        AudioGenerationMode generationMode,
        String provider,
        String modeDetail
    ) {
        this(
            data,
            fileExtension,
            contentType,
            generationMode,
            provider,
            modeDetail,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public SynthesizedAudio(
        byte[] data,
        String fileExtension,
        String contentType,
        AudioGenerationMode generationMode,
        String provider,
        String modeDetail,
        String voiceIdUsed,
        String modelIdUsed,
        String outputFormatUsed,
        Long providerRequestDurationMs,
        String failureReason,
        String failureDetails
    ) {
        this.data = data;
        this.fileExtension = fileExtension;
        this.contentType = contentType;
        this.generationMode = generationMode;
        this.provider = provider;
        this.modeDetail = modeDetail;
        this.voiceIdUsed = voiceIdUsed;
        this.modelIdUsed = modelIdUsed;
        this.outputFormatUsed = outputFormatUsed;
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

    public AudioGenerationMode getGenerationMode() {
        return generationMode;
    }

    public String getProvider() {
        return provider;
    }

    public String getModeDetail() {
        return modeDetail;
    }

    public String getVoiceIdUsed() {
        return voiceIdUsed;
    }

    public String getModelIdUsed() {
        return modelIdUsed;
    }

    public String getOutputFormatUsed() {
        return outputFormatUsed;
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
