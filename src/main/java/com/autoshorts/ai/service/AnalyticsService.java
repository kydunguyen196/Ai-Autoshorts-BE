package com.autoshorts.ai.service;

import com.autoshorts.ai.dto.analytics.AnalyticsCount;
import com.autoshorts.ai.dto.analytics.AnalyticsDailyPoint;
import com.autoshorts.ai.dto.analytics.AnalyticsSummaryResponse;
import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.PublishStatus;
import com.autoshorts.ai.repository.VideoJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Live analytics aggregation over a user's video jobs (Phase 4). Aggregates are computed on read
 * from existing columns (status, publish status/platform, estimated cost credits, created_at) — no
 * separate materialized table, which keeps the numbers always-consistent for the current scale.
 */
@Service
public class AnalyticsService {

    private static final int MAX_WINDOW_DAYS = 365;
    private static final int MIN_WINDOW_DAYS = 1;

    private final VideoJobRepository videoJobRepository;

    public AnalyticsService(VideoJobRepository videoJobRepository) {
        this.videoJobRepository = videoJobRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(UUID userId, int requestedWindowDays) {
        int windowDays = Math.max(MIN_WINDOW_DAYS, Math.min(MAX_WINDOW_DAYS, requestedWindowDays));

        List<AnalyticsCount> jobsByStatus = toCounts(videoJobRepository.countByStatusForUser(userId));
        List<AnalyticsCount> publishByStatus = toCounts(videoJobRepository.countByPublishStatusForUser(userId));
        List<AnalyticsCount> publishedByPlatform =
            toCounts(videoJobRepository.countPublishedByPlatformForUser(userId, PublishStatus.PUBLISHED));

        long totalJobs = jobsByStatus.stream().mapToLong(AnalyticsCount::count).sum();
        long completedJobs = countFor(jobsByStatus, JobStatus.COMPLETED.name());
        long failedJobs = countFor(jobsByStatus, JobStatus.FAILED.name());
        long publishedCount = countFor(publishByStatus, PublishStatus.PUBLISHED.name());
        double successRate = totalJobs == 0 ? 0.0 : round2((double) completedJobs / totalJobs);
        long totalCost = videoJobRepository.sumEstimatedCostCreditsForUser(userId);

        List<AnalyticsDailyPoint> timeseries = buildTimeseries(userId, windowDays);

        return new AnalyticsSummaryResponse(
            windowDays,
            totalJobs,
            completedJobs,
            failedJobs,
            publishedCount,
            successRate,
            totalCost,
            jobsByStatus,
            publishByStatus,
            publishedByPlatform,
            timeseries
        );
    }

    private List<AnalyticsDailyPoint> buildTimeseries(UUID userId, int windowDays) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDay = today.minusDays(windowDays - 1L);
        Instant since = startDay.atStartOfDay(ZoneOffset.UTC).toInstant();

        // Seed every day in the window so the chart has no gaps.
        Map<LocalDate, long[]> byDay = new LinkedHashMap<>(); // [jobs, completed, cost]
        for (LocalDate day = startDay; !day.isAfter(today); day = day.plusDays(1)) {
            byDay.put(day, new long[] {0, 0, 0});
        }

        for (Object[] row : videoJobRepository.findCreatedCostStatusSince(userId, since)) {
            Instant createdAt = (Instant) row[0];
            if (createdAt == null) {
                continue;
            }
            LocalDate day = createdAt.atZone(ZoneOffset.UTC).toLocalDate();
            long[] bucket = byDay.get(day);
            if (bucket == null) {
                continue;
            }
            long cost = row[1] == null ? 0L : ((Number) row[1]).longValue();
            boolean completed = row[2] == JobStatus.COMPLETED;
            bucket[0] += 1;
            bucket[1] += completed ? 1 : 0;
            bucket[2] += cost;
        }

        List<AnalyticsDailyPoint> points = new ArrayList<>(byDay.size());
        byDay.forEach((day, bucket) ->
            points.add(new AnalyticsDailyPoint(day.toString(), bucket[0], bucket[1], bucket[2])));
        return points;
    }

    private List<AnalyticsCount> toCounts(List<Object[]> rows) {
        List<AnalyticsCount> counts = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            String key = row[0] == null ? "unknown" : String.valueOf(row[0]);
            long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
            counts.add(new AnalyticsCount(key, count));
        }
        return counts;
    }

    private long countFor(List<AnalyticsCount> counts, String key) {
        return counts.stream()
            .filter(c -> key.equalsIgnoreCase(c.key()))
            .mapToLong(AnalyticsCount::count)
            .sum();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // Retained for clarity that windows are day-aligned.
    @SuppressWarnings("unused")
    private long daysBetween(Instant a, Instant b) {
        return ChronoUnit.DAYS.between(a, b);
    }
}
