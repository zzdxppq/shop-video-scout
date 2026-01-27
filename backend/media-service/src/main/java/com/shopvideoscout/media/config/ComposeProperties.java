package com.shopvideoscout.media.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Compose workflow configuration properties.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "compose")
public class ComposeProperties {

    private String callbackUrl;
    private int progressTtlSeconds = 3600;
}
