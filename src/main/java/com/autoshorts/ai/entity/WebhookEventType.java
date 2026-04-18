package com.autoshorts.ai.entity;

public enum WebhookEventType {
    JOB_GENERATED("job.generated"),
    JOB_APPROVED("job.approved"),
    JOB_REJECTED("job.rejected"),
    PUBLISH_REQUESTED("publish.requested"),
    PUBLISH_SUCCEEDED("publish.succeeded"),
    PUBLISH_FAILED("publish.failed"),
    JOB_COMPLETED("job.completed"),
    JOB_FAILED("job.failed"),
    JOB_PUBLISHED("job.published"),
    JOB_PUBLISH_FAILED("job.publish_failed");

    private final String eventName;

    WebhookEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}
