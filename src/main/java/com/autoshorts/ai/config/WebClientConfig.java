package com.autoshorts.ai.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
            .responseTimeout(Duration.ofSeconds(60))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(60))
                .addHandlerLast(new WriteTimeoutHandler(60)));

        return WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
            .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
