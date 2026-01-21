package com.shopvideoscout.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * AI Service Application.
 * Handles AI-powered video analysis and script generation.
 */
@SpringBootApplication(scanBasePackages = {
        "com.shopvideoscout.ai",
        "com.shopvideoscout.common",
        "com.shopvideoscout.security",
        "com.shopvideoscout.mybatis"
})
@EnableDiscoveryClient
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
