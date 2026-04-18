package com.autoshorts.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class WebCorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
        AppProperties.Cors cors = appProperties.getWeb().getCors();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(clean(cors.getAllowedOrigins()));
        config.setAllowedMethods(clean(cors.getAllowedMethods()));
        config.setAllowedHeaders(clean(cors.getAllowedHeaders()));
        config.setExposedHeaders(clean(cors.getExposedHeaders()));
        config.setAllowCredentials(cors.isAllowCredentials());
        config.setMaxAge(cors.getMaxAgeSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> clean(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .toList();
    }
}
