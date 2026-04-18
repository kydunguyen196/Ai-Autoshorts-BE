package com.autoshorts.ai.service;

import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.dto.FrontendBootstrapResponse;
import com.autoshorts.ai.repository.PromptTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FrontendMetadataServiceTest {

    @Mock
    private PromptTemplateRepository promptTemplateRepository;

    @Test
    void shouldBuildFrontendBootstrapMetadataWithNormalizedStylesAndDefaults() {
        when(promptTemplateRepository.findDistinctActiveStyleKeys())
            .thenReturn(List.of(" Motivation ", "custom_style", "facts", "  "));

        AppProperties appProperties = new AppProperties();
        appProperties.getElevenlabs().setDefaultVoiceId("voice_en_1");
        PromptTemplateService promptTemplateService = new PromptTemplateService(promptTemplateRepository);
        FrontendMetadataService service = new FrontendMetadataService(
            promptTemplateRepository,
            promptTemplateService,
            appProperties
        );

        FrontendBootstrapResponse response = service.getBootstrapMetadata();

        assertThat(response.getSupportedStyles())
            .containsExactly("custom-style", "facts", "motivation", "self-improvement", "storytelling");
        assertThat(response.getVideoStatuses()).contains("PENDING", "PROCESSING", "COMPLETED", "FAILED");
        assertThat(response.getGenerationSteps()).contains("QUEUED", "CONTENT_PREPARATION", "COMPLETED");
        assertThat(response.getTopicStatuses()).contains("PENDING", "PROCESSING", "USED", "FAILED");
        assertThat(response.getDefaults().getDefaultStyle()).isEqualTo("motivation");
        assertThat(response.getDefaults().getDefaultDurationSeconds()).isEqualTo(30);
        assertThat(response.getDefaults().getMinDurationSeconds()).isEqualTo(10);
        assertThat(response.getDefaults().getMaxDurationSeconds()).isEqualTo(120);
        assertThat(response.getDefaults().getDefaultVideosPageSize()).isEqualTo(20);
        assertThat(response.getDefaults().getMaxVideosPageSize()).isEqualTo(100);
        assertThat(response.getDefaults().getDefaultTopicsPageSize()).isEqualTo(20);
        assertThat(response.getDefaults().getMaxTopicsPageSize()).isEqualTo(200);
        assertThat(response.getDefaults().getDefaultVoiceId()).isEqualTo("voice_en_1");
    }
}
