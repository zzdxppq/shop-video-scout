package com.shopvideoscout.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Doubao (豆包) API.
 * Used for script generation via large language model.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "doubao")
public class DoubaoConfig {

    /**
     * Doubao API base URL.
     */
    private String baseUrl = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";

    /**
     * Doubao API key.
     */
    private String apiKey;

    /**
     * Model endpoint ID (e.g., ep-xxx).
     */
    private String endpointId;

    /**
     * Request timeout in seconds (60s for script generation).
     */
    private int timeoutSeconds = 60;

    /**
     * Maximum retry attempts on timeout.
     */
    private int maxRetryAttempts = 2;

    /**
     * Initial retry delay in milliseconds (exponential backoff: 5s, 10s).
     */
    private long retryDelayMs = 5000;

    /**
     * Default temperature for script generation.
     */
    private double defaultTemperature = 0.7;

    /**
     * Maximum temperature for regeneration attempts.
     */
    private double maxTemperature = 0.9;

    /**
     * Temperature increment per regeneration.
     */
    private double temperatureIncrement = 0.1;
}
