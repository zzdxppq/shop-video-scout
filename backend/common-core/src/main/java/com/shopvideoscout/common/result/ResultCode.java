package com.shopvideoscout.common.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Standard result codes for API responses.
 */
@Getter
@RequiredArgsConstructor
public enum ResultCode {

    // Success
    SUCCESS(200, "Success"),

    // Client Errors (4xx)
    BAD_REQUEST(400, "Bad request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Resource not found"),
    VALIDATION_ERROR(422, "Validation error"),

    // Server Errors (5xx)
    INTERNAL_ERROR(500, "Internal server error"),
    SERVICE_UNAVAILABLE(503, "Service unavailable"),
    GATEWAY_TIMEOUT(504, "Gateway timeout"),

    // Rate Limiting
    RATE_LIMIT_EXCEEDED(429, "请求过于频繁，请稍后重试"),

    // Account Locked
    ACCOUNT_LOCKED(423, "账号已被锁定"),

    // Business Errors (1xxx)
    USER_NOT_FOUND(1001, "User not found"),
    TASK_NOT_FOUND(1002, "Task not found"),
    VIDEO_NOT_FOUND(1003, "Video not found"),
    INVALID_FILE_FORMAT(1004, "Invalid file format"),
    FILE_SIZE_EXCEEDED(1005, "File size exceeded"),
    VIDEO_DURATION_EXCEEDED(1006, "Video duration exceeded"),
    VIDEO_COUNT_EXCEEDED(1007, "Video count exceeded"),
    AI_SERVICE_ERROR(1008, "AI service error"),
    AI_SERVICE_TIMEOUT(1009, "AI service timeout"),

    // Auth Errors (2xxx)
    INVALID_PHONE_FORMAT(2001, "请输入正确的手机号"),
    SMS_CODE_INVALID(2002, "验证码错误，请重新输入"),
    SMS_CODE_EXPIRED(2003, "验证码已过期，请重新获取"),
    SMS_SEND_RATE_LIMITED(2004, "请求过于频繁，请60秒后重试"),
    SMS_SERVICE_UNAVAILABLE(2005, "短信服务暂时不可用，请稍后重试"),
    INVALID_TOKEN(2006, "无效的令牌"),
    TOKEN_EXPIRED(2007, "令牌已过期"),
    REFRESH_TOKEN_INVALID(2008, "刷新令牌无效"),
    INVALID_CODE_FORMAT(2009, "请输入6位验证码");

    private final int code;
    private final String message;
}
