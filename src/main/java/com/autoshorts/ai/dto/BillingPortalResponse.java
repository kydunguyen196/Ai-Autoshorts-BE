package com.autoshorts.ai.dto;

public class BillingPortalResponse {

    private String url;
    private String mode;

    public BillingPortalResponse() {
    }

    public BillingPortalResponse(String url, String mode) {
        this.url = url;
        this.mode = mode;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
