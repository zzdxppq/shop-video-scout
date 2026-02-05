package com.shopvideoscout.common.doubao;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Doubao (Volcengine) API client.
 * Story 5.3: 发布辅助服务
 */
@Data
@ConfigurationProperties(prefix = "doubao")
public class DoubaoProperties {

    /**
     * Doubao API key (Volcengine access key).
     */
    private String apiKey;

    /**
     * Doubao API endpoint URL.
     */
    private String endpoint = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";

    /**
     * Model name to use.
     */
    private String model = "doubao-pro-32k";

    /**
     * Request timeout in seconds.
     */
    private int timeoutSeconds = 30;

    /**
     * Default temperature for generation (0.0-2.0).
     */
    private double defaultTemperature = 0.7;

    /**
     * Maximum retry attempts.
     */
    private int maxRetries = 3;

    /**
     * Initial backoff interval in milliseconds.
     */
    private long initialBackoffMs = 1000;

    /**
     * Backoff multiplier.
     */
    private double backoffMultiplier = 2.0;
}
