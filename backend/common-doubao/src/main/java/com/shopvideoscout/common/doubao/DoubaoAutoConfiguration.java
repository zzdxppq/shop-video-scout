package com.shopvideoscout.common.doubao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Auto-configuration for Doubao client.
 * Story 5.3: 发布辅助服务
 *
 * Enables when doubao.api-key property is set.
 */
@Configuration
@EnableRetry
@EnableConfigurationProperties(DoubaoProperties.class)
@ConditionalOnProperty(prefix = "doubao", name = "api-key")
public class DoubaoAutoConfiguration {

    /**
     * Create RestTemplate with configured timeout.
     */
    @Bean
    @ConditionalOnMissingBean(name = "doubaoRestTemplate")
    public RestTemplate doubaoRestTemplate(DoubaoProperties properties, RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    /**
     * Create Doubao client bean.
     */
    @Bean
    @ConditionalOnMissingBean
    public DoubaoClient doubaoClient(DoubaoProperties properties,
                                      RestTemplate doubaoRestTemplate,
                                      ObjectMapper objectMapper) {
        return new DoubaoClient(properties, doubaoRestTemplate, objectMapper);
    }
}
