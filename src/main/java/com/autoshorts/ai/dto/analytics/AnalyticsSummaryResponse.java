package com.autoshorts.ai.dto.analytics;

import java.util.List;

/**
 * Aggregated analytics for the current user over the requested window.
 *
 * @param windowDays            range used for the timeseries
 * @param totalJobs             all-time job count
 * @param completedJobs         all-time COMPLETED count
 * @param failedJobs            all-time FAILED count
 * @param publishedCount        all-time PUBLISHED count
 * @param successRate           completed / total (0..1)
 * @param totalEstimatedCostCredits sum of estimated_cost_credits across all jobs
 * @param jobsByStatus          counts grouped by job status
 * @param publishByStatus       counts grouped by publish status
 * @param publishedByPlatform   PUBLISHED counts grouped by platform
 * @param timeseries            per-day jobs/completed/cost over the window
 */
public record AnalyticsSummaryResponse(
    int windowDays,
    long totalJobs,
    long completedJobs,
    long failedJobs,
    long publishedCount,
    double successRate,
    long totalEstimatedCostCredits,
    List<AnalyticsCount> jobsByStatus,
    List<AnalyticsCount> publishByStatus,
    List<AnalyticsCount> publishedByPlatform,
    List<AnalyticsDailyPoint> timeseries
) {
}
