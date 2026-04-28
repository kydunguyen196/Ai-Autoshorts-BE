package com.autoshorts.ai.client;

public interface AiClient {

    ChatCompletionResult chatCompletion(String systemPrompt, String userPrompt, Double temperature);

    ImageGenerationResult generateImage(String prompt, String model, String size);

    SpeechSynthesisResult synthesizeSpeech(String input, String model, String voice, String responseFormat);

    record ChatCompletionResult(String content, String model) {
    }

    record ImageGenerationResult(byte[] data, String model) {
    }

    record SpeechSynthesisResult(byte[] data, String model, String voice, String responseFormat) {
    }
}
