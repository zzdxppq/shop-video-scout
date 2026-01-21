package com.shopvideoscout.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT configuration properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Secret key for signing JWT tokens.
     */
    private String secret = "shop-video-scout-jwt-secret-key-must-be-at-least-256-bits";

    /**
     * Token expiration time in seconds (default: 15 minutes per BR-2.2).
     */
    private long expiration = 900;

    /**
     * Token prefix (default: Bearer).
     */
    private String prefix = "Bearer ";

    /**
     * Header name for JWT token.
     */
    private String header = "Authorization";

    /**
     * Refresh token expiration time in seconds (default: 7 days).
     */
    private long refreshExpiration = 604800;
}
