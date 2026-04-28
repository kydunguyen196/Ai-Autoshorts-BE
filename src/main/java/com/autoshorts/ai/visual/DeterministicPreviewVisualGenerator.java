package com.autoshorts.ai.visual;

import com.autoshorts.ai.client.model.GeneratedVisualImage;
import com.autoshorts.ai.entity.VisualGenerationMode;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class DeterministicPreviewVisualGenerator {

    private static final int DEFAULT_WIDTH = 1080;
    private static final int DEFAULT_HEIGHT = 1920;

    public GeneratedVisualImage generateMockImage(String prompt, int sceneIndex) {
        return generate(prompt, sceneIndex, VisualGenerationMode.MOCK, "mock_visual", "mock-visual-v1", null, null);
    }

    public GeneratedVisualImage generateFallbackImage(String prompt, int sceneIndex, String reason, String details) {
        return generate(prompt, sceneIndex, VisualGenerationMode.FALLBACK, "deterministic_visual", "fallback-v1", reason, details);
    }

    private GeneratedVisualImage generate(
        String prompt,
        int sceneIndex,
        VisualGenerationMode mode,
        String provider,
        String modelId,
        String reason,
        String details
    ) {
        int seed = (prompt == null ? 0 : prompt.hashCode()) + (sceneIndex * 31);
        byte[] png = renderPng(prompt, sceneIndex, seed);
        return new GeneratedVisualImage(
            png,
            "png",
            "image/png",
            mode,
            provider,
            modelId,
            0L,
            reason,
            details
        );
    }

    private byte[] renderPng(String prompt, int sceneIndex, int seed) {
        BufferedImage image = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Random random = new Random(seed);
            Color top = colorFromSeed(seed, 0.72f);
            Color bottom = colorFromSeed(seed + 17, 0.54f);
            g.setPaint(new GradientPaint(0, 0, top, 0, DEFAULT_HEIGHT, bottom));
            g.fillRect(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT);

            drawBackdropShapes(g, random);
            drawTitleCard(g, prompt, sceneIndex);
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render deterministic preview image", ex);
        }
    }

    private void drawBackdropShapes(Graphics2D g, Random random) {
        for (int i = 0; i < 10; i++) {
            int width = 140 + random.nextInt(380);
            int height = 90 + random.nextInt(260);
            int x = random.nextInt(Math.max(1, DEFAULT_WIDTH - width));
            int y = random.nextInt(Math.max(1, DEFAULT_HEIGHT - height));
            int alpha = 35 + random.nextInt(70);

            g.setColor(new Color(255, 255, 255, alpha));
            g.fillRoundRect(x, y, width, height, 48, 48);
        }
    }

    private void drawTitleCard(Graphics2D g, String prompt, int sceneIndex) {
        int cardWidth = 920;
        int cardHeight = 520;
        int x = (DEFAULT_WIDTH - cardWidth) / 2;
        int y = (DEFAULT_HEIGHT - cardHeight) / 2;

        g.setColor(new Color(10, 14, 26, 180));
        g.fill(new RoundRectangle2D.Double(x, y, cardWidth, cardHeight, 42, 42));

        g.setColor(new Color(255, 255, 255, 205));
        g.setStroke(new BasicStroke(2.2f));
        g.draw(new RoundRectangle2D.Double(x, y, cardWidth, cardHeight, 42, 42));

        g.setColor(new Color(230, 235, 255, 230));
        g.setFont(new Font("SansSerif", Font.BOLD, 46));
        g.drawString("AI Scene " + (sceneIndex + 1), x + 48, y + 92);

        g.setFont(new Font("SansSerif", Font.PLAIN, 36));
        List<String> lines = wrapText(prompt, 38, 7);
        int lineY = y + 170;
        for (String line : lines) {
            g.drawString(line, x + 48, lineY);
            lineY += 56;
        }
    }

    private List<String> wrapText(String text, int maxCharsPerLine, int maxLines) {
        String source = sanitizeText(text);
        String[] words = source.split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= maxCharsPerLine) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
            if (lines.size() >= maxLines) {
                break;
            }
        }
        if (current.length() > 0 && lines.size() < maxLines) {
            lines.add(current.toString());
        }
        if (lines.isEmpty()) {
            lines.add("Story-driven visual scene");
        }
        if (lines.size() == maxLines && words.length > 0) {
            int last = lines.size() - 1;
            lines.set(last, trimEllipsis(lines.get(last), maxCharsPerLine));
        }
        return lines;
    }

    private String sanitizeText(String text) {
        if (text == null || text.isBlank()) {
            return "Story-driven visual scene for short-form ad";
        }
        return text
            .replaceAll("[\\r\\n\\t]+", " ")
            .replaceAll("\\s+", " ")
            .replaceAll("[^\\p{L}\\p{N} ,.!?'-]", " ")
            .trim();
    }

    private String trimEllipsis(String line, int maxChars) {
        if (line.length() <= maxChars - 1) {
            return line + "…";
        }
        return line.substring(0, Math.max(1, maxChars - 1)).trim() + "…";
    }

    private Color colorFromSeed(int seed, float brightness) {
        Random random = new Random(seed * 1_003L);
        float hue = random.nextFloat();
        float saturation = 0.45f + (random.nextFloat() * 0.35f);
        return Color.getHSBColor(hue, saturation, Math.max(0.35f, Math.min(1f, brightness)));
    }
}
