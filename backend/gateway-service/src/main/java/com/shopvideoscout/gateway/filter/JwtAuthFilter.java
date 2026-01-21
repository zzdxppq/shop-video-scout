package com.shopvideoscout.gateway.filter;

import com.shopvideoscout.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * JWT Authentication filter for Gateway.
 * Validates JWT tokens on non-whitelisted routes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Whitelist paths that don't require authentication.
     */
    @Value("${gateway.auth.whitelist:/api/v1/auth/**,/actuator/**}")
    private List<String> whitelist;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Check if path is whitelisted
        if (isWhitelisted(path)) {
            log.debug("Path {} is whitelisted, skipping JWT validation", path);
            return chain.filter(exchange);
        }

        // Extract token from Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = jwtUtils.extractTokenFromHeader(authHeader);

        if (token == null) {
            log.warn("Missing Authorization header for path: {}", path);
            return unauthorized(exchange, "Missing Authorization header");
        }

        // Validate token
        if (!jwtUtils.validateToken(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            return unauthorized(exchange, "Invalid or expired token");
        }

        // Extract user info and add to headers for downstream services
        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            String phone = jwtUtils.getPhoneFromToken(token);

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-User-Phone", phone)
                    .build();

            log.debug("JWT validated for user {} on path {}", userId, path);
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (Exception e) {
            log.error("Error extracting claims from token: {}", e.getMessage());
            return unauthorized(exchange, "Invalid token claims");
        }
    }

    /**
     * Check if the path matches any whitelist pattern.
     */
    private boolean isWhitelisted(String path) {
        return whitelist.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * Return 401 Unauthorized response.
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");

        String body = String.format("{\"code\":401,\"message\":\"%s\"}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        // Run after rate limiting but before other filters
        return -100;
    }
}
