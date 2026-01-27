/**
 * User store for authentication state management.
 * BR-2.3: Token stored to localStorage for persistence.
 */
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export interface UserInfo {
  id: number;
  phone: string;
  nickname?: string;
  avatarUrl?: string;
  membershipType: 'free' | 'basic' | 'pro';
}

export const useUserStore = defineStore('user', () => {
  // State
  const accessToken = ref<string | null>(localStorage.getItem('access_token'));
  const refreshToken = ref<string | null>(localStorage.getItem('refresh_token'));
  const userInfo = ref<UserInfo | null>(null);

  // Getters
  const isAuthenticated = computed(() => !!accessToken.value);

  const token = computed(() => accessToken.value);

  // Actions

  /**
   * Set tokens after successful login.
   * BR-2.3: Tokens stored to localStorage.
   */
  function setTokens(access: string, refresh: string) {
    accessToken.value = access;
    refreshToken.value = refresh;
    localStorage.setItem('access_token', access);
    localStorage.setItem('refresh_token', refresh);
  }

  /**
   * Update access token after refresh.
   */
  function setAccessToken(access: string) {
    accessToken.value = access;
    localStorage.setItem('access_token', access);
  }

  /**
   * Get current refresh token.
   */
  function getRefreshToken(): string | null {
    return refreshToken.value;
  }

  /**
   * Set user info after login.
   */
  function setUserInfo(info: UserInfo) {
    userInfo.value = info;
  }

  /**
   * Clear all auth state (logout).
   */
  function clearAuth() {
    accessToken.value = null;
    refreshToken.value = null;
    userInfo.value = null;
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
  }

  /**
   * Check if user is logged in (has valid token).
   */
  function checkAuth(): boolean {
    return !!accessToken.value;
  }

  return {
    // State
    accessToken,
    refreshToken,
    userInfo,

    // Getters
    isAuthenticated,
    token,

    // Actions
    setTokens,
    setAccessToken,
    getRefreshToken,
    setUserInfo,
    clearAuth,
    checkAuth
  };
});
