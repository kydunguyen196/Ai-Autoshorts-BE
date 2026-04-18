package com.autoshorts.ai.client;

import com.autoshorts.ai.client.model.SynthesizedAudio;

public interface ElevenLabsClient {

    SynthesizedAudio synthesizeSpeech(String text, String requestedVoiceId, int durationSeconds);
}
