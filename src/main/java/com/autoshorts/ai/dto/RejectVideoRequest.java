package com.autoshorts.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RejectVideoRequest {

    @NotBlank(message = "rejectionReason is required")
    @Size(max = 2000, message = "rejectionReason must be <= 2000 characters")
    private String rejectionReason;

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
