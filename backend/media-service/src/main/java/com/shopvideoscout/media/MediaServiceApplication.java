package com.shopvideoscout.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Media Service Application.
 * Handles video upload, processing, and storage.
 */
@SpringBootApplication(scanBasePackages = {
        "com.shopvideoscout.media",
        "com.shopvideoscout.common",
        "com.shopvideoscout.security",
        "com.shopvideoscout.mybatis"
})
@EnableDiscoveryClient
public class MediaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaServiceApplication.class, args);
    }
}
