package com.shopvideoscout.user.constant;

/**
 * Constants for authentication operations.
 * Redis key formats per architecture Section 5.2.
 */
public final class AuthConstants {

    private AuthConstants() {
        // Utility class
    }

    // Redis key patterns (per architecture Section 5.2)
    public static final String SMS_CODE_KEY = "sms:code:%s";
    public static final String SMS_LIMIT_KEY = "sms:limit:%s";
    public static final String LOGIN_ATTEMPTS_KEY = "login:attempts:%s";
    public static final String TOKEN_BLACKLIST_KEY = "token:blacklist:%s";

    // TTL values in seconds
    public static final long SMS_CODE_TTL = 300;          // 5 minutes (BR-1.3)
    public static final long SMS_LIMIT_TTL = 60;          // 60 seconds (BR-1.1)
    public static final long LOGIN_LOCK_TTL = 900;        // 15 minutes (BR-2.1)
    public static final long TOKEN_BLACKLIST_TTL = 604800; // 7 days (match refresh token TTL)

    // Business rule limits
    public static final int MAX_LOGIN_ATTEMPTS = 5;       // BR-2.1
    public static final int SMS_CODE_LENGTH = 6;          // BR-1.2

    // Phone validation
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    public static final String CODE_REGEX = "^\\d{6}$";
}
