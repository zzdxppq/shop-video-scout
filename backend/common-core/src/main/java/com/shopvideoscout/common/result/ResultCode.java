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
    TTS_SERVICE_ERROR(1010, "TTS配音服务异常"),
    TTS_SERVICE_TIMEOUT(1011, "配音生成超时"),
    INVALID_VOICE_TYPE(1012, "不支持的音色类型"),
    COMPOSE_TEXT_TOO_LONG(1013, "单次配音文本不能超过5000字"),
    TASK_STATUS_INVALID(1014, "任务状态不允许此操作"),
    TASK_ALREADY_COMPOSING(1015, "任务正在合成中，请勿重复提交"),
    SCRIPT_NOT_FOUND(1016, "脚本未找到"),

    // Auth Errors (2xxx)
    INVALID_PHONE_FORMAT(2001, "请输入正确的手机号"),
    SMS_CODE_INVALID(2002, "验证码错误，请重新输入"),
    SMS_CODE_EXPIRED(2003, "验证码已过期，请重新获取"),
    SMS_SEND_RATE_LIMITED(2004, "请求过于频繁，请60秒后重试"),
    SMS_SERVICE_UNAVAILABLE(2005, "短信服务暂时不可用，请稍后重试"),
    INVALID_TOKEN(2006, "无效的令牌"),
    TOKEN_EXPIRED(2007, "令牌已过期"),
    REFRESH_TOKEN_INVALID(2008, "刷新令牌无效"),
    INVALID_CODE_FORMAT(2009, "请输入6位验证码"),

    // Voice Clone Errors (1017-1022)
    VOICE_SAMPLE_NOT_FOUND(1017, "声音样本未找到"),
    VOICE_SAMPLE_LIMIT_EXCEEDED(1018, "声音样本数量已达上限（最多3个）"),
    VOICE_CLONE_FAILED(1019, "声音克隆失败，请确保样本清晰无杂音"),
    VOICE_CLONE_IN_PROGRESS(1020, "您的声音样本正在处理中，请稍后"),
    INVALID_AUDIO_FORMAT(1021, "不支持的音频格式，请上传MP3、WAV或M4A文件"),
    AUDIO_DURATION_INVALID(1022, "声音样本时长需要在5秒到2分钟之间"),

    // Video Composition Errors (1023-1025, Story 4.3)
    COMPOSITION_FAILED(1023, "视频合成失败"),
    VIDEO_CUTTING_FAILED(1024, "视频片段裁剪失败"),
    OUTPUT_NOT_READY(1025, "视频输出尚未就绪");

    private final int code;
    private final String message;
}
