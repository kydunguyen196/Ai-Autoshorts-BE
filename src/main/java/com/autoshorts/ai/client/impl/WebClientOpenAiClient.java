package com.autoshorts.ai.client.impl;

import com.autoshorts.ai.client.OpenAiClient;
import com.autoshorts.ai.client.model.OpenAiGenerationResult;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.ContentGenerationMode;
import com.autoshorts.ai.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.openai.mock", havingValue = "false", matchIfMissing = true)
public class WebClientOpenAiClient implements OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientOpenAiClient.class);

    private final WebClient webClient;
    private final AppProperties appProperties;

    public WebClientOpenAiClient(WebClient.Builder webClientBuilder, AppProperties appProperties) {
        this.appProperties = appProperties;
        this.webClient = webClientBuilder
            .baseUrl(appProperties.getOpenai().getBaseUrl())
            .build();
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
            throw new ExternalServiceException("OPENAI_API_KEY is missing while OPENAI_MOCK=false");
        }

        log.info(
            "event=openai_request_start mode=REAL provider=openai model={} baseUrl={}",
            appProperties.getOpenai().getModel(),
            appProperties.getOpenai().getBaseUrl()
        );

        ChatCompletionRequest request = new ChatCompletionRequest(
            appProperties.getOpenai().getModel(),
            List.of(
                new Message("system", systemPrompt),
                new Message("user", userPrompt)
            ),
            0.85
        );

        ChatCompletionResponse response = webClient.post()
            .uri("/v1/chat/completions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + appProperties.getOpenai().getApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse ->
                clientResponse.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> Mono.error(new ExternalServiceException(
                        "OpenAI request failed: status=%s body=%s".formatted(clientResponse.statusCode(), body)
                    )))
            )
            .bodyToMono(ChatCompletionResponse.class)
            .block(Duration.ofSeconds(70));

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new ExternalServiceException("OpenAI returned an empty response");
        }

        String script = response.choices().get(0).message() != null
            ? response.choices().get(0).message().content()
            : null;

        if (!StringUtils.hasText(script)) {
            throw new ExternalServiceException("OpenAI returned empty script content");
        }

        String content = script.trim();
        log.info(
            "event=openai_request_success mode=REAL provider=openai model={} contentLength={}",
            appProperties.getOpenai().getModel(),
            content.length()
        );
        return new OpenAiGenerationResult(
            content,
            ContentGenerationMode.REAL,
            "openai",
            appProperties.getOpenai().getModel()
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

    private record ChatCompletionRequest(
        String model,
        List<Message> messages,
        Double temperature
    ) {
    }

    private record Message(String role, String content) {
    }

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }
}
