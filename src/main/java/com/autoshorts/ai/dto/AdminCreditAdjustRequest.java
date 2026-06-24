package com.autoshorts.ai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminCreditAdjustRequest {

    /** Positive to grant, negative to deduct. */
    @NotNull(message = "amount is required")
    private Integer amount;

    @Size(max = 255)
    private String reason;

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
