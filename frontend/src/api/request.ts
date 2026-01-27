/**
 * Axios instance configuration with token refresh.
 * Story 1.3: Frontend Login Page - T4
 *
 * BR-1.1: All requests auto-add Authorization header (except auth endpoints)
 * BR-1.2: 401 response auto-tries token refresh before logout
 */
import axios, { type AxiosInstance, type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse } from '../types/task';
import { refreshToken as refreshTokenApi, isAuthEndpoint } from './auth';
import { useUserStore } from '../stores/user';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

const request: AxiosInstance = axios.create({
  baseURL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

/** Flag to prevent multiple concurrent refresh attempts. */
let isRefreshing = false;

/** Queue of requests waiting for token refresh. */
let refreshSubscribers: Array<(token: string) => void> = [];

/**
 * Add request to queue waiting for token refresh.
 */
function subscribeTokenRefresh(callback: (token: string) => void): void {
  refreshSubscribers.push(callback);
}

/**
 * Notify all queued requests with new token.
 */
function onTokenRefreshed(newToken: string): void {
  refreshSubscribers.forEach((callback) => callback(newToken));
  refreshSubscribers = [];
}

/**
 * Clear refresh queue on refresh failure.
 */
function onRefreshFailed(): void {
  refreshSubscribers = [];
}

// Request interceptor - add auth token
// BR-1.1: Auto-add Authorization header except for auth endpoints
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Skip auth header for auth endpoints
    if (isAuthEndpoint(config.url)) {
      return config;
    }

    const token = localStorage.getItem('access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle errors with token refresh
// BR-1.2: 401 auto-tries token refresh
request.interceptors.response.use(
  (response) => response.data,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    const response = error.response;

    if (!response) {
      return Promise.reject(new Error('网络连接失败，请检查网络'));
    }

    const { code, message } = response.data || { code: response.status, message: 'Request failed' };

    // Handle 401 or token-related error codes
    // 2006: Token expired, 2007: Token invalid
    if ((code === 401 || code === 2006 || code === 2007) && !originalRequest._retry) {
      // Skip refresh for auth endpoints
      if (isAuthEndpoint(originalRequest.url)) {
        return Promise.reject(new Error(message || '认证失败'));
      }

      originalRequest._retry = true;

      // If already refreshing, queue this request
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          subscribeTokenRefresh((newToken: string) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            resolve(request(originalRequest));
          });
          // If refresh fails, this will reject via onRefreshFailed being called
          // We set a timeout to avoid hanging forever
          setTimeout(() => {
            if (refreshSubscribers.length === 0) {
              reject(new Error('Token refresh timeout'));
            }
          }, 10000);
        });
      }

      isRefreshing = true;

      const storedRefreshToken = localStorage.getItem('refresh_token');

      if (!storedRefreshToken) {
        // No refresh token - redirect to login
        isRefreshing = false;
        redirectToLogin();
        return Promise.reject(new Error('请重新登录'));
      }

      try {
        // Attempt to refresh the token
        const refreshResponse = await refreshTokenApi(storedRefreshToken);

        if (refreshResponse.code === 0 && refreshResponse.data) {
          const newAccessToken = refreshResponse.data.access_token;

          // Update token in store and localStorage
          const userStore = useUserStore();
          userStore.setAccessToken(newAccessToken);

          // Notify queued requests
          onTokenRefreshed(newAccessToken);
          isRefreshing = false;

          // Retry the original request
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          return request(originalRequest);
        } else {
          throw new Error('Refresh failed');
        }
      } catch {
        // Refresh failed - clear auth and redirect
        isRefreshing = false;
        onRefreshFailed();
        redirectToLogin();
        return Promise.reject(new Error('登录已过期，请重新登录'));
      }
    }

    return Promise.reject(new Error(message || 'Request failed'));
  }
);

/**
 * Clear auth state and redirect to login page.
 */
function redirectToLogin(): void {
  localStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');

  // Preserve current path for redirect after login
  const currentPath = window.location.pathname;
  if (currentPath !== '/login') {
    window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`;
  }
}

export default request;
