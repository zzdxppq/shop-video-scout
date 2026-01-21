package com.shopvideoscout.user.service;

import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.common.util.RedisUtils;
import com.shopvideoscout.security.jwt.JwtProperties;
import com.shopvideoscout.security.jwt.JwtUtils;
import com.shopvideoscout.user.constant.AuthConstants;
import com.shopvideoscout.user.dto.*;
import com.shopvideoscout.user.entity.User;
import com.shopvideoscout.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Authentication service handling verification codes, login, and token management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RedisUtils redisUtils;
    private final SmsService smsService;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;

    private final Random random = new Random();

    /**
     * Send verification code to phone number.
     * Implements BR-1.1 (60s rate limit), BR-1.2 (6-digit code), BR-1.3 (5-min expiry).
     */
    public SendCodeResponse sendCode(SendCodeRequest request) {
        String phone = request.getPhone();

        // Validate phone format
        if (!isValidPhone(phone)) {
            throw new BusinessException(ResultCode.INVALID_PHONE_FORMAT);
        }

        // Check rate limit (BR-1.1: 60s between requests)
        String limitKey = String.format(AuthConstants.SMS_LIMIT_KEY, phone);
        if (redisUtils.hasKey(limitKey)) {
            Long remainingSeconds = redisUtils.getExpire(limitKey, TimeUnit.SECONDS);
            throw new BusinessException(ResultCode.SMS_SEND_RATE_LIMITED.getCode(),
                    String.format("请求过于频繁，请%d秒后重试", remainingSeconds));
        }

        // Generate 6-digit code (BR-1.2)
        String code = generateCode();

        // Store code in Redis with 5-min TTL (BR-1.3)
        String codeKey = String.format(AuthConstants.SMS_CODE_KEY, phone);
        redisUtils.set(codeKey, code, AuthConstants.SMS_CODE_TTL, TimeUnit.SECONDS);

        // Set rate limit
        redisUtils.set(limitKey, "1", AuthConstants.SMS_LIMIT_TTL, TimeUnit.SECONDS);

        // Send SMS
        boolean sent = smsService.sendVerificationCode(phone, code);
        if (!sent) {
            log.error("Failed to send SMS to phone: {}", phone);
            throw new BusinessException(ResultCode.SMS_SERVICE_UNAVAILABLE);
        }

        log.info("Verification code sent to phone: {}", phone);
        return SendCodeResponse.success();
    }

    /**
     * Login with phone number and verification code.
     * Implements BR-2.1 (lockout), BR-2.2 (15-min access token), BR-2.3 (7-day refresh),
     * BR-2.4 (auto-create user with free membership).
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String phone = request.getPhone();
        String code = request.getCode();

        // Validate inputs
        if (!isValidPhone(phone)) {
            throw new BusinessException(ResultCode.INVALID_PHONE_FORMAT);
        }
        if (!isValidCode(code)) {
            throw new BusinessException(ResultCode.INVALID_CODE_FORMAT);
        }

        // Check if phone is locked (BR-2.1)
        String attemptsKey = String.format(AuthConstants.LOGIN_ATTEMPTS_KEY, phone);
        Object attemptsObj = redisUtils.get(attemptsKey);
        int attempts = attemptsObj != null ? Integer.parseInt(attemptsObj.toString()) : 0;

        if (attempts >= AuthConstants.MAX_LOGIN_ATTEMPTS) {
            Long remainingSeconds = redisUtils.getExpire(attemptsKey, TimeUnit.SECONDS);
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED.getCode(),
                    String.format("验证码错误次数过多，请%d分钟后重试", remainingSeconds / 60));
        }

        // Verify code
        String codeKey = String.format(AuthConstants.SMS_CODE_KEY, phone);
        Object storedCode = redisUtils.get(codeKey);

        if (storedCode == null) {
            // Code expired or never sent
            incrementLoginAttempts(phone, attempts);
            throw new BusinessException(ResultCode.SMS_CODE_EXPIRED);
        }

        if (!code.equals(storedCode.toString())) {
            // Wrong code
            incrementLoginAttempts(phone, attempts);
            int remaining = AuthConstants.MAX_LOGIN_ATTEMPTS - attempts - 1;
            throw new BusinessException(ResultCode.SMS_CODE_INVALID.getCode(),
                    String.format("验证码错误，请重新输入（剩余%d次尝试）", remaining));
        }

        // Code is correct - clear attempts and code
        redisUtils.delete(attemptsKey, codeKey);

        // Find or create user (BR-2.4)
        User user = userMapper.findByPhone(phone);
        if (user == null) {
            user = User.createNewUser(phone);
            userMapper.insert(user);
            log.info("Created new user with phone: {}", phone);
        }

        // Generate tokens (BR-2.2, BR-2.3)
        String accessToken = jwtUtils.generateToken(user.getId(), phone);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), phone);

        log.info("User logged in successfully: {}", phone);

        return LoginResponse.builder()
                .user(LoginResponse.UserInfo.fromUser(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getExpiration())
                .build();
    }

    /**
     * Refresh access token using refresh token.
     */
    public RefreshResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        // Check if token is blacklisted
        String blacklistKey = String.format(AuthConstants.TOKEN_BLACKLIST_KEY, refreshToken);
        if (redisUtils.hasKey(blacklistKey)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        // Validate refresh token
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        // Check if it's a refresh token (has "type": "refresh" claim)
        // Extract user info and generate new access token
        Long userId = jwtUtils.getUserIdFromToken(refreshToken);
        String phone = jwtUtils.getPhoneFromToken(refreshToken);

        String newAccessToken = jwtUtils.generateToken(userId, phone);

        log.info("Access token refreshed for user: {}", phone);

        return RefreshResponse.builder()
                .accessToken(newAccessToken)
                .expiresIn(jwtProperties.getExpiration())
                .build();
    }

    /**
     * Logout - blacklist the refresh token.
     */
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return;
        }

        // Add to blacklist with TTL matching refresh token expiry
        String blacklistKey = String.format(AuthConstants.TOKEN_BLACKLIST_KEY, refreshToken);
        redisUtils.set(blacklistKey, "1", AuthConstants.TOKEN_BLACKLIST_TTL, TimeUnit.SECONDS);

        log.info("Refresh token blacklisted");
    }

    /**
     * Generate 6-digit verification code (BR-1.2).
     */
    private String generateCode() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Validate phone number format (Chinese mainland: 11 digits, starts with 1).
     */
    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches(AuthConstants.PHONE_REGEX);
    }

    /**
     * Validate verification code format (6 digits).
     */
    private boolean isValidCode(String code) {
        return code != null && code.matches(AuthConstants.CODE_REGEX);
    }

    /**
     * Increment login attempts counter with lockout logic (BR-2.1).
     */
    private void incrementLoginAttempts(String phone, int currentAttempts) {
        String attemptsKey = String.format(AuthConstants.LOGIN_ATTEMPTS_KEY, phone);
        int newAttempts = currentAttempts + 1;

        if (newAttempts >= AuthConstants.MAX_LOGIN_ATTEMPTS) {
            // Lock for 15 minutes
            redisUtils.set(attemptsKey, String.valueOf(newAttempts),
                    AuthConstants.LOGIN_LOCK_TTL, TimeUnit.SECONDS);
            log.warn("Phone {} locked due to {} failed login attempts", phone, newAttempts);
        } else {
            // Just increment (keep TTL if exists, or set 15 minutes)
            redisUtils.set(attemptsKey, String.valueOf(newAttempts),
                    AuthConstants.LOGIN_LOCK_TTL, TimeUnit.SECONDS);
        }
    }
}
