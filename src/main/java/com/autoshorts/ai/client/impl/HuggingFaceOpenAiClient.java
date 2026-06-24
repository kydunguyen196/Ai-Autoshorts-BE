package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.OpenAiClient;
import com.autoshorts.ai.client.model.OpenAiGenerationResult;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.ContentGenerationMode;
import com.autoshorts.ai.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnExpression("'${app.openai.mock:false}' == 'false' && '${app.text.provider:openai}' == 'huggingface'")
public class HuggingFaceOpenAiClient implements OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceOpenAiClient.class);

    private final AppProperties appProperties;
    private final WebClient.Builder webClientBuilder;

    public HuggingFaceOpenAiClient(AppProperties appProperties, WebClient.Builder webClientBuilder) {
        this.appProperties = appProperties;
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public OpenAiGenerationResult generateShortVideoScript(String topic, String style, int durationSeconds) {
        return generateFromPrompts(
            """
                You are a creator scriptwriter for short vertical videos.
                Return only the final script text. No markdown.
                Keep it concise, high-retention, and spoken naturally.
                """,
            """
                Write a %d-second short-form video script.
                Topic: %s
                Style: %s

                Requirements:
                - Hook in first sentence
                - 3-5 concise points
                - Strong ending CTA
                - Speakable format suitable for TTS
                - Around %d seconds when spoken at normal speed
                """.formatted(durationSeconds, topic, style, durationSeconds)
        );
    }

    @Override
    public OpenAiGenerationResult generateFromPrompts(String systemPrompt, String userPrompt) {
        String model = resolveModel();
        String provider = resolveProvider();
        WebClient client = HuggingFaceSupport.webClient(webClientBuilder, appProperties);
        Map<String, Object> body = Map.of(
            "model", model + ":" + provider,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "temperature", 0.85
        );

        JsonNode response = client
            .post()
            .uri("/v1/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .onStatus(HuggingFaceSupport::isError, HuggingFaceSupport::errorFromResponse)
            .bodyToMono(JsonNode.class)
            .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)).filter(this::isTransientFailure))
            .block(Duration.ofMillis(Math.max(1000L, appProperties.getHuggingface().getRequestTimeoutMs())));

        String content = extractContent(response);
        log.info(
            "event=huggingface_text_generation_success provider={} model={} contentLength={}",
            provider,
            model,
            content.length()
        );
        return new OpenAiGenerationResult(content, ContentGenerationMode.REAL, "huggingface:" + provider, model);
    }

    private String extractContent(JsonNode response) {
        JsonNode contentNode = response == null ? null : response.at("/choices/0/message/content");
        if (contentNode == null || contentNode.isMissingNode() || !StringUtils.hasText(contentNode.asText())) {
            throw new ExternalServiceException("Hugging Face chat response is invalid: missing choices[0].message.content");
        }
        return contentNode.asText().trim();
    }

    private boolean isTransientFailure(Throwable throwable) {
        String message = throwable == null ? "" : throwable.getMessage();
        return message.contains("Failed to resolve")
            || message.contains("Connection reset")
            || message.contains("Connection timed out")
            || message.contains("PrematureCloseException");
    }

    private String resolveModel() {
        String model = HuggingFaceSupport.trimToNull(appProperties.getHuggingface().getTextModel());
        if (!StringUtils.hasText(model)) {
            throw new ExternalServiceException("Missing HF_TEXT_MODEL");
        }
        return model;
    }

    private String resolveProvider() {
        String provider = HuggingFaceSupport.trimToNull(appProperties.getHuggingface().getTextProvider());
        return StringUtils.hasText(provider) ? provider : "hf-inference";
    }
}
