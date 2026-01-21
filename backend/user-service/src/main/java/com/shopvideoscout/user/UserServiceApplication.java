package com.shopvideoscout.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * User Service Application.
 * Handles user management and authentication.
 */
@SpringBootApplication(scanBasePackages = {
        "com.shopvideoscout.user",
        "com.shopvideoscout.common",
        "com.shopvideoscout.security",
        "com.shopvideoscout.mybatis"
})
@EnableDiscoveryClient
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
