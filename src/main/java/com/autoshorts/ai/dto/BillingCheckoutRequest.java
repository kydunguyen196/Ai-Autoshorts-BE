package com.autoshorts.ai.dto;

import jakarta.validation.constraints.Size;

public class BillingCheckoutRequest {

    @Size(max = 50, message = "planKey must be <= 50 characters")
    private String planKey;

    public String getPlanKey() {
        return planKey;
    }

    public void setPlanKey(String planKey) {
        this.planKey = planKey;
    }
}
