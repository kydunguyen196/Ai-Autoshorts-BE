package com.autoshorts.ai.dto;

import com.autoshorts.ai.entity.Notification;

import java.time.Instant;
import java.util.UUID;

public class NotificationResponse {

    private UUID id;
    private String type;
    private String title;
    private String message;
    private String dataJson;
    private boolean read;
    private Instant readAt;
    private Instant createdAt;

    public static NotificationResponse from(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.id = n.getId();
        r.type = n.getType() != null ? n.getType().name() : null;
        r.title = n.getTitle();
        r.message = n.getMessage();
        r.dataJson = n.getDataJson();
        r.read = n.getReadAt() != null;
        r.readAt = n.getReadAt();
        r.createdAt = n.getCreatedAt();
        return r;
    }

    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getDataJson() {
        return dataJson;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
