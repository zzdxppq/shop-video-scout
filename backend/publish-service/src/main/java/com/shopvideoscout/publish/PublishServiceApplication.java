package com.shopvideoscout.publish;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Publish Service Application.
 * Handles video publishing and export.
 */
@SpringBootApplication(scanBasePackages = {
        "com.shopvideoscout.publish",
        "com.shopvideoscout.common",
        "com.shopvideoscout.security",
        "com.shopvideoscout.mybatis"
})
@EnableDiscoveryClient
public class PublishServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PublishServiceApplication.class, args);
    }
}
