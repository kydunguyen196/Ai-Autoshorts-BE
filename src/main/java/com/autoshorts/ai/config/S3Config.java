package com.autoshorts.ai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "app.storage.mock", havingValue = "false", matchIfMissing = true)
public class S3Config {

    @Bean
    public S3Client s3Client(AppProperties appProperties) {
        AppProperties.Storage storage = appProperties.getStorage();

        var builder = S3Client.builder()
            .region(Region.of(storage.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                defaultIfBlank(storage.getAccessKey(), "minioadmin"),
                defaultIfBlank(storage.getSecretKey(), "minioadmin")
            )))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());

        if (StringUtils.hasText(storage.getEndpoint())) {
            builder.endpointOverride(URI.create(storage.getEndpoint()));
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(AppProperties appProperties) {
        AppProperties.Storage storage = appProperties.getStorage();

        var builder = S3Presigner.builder()
            .region(Region.of(storage.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                defaultIfBlank(storage.getAccessKey(), "minioadmin"),
                defaultIfBlank(storage.getSecretKey(), "minioadmin")
            )));

        if (StringUtils.hasText(storage.getEndpoint())) {
            builder.endpointOverride(URI.create(storage.getEndpoint()));
        }

        return builder.build();
    }

    private String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
