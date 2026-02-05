package com.shopvideoscout.publish;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Publish Service Application.
 * Handles video publishing and export.
 * Story 5.3: 发布辅助服务
 */
@SpringBootApplication(scanBasePackages = {
        "com.shopvideoscout.publish",
        "com.shopvideoscout.common",
        "com.shopvideoscout.security",
        "com.shopvideoscout.mybatis"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.shopvideoscout.publish.client")
public class PublishServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PublishServiceApplication.class, args);
    }
}
