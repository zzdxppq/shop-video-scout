package com.shopvideoscout.task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Task Service Application.
 * Handles video production task management.
 */
@SpringBootApplication(scanBasePackages = {
        "com.shopvideoscout.task",
        "com.shopvideoscout.common",
        "com.shopvideoscout.security",
        "com.shopvideoscout.mybatis"
})
@EnableDiscoveryClient
public class TaskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
    }
}
