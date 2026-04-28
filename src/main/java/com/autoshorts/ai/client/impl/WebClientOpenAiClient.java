package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.AiClient;
import com.autoshorts.ai.client.OpenAiClient;
import com.autoshorts.ai.client.model.OpenAiGenerationResult;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.ContentGenerationMode;
import com.autoshorts.ai.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "app.openai.mock", havingValue = "false", matchIfMissing = true)
public class WebClientOpenAiClient implements OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientOpenAiClient.class);

    private final AiClient aiClient;
    private final AppProperties appProperties;

    public WebClientOpenAiClient(AiClient aiClient, AppProperties appProperties) {
        this.aiClient = aiClient;
        this.appProperties = appProperties;
    }

    @Override
    public OpenAiGenerationResult generateShortVideoScript(String topic, String style, int durationSeconds) {
        OpenAiGenerationResult response = generateFromPrompts(
            """
                You are a creator scriptwriter for short vertical videos.
                Return only the final script text. No markdown.
                Keep it concise, high-retention, and spoken naturally.
                """,
            buildPrompt(topic, style, durationSeconds)
        );
        log.info(
            "event=openai_script_generated mode={} provider={} model={} length={}",
            response.mode(),
            response.provider(),
            response.model(),
            response.content() != null ? response.content().length() : 0
        );
        return response;
    }

    @Override
    public OpenAiGenerationResult generateFromPrompts(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(appProperties.getOpenai().getApiKey())) {
            log.warn(
                "event=openai_request_blocked mode=REAL reason=missing_api_key openAiMockConfigured={}",
                appProperties.getOpenai().isMock()
            );
            throw new ExternalServiceException("Missing AI API key. Set environment variable: apikey");
        }

        log.info(
            "event=openai_request_start mode=REAL provider=9router_openai_compatible model={} baseUrl={}",
            appProperties.getOpenai().getModel(),
            appProperties.getOpenai().getBaseUrl()
        );

        AiClient.ChatCompletionResult result = aiClient.chatCompletion(systemPrompt, userPrompt, 0.85);
        String content = result.content();
        log.info(
            "event=openai_request_success mode=REAL provider=9router_openai_compatible model={} contentLength={}",
            result.model(),
            content.length()
        );
        return new OpenAiGenerationResult(
            content,
            ContentGenerationMode.REAL,
            "9router_openai_compatible",
            result.model()
        );
    }

    private String buildPrompt(String topic, String style, int durationSeconds) {
        return """
            Write a %d-second short-form video script.
            Topic: %s
            Style: %s

            Requirements:
            - Hook in first sentence
            - 3-5 concise points
            - Strong ending CTA
            - Speakable format suitable for TTS
            - Around %d seconds when spoken at normal speed
            """.formatted(durationSeconds, topic, style, durationSeconds);
    }
}
