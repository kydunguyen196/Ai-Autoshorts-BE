package com.autoshorts.ai.dto;

import java.util.Map;

public class AdminOverviewResponse {

    private long totalUsers;
    private long enabledUsers;
    private long jobsPending;
    private long jobsProcessing;
    private long jobsCompleted;
    private long jobsFailed;
    private long jobsLast24h;
    private long jobsLast7d;
    private long newsSourcesEnabled;
    private boolean schedulerEnabled;
    private boolean newsEnabled;
    private boolean queueEnabled;
    private Map<String, String> providerModes;

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getEnabledUsers() {
        return enabledUsers;
    }

    public void setEnabledUsers(long enabledUsers) {
        this.enabledUsers = enabledUsers;
    }

    public long getJobsPending() {
        return jobsPending;
    }

    public void setJobsPending(long jobsPending) {
        this.jobsPending = jobsPending;
    }

    public long getJobsProcessing() {
        return jobsProcessing;
    }

    public void setJobsProcessing(long jobsProcessing) {
        this.jobsProcessing = jobsProcessing;
    }

    public long getJobsCompleted() {
        return jobsCompleted;
    }

    public void setJobsCompleted(long jobsCompleted) {
        this.jobsCompleted = jobsCompleted;
    }

    public long getJobsFailed() {
        return jobsFailed;
    }

    public void setJobsFailed(long jobsFailed) {
        this.jobsFailed = jobsFailed;
    }

    public long getJobsLast24h() {
        return jobsLast24h;
    }

    public void setJobsLast24h(long jobsLast24h) {
        this.jobsLast24h = jobsLast24h;
    }

    public long getJobsLast7d() {
        return jobsLast7d;
    }

    public void setJobsLast7d(long jobsLast7d) {
        this.jobsLast7d = jobsLast7d;
    }

    public long getNewsSourcesEnabled() {
        return newsSourcesEnabled;
    }

    public void setNewsSourcesEnabled(long newsSourcesEnabled) {
        this.newsSourcesEnabled = newsSourcesEnabled;
    }

    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }

    public void setSchedulerEnabled(boolean schedulerEnabled) {
        this.schedulerEnabled = schedulerEnabled;
    }

    public boolean isNewsEnabled() {
        return newsEnabled;
    }

    public void setNewsEnabled(boolean newsEnabled) {
        this.newsEnabled = newsEnabled;
    }

    public boolean isQueueEnabled() {
        return queueEnabled;
    }

    public void setQueueEnabled(boolean queueEnabled) {
        this.queueEnabled = queueEnabled;
    }

    public Map<String, String> getProviderModes() {
        return providerModes;
    }

    public void setProviderModes(Map<String, String> providerModes) {
        this.providerModes = providerModes;
    }
}
