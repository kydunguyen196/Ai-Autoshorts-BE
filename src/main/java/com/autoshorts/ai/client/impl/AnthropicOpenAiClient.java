package com.autoshorts.ai.client.impl;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.autoshorts.ai.client.OpenAiClient;
import com.autoshorts.ai.client.model.OpenAiGenerationResult;
import com.autoshorts.ai.entity.ContentGenerationMode;
import com.autoshorts.ai.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.StringJoiner;

/**
 * Text provider backed by Anthropic Claude (official anthropic-java SDK).
 * Activated with APP_TEXT_PROVIDER=anthropic (and OPENAI_MOCK=false). Uses adaptive thinking for
 * higher-quality scripts/hooks/CTAs; per-call token usage is logged for cost visibility.
 */
@Component
@ConditionalOnExpression("'${app.openai.mock:false}' == 'false' && '${app.text.provider:huggingface}' == 'anthropic'")
public class AnthropicOpenAiClient implements OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicOpenAiClient.class);

    private final AnthropicClient client;
    private final String model;
    private final long maxTokens;

    public AnthropicOpenAiClient(
        @Value("${app.anthropic.api-key:}") String apiKey,
        @Value("${app.anthropic.model:claude-opus-4-8}") String model,
        @Value("${app.anthropic.max-tokens:4000}") long maxTokens,
        @Value("${app.anthropic.base-url:}") String baseUrl
    ) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                "APP_TEXT_PROVIDER=anthropic requires ANTHROPIC_API_KEY (app.anthropic.api-key)");
        }
        AnthropicOkHttpClient.Builder builder = AnthropicOkHttpClient.builder().apiKey(apiKey);
        if (StringUtils.hasText(baseUrl)) {
            builder.baseUrl(baseUrl);
        }
        this.client = builder.build();
        this.model = model;
        this.maxTokens = maxTokens;
        log.info("event=anthropic_client_initialized model={}", model);
    }

    @CircuitBreaker(name = "ai-providers")
    @Override
    public OpenAiGenerationResult generateShortVideoScript(String topic, String style, int durationSeconds) {
        String systemPrompt = """
            You are a creator scriptwriter for short vertical videos.
            Return only the final script text. No markdown.
            Keep it concise, high-retention, and spoken naturally.
            """;
        String userPrompt = """
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
        return generateFromPrompts(systemPrompt, userPrompt);
    }

    @CircuitBreaker(name = "ai-providers")
    @Override
    public OpenAiGenerationResult generateFromPrompts(String systemPrompt, String userPrompt) {
        MessageCreateParams params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(maxTokens)
            .thinking(ThinkingConfigAdaptive.builder().build())
            .system(systemPrompt)
            .addUserMessage(userPrompt)
            .build();

        Message response;
        try {
            response = client.messages().create(params);
        } catch (RuntimeException ex) {
            throw new ExternalServiceException("Anthropic text generation failed: " + ex.getMessage(), ex);
        }

        String content = extractText(response);
        if (!StringUtils.hasText(content)) {
            throw new ExternalServiceException("Anthropic returned an empty response");
        }

        logUsage(response, content.length());
        return new OpenAiGenerationResult(content, ContentGenerationMode.REAL, "anthropic:" + model, model);
    }

    private String extractText(Message response) {
        StringJoiner joiner = new StringJoiner("\n");
        for (ContentBlock block : response.content()) {
            block.text().ifPresent(text -> joiner.add(text.text()));
        }
        return joiner.toString().trim();
    }

    private void logUsage(Message response, int contentLength) {
        try {
            long inputTokens = response.usage().inputTokens();
            long outputTokens = response.usage().outputTokens();
            log.info(
                "event=anthropic_text_generation_success model={} inputTokens={} outputTokens={} contentLength={}",
                model, inputTokens, outputTokens, contentLength
            );
        } catch (RuntimeException ex) {
            log.info("event=anthropic_text_generation_success model={} contentLength={}", model, contentLength);
        }
    }
}
