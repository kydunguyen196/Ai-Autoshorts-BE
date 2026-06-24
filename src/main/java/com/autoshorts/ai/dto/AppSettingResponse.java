package com.autoshorts.ai.dto;

import com.autoshorts.ai.entity.AppSetting;

import java.time.Instant;

public class AppSettingResponse {

    private String key;
    private String value;
    private String valueType;
    private String category;
    private String description;
    private Instant updatedAt;

    public static AppSettingResponse from(AppSetting s) {
        AppSettingResponse r = new AppSettingResponse();
        r.key = s.getKey();
        r.value = s.getValue();
        r.valueType = s.getValueType();
        r.category = s.getCategory();
        r.description = s.getDescription();
        r.updatedAt = s.getUpdatedAt();
        return r;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getValueType() {
        return valueType;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
