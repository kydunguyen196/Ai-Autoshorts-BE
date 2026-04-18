package com.autoshorts.ai.service;

import com.autoshorts.ai.entity.PromptTemplate;
import com.autoshorts.ai.repository.PromptTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class PromptTemplateService {

    public static final String DEFAULT_STYLE = "motivation";

    private final PromptTemplateRepository promptTemplateRepository;

    public PromptTemplateService(PromptTemplateRepository promptTemplateRepository) {
        this.promptTemplateRepository = promptTemplateRepository;
    }

    public PromptTemplate resolveTemplate(String requestedStyle) {
        String normalizedStyle = normalizeStyle(requestedStyle);

        return promptTemplateRepository.findFirstByStyleKeyIgnoreCaseAndActiveTrue(normalizedStyle)
            .or(() -> promptTemplateRepository.findFirstByStyleKeyIgnoreCaseAndActiveTrue(DEFAULT_STYLE))
            .orElseGet(() -> fallbackTemplate(normalizedStyle));
    }

    public String normalizeStyle(String style) {
        if (!StringUtils.hasText(style)) {
            return DEFAULT_STYLE;
        }
        return style.trim()
            .toLowerCase()
            .replace('_', '-')
            .replace(' ', '-');
    }

    private PromptTemplate fallbackTemplate(String styleKey) {
        PromptTemplate template = new PromptTemplate();
        template.setId(UUID.nameUUIDFromBytes(("fallback:" + styleKey).getBytes(StandardCharsets.UTF_8)));
        template.setStyleKey(styleKey);
        template.setSystemPrompt("""
            You are an elite short-form content strategist.
            Create compelling TikTok-ready content optimized for retention.
            """);
        template.setUserPromptPattern("""
            Create short-form content.
            Topic: {{topic}}
            Style: {{style}}
            Duration: {{durationSeconds}}
            Target tone: {{targetTone}}
            CTA style: {{targetCtaStyle}}
            Selected hook strategy: {{hookPattern}}
            Selected CTA strategy: {{ctaPattern}}
            Selected structure strategy: {{structurePattern}}
            """);
        template.setTargetTone("high-energy");
        template.setTargetCtaStyle("follow_for_more");
        template.setHookPatternsJson("[\"curiosity-gap\",\"controversial-statement\",\"emotional-hook\",\"problem-hook\",\"story-hook\"]");
        template.setCtaPatternsJson("[\"follow-cta\",\"save-cta\",\"comment-cta\",\"reflection-cta\"]");
        template.setStructurePatternsJson("[\"mini-story\",\"problem-solution-insight\",\"narrative-lesson\",\"list-with-story-transitions\"]");
        template.setActive(true);
        template.setBuiltin(true);
        return template;
    }
}
