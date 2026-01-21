package com.shopvideoscout.user.dto;

import lombok.Data;

/**
 * Request DTO for token refresh.
 */
@Data
public class RefreshRequest {

    /**
     * The refresh token to use for getting a new access token.
     */
    private String refreshToken;
}
