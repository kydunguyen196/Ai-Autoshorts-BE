package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.OpenAiClient;
import com.autoshorts.ai.client.model.OpenAiGenerationResult;
import com.autoshorts.ai.entity.ContentGenerationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.openai.mock", havingValue = "true")
public class MockOpenAiClient implements OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(MockOpenAiClient.class);

    @Override
    public OpenAiGenerationResult generateShortVideoScript(String topic, String style, int durationSeconds) {
        log.info("event=openai_request_mock mode=MOCK provider=mock_openai");
        return new OpenAiGenerationResult(
            MockGenerationSupport.script(),
            ContentGenerationMode.MOCK,
            "mock_openai",
            "mock-v1"
        );
    }

    @Override
    public OpenAiGenerationResult generateFromPrompts(String systemPrompt, String userPrompt) {
        log.info("event=openai_request_mock mode=MOCK provider=mock_openai");
        return new OpenAiGenerationResult(
            MockGenerationSupport.contentJsonFromPrompt(userPrompt),
            ContentGenerationMode.MOCK,
            "mock_openai",
            "mock-v1"
        );
    }
}
