/**
 * Tests for user store.
 * Covers: 1.3-UNIT-006: User store manages token state.
 */
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useUserStore } from '../user';

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: vi.fn((key: string) => store[key] || null),
    setItem: vi.fn((key: string, value: string) => { store[key] = value; }),
    removeItem: vi.fn((key: string) => { delete store[key]; }),
    clear: vi.fn(() => { store = {}; })
  };
})();

Object.defineProperty(window, 'localStorage', { value: localStorageMock });

describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorageMock.clear();
    vi.clearAllMocks();
  });

  describe('initial state', () => {
    it('should have null tokens when localStorage is empty', () => {
      const store = useUserStore();

      expect(store.accessToken).toBeNull();
      expect(store.refreshToken).toBeNull();
      expect(store.isAuthenticated).toBe(false);
    });

    it('should load token from localStorage if present', () => {
      localStorageMock.setItem('access_token', 'existing-token');
      localStorageMock.setItem('refresh_token', 'existing-refresh');

      // Need to recreate store to load from localStorage
      setActivePinia(createPinia());
      const store = useUserStore();

      expect(store.accessToken).toBe('existing-token');
    });
  });

  describe('setTokens', () => {
    it('should set both tokens in state and localStorage', () => {
      const store = useUserStore();

      store.setTokens('new-access', 'new-refresh');

      expect(store.accessToken).toBe('new-access');
      expect(store.refreshToken).toBe('new-refresh');
      expect(localStorageMock.setItem).toHaveBeenCalledWith('access_token', 'new-access');
      expect(localStorageMock.setItem).toHaveBeenCalledWith('refresh_token', 'new-refresh');
    });

    it('should update isAuthenticated to true', () => {
      const store = useUserStore();

      expect(store.isAuthenticated).toBe(false);
      store.setTokens('token', 'refresh');
      expect(store.isAuthenticated).toBe(true);
    });
  });

  describe('setAccessToken', () => {
    it('should update only access token', () => {
      const store = useUserStore();
      store.setTokens('old-access', 'refresh');

      store.setAccessToken('new-access');

      expect(store.accessToken).toBe('new-access');
      expect(store.refreshToken).toBe('refresh');
      expect(localStorageMock.setItem).toHaveBeenCalledWith('access_token', 'new-access');
    });
  });

  describe('getRefreshToken', () => {
    it('should return refresh token', () => {
      const store = useUserStore();
      store.setTokens('access', 'my-refresh');

      expect(store.getRefreshToken()).toBe('my-refresh');
    });
  });

  describe('clearAuth', () => {
    it('should clear all auth state', () => {
      const store = useUserStore();
      store.setTokens('access', 'refresh');
      store.setUserInfo({ id: 1, phone: '13800138000', membershipType: 'free' });

      store.clearAuth();

      expect(store.accessToken).toBeNull();
      expect(store.refreshToken).toBeNull();
      expect(store.userInfo).toBeNull();
      expect(store.isAuthenticated).toBe(false);
    });

    it('should remove tokens from localStorage', () => {
      const store = useUserStore();
      store.setTokens('access', 'refresh');

      store.clearAuth();

      expect(localStorageMock.removeItem).toHaveBeenCalledWith('access_token');
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('refresh_token');
    });
  });

  describe('checkAuth', () => {
    it('should return false when not authenticated', () => {
      const store = useUserStore();
      expect(store.checkAuth()).toBe(false);
    });

    it('should return true when authenticated', () => {
      const store = useUserStore();
      store.setTokens('token', 'refresh');
      expect(store.checkAuth()).toBe(true);
    });
  });

  describe('setUserInfo', () => {
    it('should set user info', () => {
      const store = useUserStore();
      const info = { id: 1, phone: '13800138000', nickname: 'Test', membershipType: 'pro' as const };

      store.setUserInfo(info);

      expect(store.userInfo).toEqual(info);
    });
  });
});
