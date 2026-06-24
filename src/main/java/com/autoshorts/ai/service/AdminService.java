package com.autoshorts.ai.service;

import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.dto.AdminOverviewResponse;
import com.autoshorts.ai.dto.AdminUserResponse;
import com.autoshorts.ai.dto.AdminUserUpdateRequest;
import com.autoshorts.ai.dto.AppSettingResponse;
import com.autoshorts.ai.dto.NewsItemResponse;
import com.autoshorts.ai.dto.NewsSourceResponse;
import com.autoshorts.ai.dto.PagedResponse;
import com.autoshorts.ai.dto.VideoJobResponse;
import com.autoshorts.ai.entity.AppUser;
import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.exception.ResourceNotFoundException;
import com.autoshorts.ai.orchestration.NewsIngestionScheduler;
import com.autoshorts.ai.orchestration.TopicAutomationScheduler;
import com.autoshorts.ai.repository.AppUserRepository;
import com.autoshorts.ai.repository.NewsSourceRepository;
import com.autoshorts.ai.repository.VideoJobRepository;
import com.autoshorts.ai.util.VideoJobMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminService {

    private final AppUserRepository appUserRepository;
    private final VideoJobRepository videoJobRepository;
    private final NewsSourceRepository newsSourceRepository;
    private final com.autoshorts.ai.repository.NewsItemRepository newsItemRepository;
    private final BillingService billingService;
    private final SettingsService settingsService;
    private final AppProperties appProperties;
    private final VideoJobService videoJobService;
    private final VideoJobDispatchService videoJobDispatchService;

    public AdminService(
        AppUserRepository appUserRepository,
        VideoJobRepository videoJobRepository,
        NewsSourceRepository newsSourceRepository,
        com.autoshorts.ai.repository.NewsItemRepository newsItemRepository,
        BillingService billingService,
        SettingsService settingsService,
        AppProperties appProperties,
        VideoJobService videoJobService,
        VideoJobDispatchService videoJobDispatchService
    ) {
        this.appUserRepository = appUserRepository;
        this.videoJobRepository = videoJobRepository;
        this.newsSourceRepository = newsSourceRepository;
        this.newsItemRepository = newsItemRepository;
        this.billingService = billingService;
        this.settingsService = settingsService;
        this.appProperties = appProperties;
        this.videoJobService = videoJobService;
        this.videoJobDispatchService = videoJobDispatchService;
    }

    @Transactional(readOnly = true)
    public AdminOverviewResponse overview() {
        AdminOverviewResponse r = new AdminOverviewResponse();
        long total = appUserRepository.count();
        r.setTotalUsers(total);
        r.setEnabledUsers(appUserRepository.findAll().stream().filter(AppUser::isEnabled).count());
        r.setJobsPending(videoJobRepository.countByStatus(JobStatus.PENDING));
        r.setJobsProcessing(videoJobRepository.countByStatus(JobStatus.PROCESSING));
        r.setJobsCompleted(videoJobRepository.countByStatus(JobStatus.COMPLETED));
        r.setJobsFailed(videoJobRepository.countByStatus(JobStatus.FAILED));
        Instant now = Instant.now();
        r.setJobsLast24h(videoJobRepository.countByCreatedAtAfter(now.minus(24, ChronoUnit.HOURS)));
        r.setJobsLast7d(videoJobRepository.countByCreatedAtAfter(now.minus(7, ChronoUnit.DAYS)));
        r.setNewsSourcesEnabled(newsSourceRepository.countByEnabledTrue());
        r.setSchedulerEnabled(settingsService.getBoolean(
            TopicAutomationScheduler.SETTING_ENABLED, appProperties.getScheduler().isEnabled()));
        r.setNewsEnabled(settingsService.getBoolean(
            NewsIngestionScheduler.SETTING_ENABLED, appProperties.getNews().isEnabled()));
        r.setQueueEnabled(appProperties.getQueue().isEnabled());

        Map<String, String> providers = new LinkedHashMap<>();
        providers.put("text", appProperties.getText().getProvider());
        providers.put("visual", appProperties.getVisual().getProvider());
        providers.put("tts", appProperties.getElevenlabs().getProvider());
        providers.put("storageMock", String.valueOf(appProperties.getStorage().isMock()));
        r.setProviderModes(providers);
        return r;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminUserResponse> listUsers(String search, int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, limit), 100));
        Page<AppUser> result = StringUtils.hasText(search)
            ? appUserRepository.findAllByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
                search.trim(), search.trim(), pageable)
            : appUserRepository.findAll(pageable);
        return PagedResponse.from(result.map(AdminUserResponse::from));
    }

    @Transactional
    public AdminUserResponse updateUser(UUID userId, AdminUserUpdateRequest request) {
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        return AdminUserResponse.from(appUserRepository.save(user));
    }

    @Transactional
    public void adjustCredits(UUID userId, int amount, String reason) {
        appUserRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        String note = StringUtils.hasText(reason) ? reason : "admin_adjustment";
        if (amount > 0) {
            billingService.creditCredits(userId, amount, note, null);
        } else if (amount < 0) {
            billingService.debitCredits(userId, -amount, note, null);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<VideoJobResponse> listJobs(JobStatus status, int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, limit), 100),
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        Page<com.autoshorts.ai.entity.VideoJob> result = status != null
            ? videoJobRepository.findByStatus(status, pageable)
            : videoJobRepository.findAll(pageable);
        return PagedResponse.from(result.map(VideoJobMapper::toResponse));
    }

    @Transactional
    public VideoJobResponse retryJob(UUID jobId) {
        com.autoshorts.ai.entity.VideoJob job = videoJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        VideoJobResponse response = VideoJobMapper.toResponse(videoJobService.resetFailedForRetry(jobId, job.getUserId()));
        videoJobDispatchService.publishOrMarkFailed(jobId, "admin_retry", "Admin retry dispatch failed before queue publish");
        return response;
    }

    @Transactional(readOnly = true)
    public java.util.List<AppSettingResponse> listSettings() {
        return settingsService.listAll().stream().map(AppSettingResponse::from).toList();
    }

    @Transactional
    public AppSettingResponse updateSetting(String key, String value, String valueType, String category, UUID adminUserId) {
        return AppSettingResponse.from(settingsService.update(key, value, valueType, category, adminUserId));
    }

    @Transactional(readOnly = true)
    public PagedResponse<NewsSourceResponse> listAllNewsSources(int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, limit), 100),
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return PagedResponse.from(newsSourceRepository.findAll(pageable).map(NewsSourceResponse::from));
    }

    @Transactional(readOnly = true)
    public PagedResponse<NewsItemResponse> listAllNewsItems(int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, limit), 100),
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return PagedResponse.from(newsItemRepository.findAll(pageable).map(NewsItemResponse::from));
    }
}
