package com.shopvideoscout.user.dto;

import lombok.Data;

/**
 * Request DTO for sending verification code.
 */
@Data
public class SendCodeRequest {

    /**
     * Phone number to send verification code to.
     * Must be valid Chinese mainland phone number (11 digits, starts with 1).
     */
    private String phone;
}
