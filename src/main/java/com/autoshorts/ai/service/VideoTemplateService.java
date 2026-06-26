package com.autoshorts.ai.service;

import com.autoshorts.ai.dto.VideoTemplateRequest;
import com.autoshorts.ai.dto.VideoTemplateResponse;
import com.autoshorts.ai.entity.VideoTemplate;
import com.autoshorts.ai.exception.ResourceNotFoundException;
import com.autoshorts.ai.repository.VideoTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class VideoTemplateService {

    private static final long MAX_TEMPLATES_PER_USER = 100;

    private final VideoTemplateRepository repository;

    public VideoTemplateService(VideoTemplateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<VideoTemplateResponse> list(UUID userId) {
        return repository.findByOwnerUserIdOrderByCreatedAtDesc(userId).stream()
            .map(VideoTemplateResponse::from)
            .toList();
    }

    @Transactional
    public VideoTemplateResponse create(UUID userId, VideoTemplateRequest request) {
        if (repository.countByOwnerUserId(userId) >= MAX_TEMPLATES_PER_USER) {
            throw new IllegalStateException("Template limit reached (" + MAX_TEMPLATES_PER_USER + ")");
        }
        VideoTemplate template = new VideoTemplate();
        template.setOwnerUserId(userId);
        apply(template, request);
        if (request.isMakeDefault()) {
            clearDefault(userId);
            template.setDefault(true);
        }
        return VideoTemplateResponse.from(repository.save(template));
    }

    @Transactional
    public VideoTemplateResponse update(UUID userId, UUID templateId, VideoTemplateRequest request) {
        VideoTemplate template = repository.findByIdAndOwnerUserId(templateId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));
        apply(template, request);
        if (request.isMakeDefault()) {
            clearDefault(userId);
            template.setDefault(true);
        }
        return VideoTemplateResponse.from(repository.save(template));
    }

    @Transactional
    public void delete(UUID userId, UUID templateId) {
        VideoTemplate template = repository.findByIdAndOwnerUserId(templateId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));
        repository.delete(template);
    }

    private void apply(VideoTemplate template, VideoTemplateRequest request) {
        template.setName(request.getName().trim());
        template.setDescription(trimToNull(request.getDescription()));
        template.setCaptionPosition(StringUtils.hasText(request.getCaptionPosition())
            ? request.getCaptionPosition().trim().toUpperCase() : "BOTTOM");
        template.setFontFamily(trimToNull(request.getFontFamily()));
        template.setPrimaryColor(trimToNull(request.getPrimaryColor()));
        template.setAccentColor(trimToNull(request.getAccentColor()));
    }

    private void clearDefault(UUID userId) {
        repository.findByOwnerUserIdOrderByCreatedAtDesc(userId).stream()
            .filter(VideoTemplate::isDefault)
            .forEach(existing -> {
                existing.setDefault(false);
                repository.save(existing);
            });
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
