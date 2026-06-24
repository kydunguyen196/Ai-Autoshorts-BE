package com.autoshorts.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminBroadcastRequest {

    @NotBlank(message = "title is required")
    @Size(max = 255)
    private String title;

    @Size(max = 4000)
    private String message;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
