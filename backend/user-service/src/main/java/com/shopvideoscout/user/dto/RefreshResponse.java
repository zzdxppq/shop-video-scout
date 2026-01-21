package com.shopvideoscout.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for token refresh.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshResponse {

    /**
     * New JWT access token.
     */
    private String accessToken;

    /**
     * Access token expiration time in seconds.
     */
    private Long expiresIn;
}
