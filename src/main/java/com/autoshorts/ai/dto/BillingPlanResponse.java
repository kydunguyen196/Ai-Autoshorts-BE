package com.autoshorts.ai.dto;

public class BillingPlanResponse {

    private String planKey;
    private String name;
    private Integer monthlyCredits;
    private Integer monthlyPriceUsdCents;
    private String audience;

    public BillingPlanResponse() {
    }

    public BillingPlanResponse(String planKey, String name, Integer monthlyCredits, Integer monthlyPriceUsdCents, String audience) {
        this.planKey = planKey;
        this.name = name;
        this.monthlyCredits = monthlyCredits;
        this.monthlyPriceUsdCents = monthlyPriceUsdCents;
        this.audience = audience;
    }

    public String getPlanKey() {
        return planKey;
    }

    public void setPlanKey(String planKey) {
        this.planKey = planKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMonthlyCredits() {
        return monthlyCredits;
    }

    public void setMonthlyCredits(Integer monthlyCredits) {
        this.monthlyCredits = monthlyCredits;
    }

    public Integer getMonthlyPriceUsdCents() {
        return monthlyPriceUsdCents;
    }

    public void setMonthlyPriceUsdCents(Integer monthlyPriceUsdCents) {
        this.monthlyPriceUsdCents = monthlyPriceUsdCents;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }
}
