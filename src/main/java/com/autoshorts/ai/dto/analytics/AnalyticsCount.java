package com.autoshorts.ai.dto.analytics;

/** A single labelled count (job status, publish status, or platform). */
public record AnalyticsCount(String key, long count) {
}
