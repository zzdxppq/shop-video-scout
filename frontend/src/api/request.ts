/**
 * Axios instance configuration.
 */
import axios, { type AxiosInstance, type AxiosError } from 'axios';
import type { ApiResponse } from '../types/task';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

const request: AxiosInstance = axios.create({
  baseURL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor - add auth token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle errors
request.interceptors.response.use(
  (response) => response.data,
  (error: AxiosError<ApiResponse<unknown>>) => {
    const response = error.response;
    if (response) {
      const { code, message } = response.data;
      // Handle specific error codes
      if (code === 401 || code === 2006 || code === 2007) {
        // Token invalid/expired - redirect to login
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        window.location.href = '/login';
      }
      return Promise.reject(new Error(message || 'Request failed'));
    }
    return Promise.reject(new Error('Network error'));
  }
);

export default request;
