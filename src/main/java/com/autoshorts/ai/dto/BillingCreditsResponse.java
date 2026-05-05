package com.autoshorts.ai.dto;

public class BillingCreditsResponse {

    private Integer creditsBalance;
    private Integer lowCreditThreshold;

    public BillingCreditsResponse() {
    }

    public BillingCreditsResponse(Integer creditsBalance, Integer lowCreditThreshold) {
        this.creditsBalance = creditsBalance;
        this.lowCreditThreshold = lowCreditThreshold;
    }

    public Integer getCreditsBalance() {
        return creditsBalance;
    }

    public void setCreditsBalance(Integer creditsBalance) {
        this.creditsBalance = creditsBalance;
    }

    public Integer getLowCreditThreshold() {
        return lowCreditThreshold;
    }

    public void setLowCreditThreshold(Integer lowCreditThreshold) {
        this.lowCreditThreshold = lowCreditThreshold;
    }
}
