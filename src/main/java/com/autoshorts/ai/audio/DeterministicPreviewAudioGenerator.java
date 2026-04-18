package com.autoshorts.ai.audio;

import com.autoshorts.ai.client.model.SynthesizedAudio;
import com.autoshorts.ai.entity.AudioGenerationMode;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@Component
public class DeterministicPreviewAudioGenerator {

    private static final int SAMPLE_RATE = 16_000;

    public SynthesizedAudio generateMockSpeech(String text, int durationSeconds) {
        byte[] wav = generateWav(text, durationSeconds, 0.35d);
        return new SynthesizedAudio(
            wav,
            "wav",
            "audio/wav",
            AudioGenerationMode.MOCK,
            "mock_elevenlabs",
            "deterministic_preview_voice",
            "deterministic_preview_voice",
            "deterministic_mock_v1",
            "wav_16000",
            0L,
            null,
            null
        );
    }

    public SynthesizedAudio generateFallbackSpeech(String text, int durationSeconds, String reason) {
        byte[] wav = generateWav(text, durationSeconds, 0.28d);
        return new SynthesizedAudio(
            wav,
            "wav",
            "audio/wav",
            AudioGenerationMode.FALLBACK,
            "deterministic_preview",
            reason,
            null,
            "deterministic_fallback_v1",
            "wav_16000",
            0L,
            reason,
            null
        );
    }

    private byte[] generateWav(String text, int durationSeconds, double amplitudeScale) {
        int seconds = Math.max(5, Math.min(durationSeconds, 60));
        int totalSamples = seconds * SAMPLE_RATE;
        int bytesPerSample = 2;
        int dataSize = totalSamples * bytesPerSample;

        int seed = text == null ? 0 : Math.abs(text.hashCode());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            writeString(dos, "RIFF");
            writeIntLE(dos, 36 + dataSize);
            writeString(dos, "WAVE");
            writeString(dos, "fmt ");
            writeIntLE(dos, 16);
            writeShortLE(dos, (short) 1);
            writeShortLE(dos, (short) 1);
            writeIntLE(dos, SAMPLE_RATE);
            writeIntLE(dos, SAMPLE_RATE * bytesPerSample);
            writeShortLE(dos, (short) bytesPerSample);
            writeShortLE(dos, (short) 16);
            writeString(dos, "data");
            writeIntLE(dos, dataSize);

            int segmentSize = SAMPLE_RATE / 8;
            for (int i = 0; i < totalSamples; i++) {
                int segment = i / segmentSize;
                int inSegment = i % segmentSize;
                double progress = inSegment / (double) segmentSize;

                double envelope = Math.sin(Math.PI * progress);
                if (segment % 7 == 6) {
                    envelope *= 0.18d;
                }

                double baseFreq = 150.0d + ((seed + (segment * 37)) % 70);
                double overtoneFreq = baseFreq * 1.97d;
                double shimmerFreq = 80.0d + ((seed + (segment * 17)) % 20);

                double carrier =
                    (Math.sin(2.0d * Math.PI * baseFreq * i / SAMPLE_RATE) * 0.70d)
                        + (Math.sin(2.0d * Math.PI * overtoneFreq * i / SAMPLE_RATE) * 0.22d)
                        + (Math.sin(2.0d * Math.PI * shimmerFreq * i / SAMPLE_RATE) * 0.08d);

                double sampleValue = carrier * envelope * (2_200.0d * amplitudeScale);
                short sample = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (int) Math.round(sampleValue)));
                writeShortLE(dos, sample);
            }
            dos.flush();
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate deterministic preview WAV audio", ex);
        }
    }

    private void writeString(DataOutputStream dos, String value) throws IOException {
        dos.writeBytes(value);
    }

    private void writeIntLE(DataOutputStream dos, int value) throws IOException {
        dos.writeByte(value & 0xFF);
        dos.writeByte((value >> 8) & 0xFF);
        dos.writeByte((value >> 16) & 0xFF);
        dos.writeByte((value >> 24) & 0xFF);
    }

    private void writeShortLE(DataOutputStream dos, short value) throws IOException {
        dos.writeByte(value & 0xFF);
        dos.writeByte((value >> 8) & 0xFF);
    }
}
