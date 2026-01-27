/**
 * Auth API functions.
 * Story 1.3: Frontend Login Page - T6
 *
 * Consumes:
 * - POST /api/v1/auth/send-code
 * - POST /api/v1/auth/login
 * - POST /api/v1/auth/refresh
 */
import axios, { AxiosError } from 'axios';
import type { ApiResponse } from '../types/task';
import type {
  SendCodeRequest,
  SendCodeResponse,
  LoginRequest,
  LoginResponse,
  RefreshTokenRequest,
  RefreshTokenResponse
} from '../types/auth';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

/**
 * Rate limit error code for 429 responses.
 * BLIND-ERROR-002: Handle API rate limit responses.
 */
export const RATE_LIMIT_ERROR_CODE = 'RATE_LIMITED';
export const RATE_LIMIT_ERROR_MESSAGE = '请求过于频繁，请稍后再试';

/**
 * Create a separate axios instance for auth endpoints.
 * This instance does NOT have interceptors that add tokens or retry on 401.
 * BR-1.1: Auth endpoints excluded from auto Authorization header.
 */
const authAxios = axios.create({
  baseURL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

/**
 * Handle auth API errors with rate limit detection.
 * @param error - Axios error
 * @throws Error with appropriate message
 */
function handleAuthError(error: unknown): never {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiResponse<unknown>>;

    // BLIND-ERROR-002: Handle 429 rate limit
    if (axiosError.response?.status === 429) {
      const retryAfter = axiosError.response.headers['retry-after'];
      const retrySeconds = retryAfter ? parseInt(retryAfter, 10) : 60;
      throw new Error(`${RATE_LIMIT_ERROR_MESSAGE}（${retrySeconds}秒后重试）`);
    }

    // Return server error message if available
    if (axiosError.response?.data?.message) {
      throw new Error(axiosError.response.data.message);
    }
  }

  throw error;
}

/**
 * Send verification code to phone number.
 * @param phone - 11-digit phone number starting with 1
 */
export async function sendCode(phone: string): Promise<ApiResponse<SendCodeResponse>> {
  try {
    const response = await authAxios.post<ApiResponse<SendCodeResponse>>(
      '/auth/send-code',
      { phone } as SendCodeRequest
    );
    return response.data;
  } catch (error) {
    handleAuthError(error);
  }
}

/**
 * Login with phone and verification code.
 * @param phone - 11-digit phone number
 * @param code - 6-digit verification code
 */
export async function login(phone: string, code: string): Promise<ApiResponse<LoginResponse>> {
  try {
    const response = await authAxios.post<ApiResponse<LoginResponse>>(
      '/auth/login',
      { phone, code } as LoginRequest
    );
    return response.data;
  } catch (error) {
    handleAuthError(error);
  }
}

/**
 * Refresh access token using refresh token.
 * Used by request interceptor on 401 response.
 * @param refreshToken - Current refresh token
 */
export async function refreshToken(refreshToken: string): Promise<ApiResponse<RefreshTokenResponse>> {
  try {
    const response = await authAxios.post<ApiResponse<RefreshTokenResponse>>(
      '/auth/refresh',
      { refresh_token: refreshToken } as RefreshTokenRequest
    );
    return response.data;
  } catch (error) {
    handleAuthError(error);
  }
}

/**
 * Check if a URL is an auth endpoint (should not have interceptors).
 */
export function isAuthEndpoint(url: string | undefined): boolean {
  if (!url) return false;
  return url.includes('/auth/');
}
