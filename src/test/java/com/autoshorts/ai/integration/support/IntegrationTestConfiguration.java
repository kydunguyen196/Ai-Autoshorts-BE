package com.autoshorts.ai.integration.support;

import com.autoshorts.ai.cache.VideoJobStateCache;
import com.autoshorts.ai.dto.VideoJobResponse;
import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import java.util.Optional;
import java.util.UUID;

@TestConfiguration
public class IntegrationTestConfiguration {

    @Bean
    @Primary
    public DeterministicFfmpegVideoComposer deterministicFfmpegVideoComposer() {
        return new DeterministicFfmpegVideoComposer();
    }

    @Bean
    @Primary
    public ConnectionFactory testRabbitConnectionFactory() {
        return new CachingConnectionFactory(new MockConnectionFactory());
    }

    @Bean
    @Primary
    public VideoJobStateCache testVideoJobStateCache() {
        return new VideoJobStateCache() {
            @Override
            public Optional<VideoJobResponse> get(UUID jobId) {
                return Optional.empty();
            }

            @Override
            public void put(VideoJobResponse response) {
                // deterministic no-op cache for integration tests
            }
        };
    }
}
