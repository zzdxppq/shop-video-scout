package com.shopvideoscout.user.dto;

import lombok.Data;

/**
 * Request DTO for phone + code login.
 */
@Data
public class LoginRequest {

    /**
     * Phone number.
     */
    private String phone;

    /**
     * 6-digit verification code.
     */
    private String code;
}
