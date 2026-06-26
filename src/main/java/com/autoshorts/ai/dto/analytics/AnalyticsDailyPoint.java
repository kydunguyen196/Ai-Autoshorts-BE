package com.autoshorts.ai.dto.analytics;

/** One day in the analytics timeseries. {@code date} is ISO-8601 (yyyy-MM-dd). */
public record AnalyticsDailyPoint(String date, long jobs, long completed, long costCredits) {
}
