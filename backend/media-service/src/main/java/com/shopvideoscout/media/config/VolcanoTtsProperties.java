package com.shopvideoscout.media.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Volcano Seed-TTS SDK configuration properties.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "volcano.tts")
public class VolcanoTtsProperties {

    private String apiUrl;
    private String appId;
    private String accessToken;
    private int sampleRate = 48000;
    private String format = "mp3";
    private int timeoutMs = 30000;
    private int maxTextLength = 5000;
}
