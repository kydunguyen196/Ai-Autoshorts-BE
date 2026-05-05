package com.autoshorts.ai.dto;

import java.time.Instant;

public class BillingSubscriptionResponse {

    private String planKey;
    private String status;
    private Integer creditsBalance;
    private Instant updatedAt;

    public String getPlanKey() {
        return planKey;
    }

    public void setPlanKey(String planKey) {
        this.planKey = planKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCreditsBalance() {
        return creditsBalance;
    }

    public void setCreditsBalance(Integer creditsBalance) {
        this.creditsBalance = creditsBalance;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
