package com.autoshorts.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prompt_templates")
public class PromptTemplate {

    @Id
    private UUID id;

    @Column(name = "style_key", nullable = false, unique = true, length = 100)
    private String styleKey;

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "user_prompt_pattern", nullable = false, columnDefinition = "TEXT")
    private String userPromptPattern;

    @Column(name = "target_tone", nullable = false, length = 100)
    private String targetTone;

    @Column(name = "target_cta_style", nullable = false, length = 100)
    private String targetCtaStyle;

    @Column(name = "hook_patterns_json", columnDefinition = "TEXT")
    private String hookPatternsJson;

    @Column(name = "cta_patterns_json", columnDefinition = "TEXT")
    private String ctaPatternsJson;

    @Column(name = "structure_patterns_json", columnDefinition = "TEXT")
    private String structurePatternsJson;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean builtin = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStyleKey() {
        return styleKey;
    }

    public void setStyleKey(String styleKey) {
        this.styleKey = styleKey;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getUserPromptPattern() {
        return userPromptPattern;
    }

    public void setUserPromptPattern(String userPromptPattern) {
        this.userPromptPattern = userPromptPattern;
    }

    public String getTargetTone() {
        return targetTone;
    }

    public void setTargetTone(String targetTone) {
        this.targetTone = targetTone;
    }

    public String getTargetCtaStyle() {
        return targetCtaStyle;
    }

    public void setTargetCtaStyle(String targetCtaStyle) {
        this.targetCtaStyle = targetCtaStyle;
    }

    public String getHookPatternsJson() {
        return hookPatternsJson;
    }

    public void setHookPatternsJson(String hookPatternsJson) {
        this.hookPatternsJson = hookPatternsJson;
    }

    public String getCtaPatternsJson() {
        return ctaPatternsJson;
    }

    public void setCtaPatternsJson(String ctaPatternsJson) {
        this.ctaPatternsJson = ctaPatternsJson;
    }

    public String getStructurePatternsJson() {
        return structurePatternsJson;
    }

    public void setStructurePatternsJson(String structurePatternsJson) {
        this.structurePatternsJson = structurePatternsJson;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getBuiltin() {
        return builtin;
    }

    public void setBuiltin(Boolean builtin) {
        this.builtin = builtin;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
