package com.autoshorts.ai.service;

import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.dto.FrontendBootstrapResponse;
import com.autoshorts.ai.entity.GenerationStep;
import com.autoshorts.ai.entity.JobStatus;
import com.autoshorts.ai.entity.TopicIdeaStatus;
import com.autoshorts.ai.repository.PromptTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

@Service
public class FrontendMetadataService {

    private static final List<String> BUILTIN_STYLE_KEYS = List.of(
        "motivation",
        "storytelling",
        "facts",
        "self-improvement"
    );

    private final PromptTemplateRepository promptTemplateRepository;
    private final PromptTemplateService promptTemplateService;
    private final AppProperties appProperties;

    public FrontendMetadataService(
        PromptTemplateRepository promptTemplateRepository,
        PromptTemplateService promptTemplateService,
        AppProperties appProperties
    ) {
        this.promptTemplateRepository = promptTemplateRepository;
        this.promptTemplateService = promptTemplateService;
        this.appProperties = appProperties;
    }

    public FrontendBootstrapResponse getBootstrapMetadata() {
        FrontendBootstrapResponse response = new FrontendBootstrapResponse();
        response.setSupportedStyles(resolveSupportedStyles());
        response.setVideoStatuses(toEnumNames(JobStatus.values()));
        response.setGenerationSteps(toEnumNames(GenerationStep.values()));
        response.setTopicStatuses(toEnumNames(TopicIdeaStatus.values()));

        FrontendBootstrapResponse.FrontendDefaults defaults = new FrontendBootstrapResponse.FrontendDefaults();
        defaults.setDefaultStyle(PromptTemplateService.DEFAULT_STYLE);
        defaults.setDefaultDurationSeconds(30);
        defaults.setMinDurationSeconds(10);
        defaults.setMaxDurationSeconds(120);
        defaults.setDefaultVideosPageSize(20);
        defaults.setMaxVideosPageSize(100);
        defaults.setDefaultTopicsPageSize(20);
        defaults.setMaxTopicsPageSize(200);
        defaults.setDefaultVoiceId(trimToNull(appProperties.getElevenlabs().getDefaultVoiceId()));
        response.setDefaults(defaults);
        return response;
    }

    private List<String> resolveSupportedStyles() {
        TreeSet<String> styles = new TreeSet<>();
        styles.addAll(BUILTIN_STYLE_KEYS);
        promptTemplateRepository.findDistinctActiveStyleKeys().stream()
            .filter(StringUtils::hasText)
            .map(promptTemplateService::normalizeStyle)
            .forEach(styles::add);
        return List.copyOf(styles);
    }

    private List<String> toEnumNames(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
