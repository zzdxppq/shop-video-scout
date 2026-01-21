package com.shopvideoscout.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for send verification code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendCodeResponse {

    /**
     * Indicates if the code was sent successfully.
     */
    private boolean success;

    /**
     * Message describing the result.
     */
    private String message;

    /**
     * Time in seconds until the code expires.
     */
    private Integer expiresIn;

    public static SendCodeResponse success() {
        return new SendCodeResponse(true, "验证码已发送", 300);
    }
}
