package com.shopvideoscout.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Qwen-VL API.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "qwen.vl")
public class QwenVlConfig {

    /**
     * DashScope API base URL.
     */
    private String baseUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    /**
     * DashScope API key.
     */
    private String apiKey;

    /**
     * Model name to use.
     */
    private String model = "qwen-vl-max";

    /**
     * Request timeout in seconds.
     */
    private int timeoutSeconds = 30;

    /**
     * Maximum retry attempts on timeout.
     */
    private int maxRetryAttempts = 3;

    /**
     * Retry delay in milliseconds.
     */
    private long retryDelayMs = 1000;
}
