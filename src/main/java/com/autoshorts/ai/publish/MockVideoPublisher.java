package com.autoshorts.ai.publish;

import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.exception.ExternalServiceException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MockVideoPublisher implements VideoPublisher {

    private final AppProperties appProperties;

    public MockVideoPublisher(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public String providerKey() {
        return "mock";
    }

    @Override
    public PublishResult publish(PublishRequest request) {
        maybeSleep();

        String failureKeyword = appProperties.getPublish().getMockFailureKeyword();
        if (StringUtils.hasText(failureKeyword)
            && StringUtils.hasText(request.topic())
            && request.topic().toLowerCase().contains(failureKeyword.toLowerCase())) {
            throw new ExternalServiceException("Mock publish rejected by configured failure keyword");
        }

        String externalId = "mock-" + request.jobId();
        String requestPayload = "{\"platform\":\"" + request.platform() + "\",\"finalVideoUrl\":\"" + request.finalVideoUrl() + "\"}";
        String responsePayload = "{\"externalId\":\"" + externalId + "\",\"status\":\"accepted\"}";
        String details = "platform=" + request.platform() + ",channelId=" + request.channelId();
        return new PublishResult(providerKey(), externalId, details, requestPayload, responsePayload, request.targetAccountId());
    }

    private void maybeSleep() {
        long latencyMs = Math.max(0, appProperties.getPublish().getMockLatencyMs());
        if (latencyMs <= 0) {
            return;
        }
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while simulating mock publish latency", ex);
        }
    }
}
