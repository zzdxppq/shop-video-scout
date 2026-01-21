package com.shopvideoscout.user.controller;

import com.shopvideoscout.common.result.R;
import com.shopvideoscout.user.dto.*;
import com.shopvideoscout.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller handling send-code, login, refresh, and logout endpoints.
 * All endpoints are public (no JWT required) - protected by Gateway whitelist.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Send verification code to phone number.
     * POST /api/v1/auth/send-code
     *
     * @param request containing phone number
     * @return success response with expiry time
     */
    @PostMapping("/send-code")
    public R<SendCodeResponse> sendCode(@RequestBody SendCodeRequest request) {
        log.debug("Send code request for phone: {}", request.getPhone());
        SendCodeResponse response = authService.sendCode(request);
        return R.ok(response);
    }

    /**
     * Login with phone number and verification code.
     * POST /api/v1/auth/login
     *
     * @param request containing phone and code
     * @return user info and tokens on success
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@RequestBody LoginRequest request) {
        log.debug("Login request for phone: {}", request.getPhone());
        LoginResponse response = authService.login(request);
        return R.ok(response);
    }

    /**
     * Refresh access token using refresh token.
     * POST /api/v1/auth/refresh
     *
     * @param request containing refresh token
     * @return new access token
     */
    @PostMapping("/refresh")
    public R<RefreshResponse> refresh(@RequestBody RefreshRequest request) {
        log.debug("Token refresh request");
        RefreshResponse response = authService.refresh(request);
        return R.ok(response);
    }

    /**
     * Logout - invalidate refresh token.
     * POST /api/v1/auth/logout
     *
     * @param request containing refresh token to invalidate
     * @return success response
     */
    @PostMapping("/logout")
    public R<Void> logout(@RequestBody RefreshRequest request) {
        log.debug("Logout request");
        authService.logout(request.getRefreshToken());
        return R.ok();
    }
}
