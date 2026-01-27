package com.shopvideoscout.media.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * General configuration for media-service.
 */
@Configuration
public class MediaServiceConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder,
                                      VolcanoTtsProperties ttsProperties) {
        return builder
                .connectTimeout(Duration.ofMillis(ttsProperties.getTimeoutMs()))
                .readTimeout(Duration.ofMillis(ttsProperties.getTimeoutMs()))
                .build();
    }
}
