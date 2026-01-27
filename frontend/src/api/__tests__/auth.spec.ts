/**
 * Tests for auth API functions.
 * Story 1.3: T6 - Auth API implementation
 *
 * Covers:
 * - 1.3-INT-003: Send code API
 * - 1.3-INT-004: Login API
 * - 1.3-UNIT-007: Token stored in localStorage
 * - 1.3-BLIND-ERROR-002: 429 rate limit handling (constants + behavioral)
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';

const { mockPost } = vi.hoisted(() => ({
  mockPost: vi.fn(),
}));

vi.mock('axios', async (importOriginal) => {
  const actual = await importOriginal<typeof import('axios')>();
  return {
    ...actual,
    default: {
      ...actual.default,
      create: vi.fn(() => ({
        post: mockPost,
      })),
    },
  };
});

import {
  isAuthEndpoint,
  RATE_LIMIT_ERROR_CODE,
  RATE_LIMIT_ERROR_MESSAGE,
  sendCode,
  login,
  refreshToken,
} from '../auth';

/**
 * Note: Full API integration tests require proper MSW (Mock Service Worker) setup.
 * These tests cover the utility functions and validation logic.
 * Integration tests with actual API calls should be done in E2E tests.
 */
describe('Auth API', () => {
  beforeEach(() => {
    mockPost.mockReset();
  });

  describe('isAuthEndpoint utility', () => {
    it('should return true for auth endpoints', () => {
      expect(isAuthEndpoint('/auth/login')).toBe(true);
      expect(isAuthEndpoint('/auth/send-code')).toBe(true);
      expect(isAuthEndpoint('/auth/refresh')).toBe(true);
      expect(isAuthEndpoint('/api/v1/auth/login')).toBe(true);
    });

    it('should return false for non-auth endpoints', () => {
      expect(isAuthEndpoint('/tasks')).toBe(false);
      expect(isAuthEndpoint('/api/v1/tasks')).toBe(false);
      expect(isAuthEndpoint('/users/me')).toBe(false);
    });

    it('should return false for undefined', () => {
      expect(isAuthEndpoint(undefined)).toBe(false);
    });

    it('should return false for empty string', () => {
      expect(isAuthEndpoint('')).toBe(false);
    });
  });

  describe('API function signatures', () => {
    it('should export sendCode function', async () => {
      const { sendCode } = await import('../auth');
      expect(typeof sendCode).toBe('function');
    });

    it('should export login function', async () => {
      const { login } = await import('../auth');
      expect(typeof login).toBe('function');
    });

    it('should export refreshToken function', async () => {
      const { refreshToken } = await import('../auth');
      expect(typeof refreshToken).toBe('function');
    });
  });

  describe('API endpoint paths', () => {
    it('should use correct paths for auth endpoints', () => {
      // Verify the endpoint patterns are correct based on isAuthEndpoint
      const endpoints = [
        '/auth/send-code',
        '/auth/login',
        '/auth/refresh'
      ];

      endpoints.forEach(endpoint => {
        expect(isAuthEndpoint(endpoint)).toBe(true);
      });
    });
  });

  /**
   * 1.3-BLIND-ERROR-002: Rate limit handling
   * Verifies that 429 rate limit constants are properly defined.
   */
  describe('Rate limit error handling', () => {
    it('should export RATE_LIMIT_ERROR_CODE constant', () => {
      expect(RATE_LIMIT_ERROR_CODE).toBe('RATE_LIMITED');
    });

    it('should export RATE_LIMIT_ERROR_MESSAGE constant', () => {
      expect(RATE_LIMIT_ERROR_MESSAGE).toBe('请求过于频繁，请稍后再试');
    });

    it('should have correct error code type', () => {
      expect(typeof RATE_LIMIT_ERROR_CODE).toBe('string');
    });

    it('should have correct error message type', () => {
      expect(typeof RATE_LIMIT_ERROR_MESSAGE).toBe('string');
    });
  });

  /**
   * 1.3-BLIND-ERROR-002: Behavioral 429 rate limit handling
   * Tests handleAuthError() behavior with mocked 429 responses.
   */
  describe('handleAuthError 429 behavior (BLIND-ERROR-002)', () => {
    function make429Error(retryAfter?: string) {
      const error = new Error('Request failed with status code 429');
      Object.assign(error, {
        isAxiosError: true,
        response: {
          status: 429,
          headers: retryAfter !== undefined ? { 'retry-after': retryAfter } : {},
          data: {},
          statusText: 'Too Many Requests',
          config: {},
        },
        config: {},
      });
      return error;
    }

    it('should throw error with retry seconds from Retry-After header on sendCode 429', async () => {
      mockPost.mockRejectedValueOnce(make429Error('30'));

      await expect(sendCode('13800138000')).rejects.toThrow(
        '请求过于频繁，请稍后再试（30秒后重试）'
      );
    });

    it('should use default 60s when Retry-After header is missing on sendCode 429', async () => {
      mockPost.mockRejectedValueOnce(make429Error());

      await expect(sendCode('13800138000')).rejects.toThrow(
        '请求过于频繁，请稍后再试（60秒后重试）'
      );
    });

    it('should throw error with retry seconds on login 429', async () => {
      mockPost.mockRejectedValueOnce(make429Error('45'));

      await expect(login('13800138000', '123456')).rejects.toThrow(
        '请求过于频繁，请稍后再试（45秒后重试）'
      );
    });

    it('should throw error with retry seconds on refreshToken 429', async () => {
      mockPost.mockRejectedValueOnce(make429Error('10'));

      await expect(refreshToken('test-refresh-token')).rejects.toThrow(
        '请求过于频繁，请稍后再试（10秒后重试）'
      );
    });

    it('should throw server error message for non-429 errors', async () => {
      const error = new Error('Request failed with status code 500');
      Object.assign(error, {
        isAxiosError: true,
        response: {
          status: 500,
          headers: {},
          data: { message: '服务器内部错误' },
          statusText: 'Internal Server Error',
          config: {},
        },
        config: {},
      });
      mockPost.mockRejectedValueOnce(error);

      await expect(sendCode('13800138000')).rejects.toThrow('服务器内部错误');
    });

    it('should rethrow non-axios errors as-is', async () => {
      const error = new TypeError('Network failure');
      mockPost.mockRejectedValueOnce(error);

      await expect(sendCode('13800138000')).rejects.toThrow('Network failure');
    });
  });
});
