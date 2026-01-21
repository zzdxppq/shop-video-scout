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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * Tests cover scenarios from QA test design document.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private RedisUtils redisUtils;

    @Mock
    private SmsService smsService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("AC1: Send Verification Code")
    class SendCodeTests {

        @Test
        @DisplayName("1.2-UNIT-001: Valid phone format should pass validation")
        void validPhoneFormat_ShouldPass() {
            // Given
            SendCodeRequest request = new SendCodeRequest();
            request.setPhone("13800138000");

            when(redisUtils.hasKey(anyString())).thenReturn(false);
            when(smsService.sendVerificationCode(anyString(), anyString())).thenReturn(true);

            // When
            SendCodeResponse response = authService.sendCode(request);

            // Then
            assertTrue(response.isSuccess());
            assertEquals("验证码已发送", response.getMessage());
            assertEquals(300, response.getExpiresIn());
        }

        @Test
        @DisplayName("1.2-UNIT-002: Null phone should return validation error")
        void nullPhone_ShouldThrowException() {
            // Given
            SendCodeRequest request = new SendCodeRequest();
            request.setPhone(null);

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.sendCode(request));
            assertEquals(ResultCode.INVALID_PHONE_FORMAT.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("1.2-UNIT-003: Empty phone should return validation error")
        void emptyPhone_ShouldThrowException() {
            // Given
            SendCodeRequest request = new SendCodeRequest();
            request.setPhone("");

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.sendCode(request));
            assertEquals(ResultCode.INVALID_PHONE_FORMAT.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("1.2-UNIT-004: Phone with 10 digits should be rejected")
        void phoneWith10Digits_ShouldThrowException() {
            // Given
            SendCodeRequest request = new SendCodeRequest();
            request.setPhone("1380013800"); // 10 digits

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.sendCode(request));
            assertEquals(ResultCode.INVALID_PHONE_FORMAT.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("1.2-UNIT-005: Phone with 12 digits should be rejected")
        void phoneWith12Digits_ShouldThrowException() {
            // Given
            SendCodeRequest request = new SendCodeRequest();
            request.setPhone("138001380001"); // 12 digits

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.sendCode(request));
            assertEquals(ResultCode.INVALID_PHONE_FORMAT.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("1.2-UNIT-006: Phone with non-numeric characters should be rejected")
        void phoneWithNonNumeric_ShouldThrowException() {
            // Given
            SendCodeRequest request = new SendCodeRequest();
            request.setPhone("1380013800a");

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.sendCode(request));
            assertEquals(ResultCode.INVALID_PHONE_FORMAT.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("1.2-UNIT-007: Phone not starting with 1 should be rejected")
        void phoneNotStartingWith1_ShouldThrowException() {
            // Given
            SendCodeRequest request = new SendCodeRequest();
            request.setPhone("23800138000");

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.sendCode(request));
            assertEquals(ResultCode.INVALID_PHONE_FORMAT.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("1.2-INT-002: Rate limit should return 429 for second request within 60s")
        void rateLimited_ShouldThrow429() {
            // Given
            SendCodeRequest request = new SendCodeRequest();
            request.setPhone("13800138000");

            when(redisUtils.hasKey(anyString())).thenReturn(true);
            when(redisUtils.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(45L);

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.sendCode(request));
            assertEquals(ResultCode.SMS_SEND_RATE_LIMITED.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("45"));
        }

        @Test
        @DisplayName("1.2-INT-005: SMS service failure should return 503")
        void smsServiceFailure_ShouldThrow503() {
            // Given
            SendCodeRequest request = new SendCodeRequest();
            request.setPhone("13800138000");

            when(redisUtils.hasKey(anyString())).thenReturn(false);
            when(smsService.sendVerificationCode(anyString(), anyString())).thenReturn(false);

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.sendCode(request));
            assertEquals(ResultCode.SMS_SERVICE_UNAVAILABLE.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("AC2: Login")
    class LoginTests {

        @BeforeEach
        void setUp() {
            when(jwtProperties.getExpiration()).thenReturn(900L);
        }

        @Test
        @DisplayName("1.2-UNIT-010: Correct code should pass validation")
        void correctCode_ShouldSucceed() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setPhone("13800138000");
            request.setCode("123456");

            User existingUser = new User();
            existingUser.setId(1L);
            existingUser.setPhone("13800138000");
            existingUser.setNickname("用户138000");
            existingUser.setMembershipType("free");

            when(redisUtils.get(contains("login:attempts"))).thenReturn(null);
            when(redisUtils.get(contains("sms:code"))).thenReturn("123456");
            when(userMapper.findByPhone("13800138000")).thenReturn(existingUser);
            when(jwtUtils.generateToken(anyLong(), anyString())).thenReturn("access_token");
            when(jwtUtils.generateRefreshToken(anyLong(), anyString())).thenReturn("refresh_token");

            // When
            LoginResponse response = authService.login(request);

            // Then
            assertNotNull(response);
            assertEquals("access_token", response.getAccessToken());
            assertEquals("refresh_token", response.getRefreshToken());
            assertEquals(900L, response.getExpiresIn());
            assertEquals(1L, response.getUser().getId());
        }

        @Test
        @DisplayName("1.2-UNIT-011: Wrong code should increment error count")
        void wrongCode_ShouldIncrementErrorCount() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setPhone("13800138000");
            request.setCode("999999");

            when(redisUtils.get(contains("login:attempts"))).thenReturn(null);
            when(redisUtils.get(contains("sms:code"))).thenReturn("123456");

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(request));
            assertEquals(ResultCode.SMS_CODE_INVALID.getCode(), exception.getCode());

            // Verify error count was set
            verify(redisUtils).set(contains("login:attempts"), eq("1"),
                    eq(AuthConstants.LOGIN_LOCK_TTL), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("1.2-UNIT-012: Expired code should return 401")
        void expiredCode_ShouldThrow401() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setPhone("13800138000");
            request.setCode("123456");

            when(redisUtils.get(contains("login:attempts"))).thenReturn(null);
            when(redisUtils.get(contains("sms:code"))).thenReturn(null); // Code expired/not found

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(request));
            assertEquals(ResultCode.SMS_CODE_EXPIRED.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("1.2-UNIT-013: Lockout after 5 errors should return 423")
        void lockedAccount_ShouldThrow423() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setPhone("13800138000");
            request.setCode("123456");

            when(redisUtils.get(contains("login:attempts"))).thenReturn("5");
            when(redisUtils.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(600L);

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(request));
            assertEquals(ResultCode.ACCOUNT_LOCKED.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("10")); // 600/60 = 10 minutes
        }

        @Test
        @DisplayName("1.2-UNIT-016: Phone should be masked in response")
        void phoneMasking_ShouldWork() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setPhone("13800138000");
            request.setCode("123456");

            User existingUser = new User();
            existingUser.setId(1L);
            existingUser.setPhone("13800138000");
            existingUser.setNickname("用户138000");
            existingUser.setMembershipType("free");

            when(redisUtils.get(contains("login:attempts"))).thenReturn(null);
            when(redisUtils.get(contains("sms:code"))).thenReturn("123456");
            when(userMapper.findByPhone("13800138000")).thenReturn(existingUser);
            when(jwtUtils.generateToken(anyLong(), anyString())).thenReturn("token");
            when(jwtUtils.generateRefreshToken(anyLong(), anyString())).thenReturn("refresh");

            // When
            LoginResponse response = authService.login(request);

            // Then
            assertEquals("138****8000", response.getUser().getPhone());
        }

        @Test
        @DisplayName("1.2-INT-008: New user should be auto-created with free membership")
        void newUser_ShouldBeAutoCreated() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setPhone("13800138000");
            request.setCode("123456");

            when(redisUtils.get(contains("login:attempts"))).thenReturn(null);
            when(redisUtils.get(contains("sms:code"))).thenReturn("123456");
            when(userMapper.findByPhone("13800138000")).thenReturn(null); // No existing user
            when(jwtUtils.generateToken(anyLong(), anyString())).thenReturn("token");
            when(jwtUtils.generateRefreshToken(anyLong(), anyString())).thenReturn("refresh");

            // When
            LoginResponse response = authService.login(request);

            // Then
            verify(userMapper).insert(argThat(user ->
                    "13800138000".equals(user.getPhone()) &&
                    "free".equals(user.getMembershipType()) &&
                    user.getNickname().contains("138000")));
        }

        @Test
        @DisplayName("1.2-BLIND-BOUNDARY-005: Null code should return validation error")
        void nullCode_ShouldThrowException() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setPhone("13800138000");
            request.setCode(null);

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(request));
            assertEquals(ResultCode.INVALID_CODE_FORMAT.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("1.2-BLIND-BOUNDARY-006: Code with 5 digits should be rejected")
        void codeWith5Digits_ShouldThrowException() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setPhone("13800138000");
            request.setCode("12345"); // 5 digits

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.login(request));
            assertEquals(ResultCode.INVALID_CODE_FORMAT.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("Token Refresh")
    class RefreshTests {

        @BeforeEach
        void setUp() {
            when(jwtProperties.getExpiration()).thenReturn(900L);
        }

        @Test
        @DisplayName("1.2-INT-012: Valid refresh token should return new access token")
        void validRefreshToken_ShouldSucceed() {
            // Given
            RefreshRequest request = new RefreshRequest();
            request.setRefreshToken("valid_refresh_token");

            when(redisUtils.hasKey(contains("token:blacklist"))).thenReturn(false);
            when(jwtUtils.validateToken("valid_refresh_token")).thenReturn(true);
            when(jwtUtils.getUserIdFromToken("valid_refresh_token")).thenReturn(1L);
            when(jwtUtils.getPhoneFromToken("valid_refresh_token")).thenReturn("13800138000");
            when(jwtUtils.generateToken(1L, "13800138000")).thenReturn("new_access_token");

            // When
            RefreshResponse response = authService.refresh(request);

            // Then
            assertEquals("new_access_token", response.getAccessToken());
            assertEquals(900L, response.getExpiresIn());
        }

        @Test
        @DisplayName("1.2-INT-013: Expired refresh token should return 401")
        void expiredRefreshToken_ShouldThrow401() {
            // Given
            RefreshRequest request = new RefreshRequest();
            request.setRefreshToken("expired_token");

            when(redisUtils.hasKey(contains("token:blacklist"))).thenReturn(false);
            when(jwtUtils.validateToken("expired_token")).thenReturn(false);

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.refresh(request));
            assertEquals(ResultCode.REFRESH_TOKEN_INVALID.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("1.2-INT-015: Blacklisted token should be rejected")
        void blacklistedToken_ShouldBeRejected() {
            // Given
            RefreshRequest request = new RefreshRequest();
            request.setRefreshToken("blacklisted_token");

            when(redisUtils.hasKey(contains("token:blacklist"))).thenReturn(true);

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> authService.refresh(request));
            assertEquals(ResultCode.REFRESH_TOKEN_INVALID.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("Logout")
    class LogoutTests {

        @Test
        @DisplayName("1.2-INT-014: Logout should add token to blacklist")
        void logout_ShouldBlacklistToken() {
            // Given
            String refreshToken = "token_to_blacklist";

            // When
            authService.logout(refreshToken);

            // Then
            verify(redisUtils).set(
                    contains("token:blacklist"),
                    eq("1"),
                    eq(AuthConstants.TOKEN_BLACKLIST_TTL),
                    eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("Logout with null token should not throw")
        void logoutNullToken_ShouldNotThrow() {
            // When/Then
            assertDoesNotThrow(() -> authService.logout(null));
            verify(redisUtils, never()).set(anyString(), any(), anyLong(), any());
        }
    }
}
