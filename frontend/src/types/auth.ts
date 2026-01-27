/**
 * Auth-related type definitions.
 * Story 1.3: Frontend Login Page
 */

/**
 * Send verification code request.
 */
export interface SendCodeRequest {
  phone: string;
}

/**
 * Send code response.
 */
export interface SendCodeResponse {
  success: boolean;
}

/**
 * Login request payload.
 */
export interface LoginRequest {
  phone: string;
  code: string;
}

/**
 * Login response with tokens.
 */
export interface LoginResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  user: UserInfo;
}

/**
 * Token refresh request.
 */
export interface RefreshTokenRequest {
  refresh_token: string;
}

/**
 * Token refresh response.
 */
export interface RefreshTokenResponse {
  access_token: string;
  expires_in: number;
}

/**
 * User info from login response.
 */
export interface UserInfo {
  id: number;
  phone: string;
  nickname?: string;
  avatar_url?: string;
  membership_type: 'free' | 'basic' | 'pro';
}

/**
 * Phone validation regex - 11 digits starting with 1.
 */
export const PHONE_REGEX = /^1\d{10}$/;

/**
 * Verification code regex - 6 digits.
 */
export const CODE_REGEX = /^\d{6}$/;

/**
 * Countdown duration for send code button (seconds).
 */
export const CODE_COUNTDOWN_SECONDS = 60;
