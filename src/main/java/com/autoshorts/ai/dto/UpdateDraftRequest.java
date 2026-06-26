package com.autoshorts.ai.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Edits applied to an AWAITING_REVIEW draft before finalizing the render (Phase 5).
 * All fields optional — only provided fields are updated.
 */
public class UpdateDraftRequest {

    @Size(max = 20000, message = "scriptText must be <= 20000 characters")
    private String scriptText;

    @Size(max = 5000, message = "captionText must be <= 5000 characters")
    private String captionText;

    @Size(max = 2000, message = "hookText must be <= 2000 characters")
    private String hookText;

    @Size(max = 2000, message = "ctaText must be <= 2000 characters")
    private String ctaText;

    @Size(max = 120, message = "voiceId must be <= 120 characters")
    private String voiceId;

    private UUID templateId;

    public String getScriptText() {
        return scriptText;
    }

    public void setScriptText(String scriptText) {
        this.scriptText = scriptText;
    }

    public String getCaptionText() {
        return captionText;
    }

    public void setCaptionText(String captionText) {
        this.captionText = captionText;
    }

    public String getHookText() {
        return hookText;
    }

    public void setHookText(String hookText) {
        this.hookText = hookText;
    }

    public String getCtaText() {
        return ctaText;
    }

    public void setCtaText(String ctaText) {
        this.ctaText = ctaText;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }
}
