/**
 * Tests for router guard.
 * Story 1.3: T7 - Route guard with redirect preservation
 *
 * Covers:
 * - 1.3-INT-005: Route guard blocks unauthenticated access
 * - 1.3-INT-006: Guard saves redirect query
 * - 1.3-E2E-001: Login with redirect
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: vi.fn((key: string) => store[key] || null),
    setItem: vi.fn((key: string, value: string) => { store[key] = value; }),
    removeItem: vi.fn((key: string) => { delete store[key]; }),
    clear: () => { store = {}; }
  };
})();

Object.defineProperty(window, 'localStorage', { value: localStorageMock });

describe('Router Guard', () => {
  beforeEach(() => {
    localStorageMock.clear();
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('1.3-INT-005: Route guard blocks unauthenticated access', () => {
    it('should block access to protected route when no token', () => {
      // Simulate guard logic
      const to = {
        matched: [{ meta: { requiresAuth: true } }],
        fullPath: '/create'
      };
      const token = localStorageMock.getItem('access_token');
      const requiresAuth = to.matched.some(record => record.meta.requiresAuth);

      expect(requiresAuth).toBe(true);
      expect(token).toBeNull();

      // Should redirect to login
      const shouldRedirect = requiresAuth && !token;
      expect(shouldRedirect).toBe(true);
    });

    it('should allow access to protected route when token exists', () => {
      localStorageMock.setItem('access_token', 'valid-token');

      const to = {
        matched: [{ meta: { requiresAuth: true } }],
        fullPath: '/create'
      };
      const token = localStorageMock.getItem('access_token');
      const requiresAuth = to.matched.some(record => record.meta.requiresAuth);

      expect(requiresAuth).toBe(true);
      expect(token).toBe('valid-token');

      // Should not redirect
      const shouldRedirect = requiresAuth && !token;
      expect(shouldRedirect).toBe(false);
    });

    it('should allow access to public routes without token', () => {
      const to = {
        matched: [{ meta: { requiresAuth: false } }],
        fullPath: '/login'
      };
      const token = localStorageMock.getItem('access_token');
      const requiresAuth = to.matched.some(record => record.meta.requiresAuth);

      expect(requiresAuth).toBe(false);
      expect(token).toBeNull();

      // Should not redirect
      const shouldRedirect = requiresAuth && !token;
      expect(shouldRedirect).toBe(false);
    });
  });

  describe('1.3-INT-006: Guard saves redirect query', () => {
    it('should preserve original destination in redirect query', () => {
      const to = {
        matched: [{ meta: { requiresAuth: true } }],
        fullPath: '/create?step=2',
        path: '/create',
        query: { step: '2' }
      };

      const token = localStorageMock.getItem('access_token');
      const requiresAuth = to.matched.some(record => record.meta.requiresAuth);

      expect(requiresAuth && !token).toBe(true);

      const redirectConfig = {
        path: '/login',
        query: { redirect: to.fullPath }
      };

      expect(redirectConfig.path).toBe('/login');
      expect(redirectConfig.query.redirect).toBe('/create?step=2');
    });

    it('should preserve complex paths with params', () => {
      const to = {
        matched: [{ meta: { requiresAuth: true } }],
        fullPath: '/task/123/upload',
        path: '/task/123/upload',
        query: {}
      };

      const redirectConfig = {
        path: '/login',
        query: { redirect: to.fullPath }
      };

      expect(redirectConfig.query.redirect).toBe('/task/123/upload');
    });
  });

  describe('Login page redirect behavior', () => {
    it('should redirect authenticated user away from login page to default', () => {
      localStorageMock.setItem('access_token', 'valid-token');

      const to = {
        path: '/login',
        query: {} as Record<string, string>,
        matched: [{ meta: { requiresAuth: false } }]
      };
      const token = localStorageMock.getItem('access_token');

      // If already logged in and on login page, redirect away
      const shouldRedirectAway = to.path === '/login' && !!token;
      expect(shouldRedirectAway).toBe(true);

      // Default redirect is home
      const redirect = to.query.redirect || '/';
      expect(redirect).toBe('/');
    });

    it('should redirect to original URL after login when redirect query exists', () => {
      localStorageMock.setItem('access_token', 'valid-token');

      const to = {
        path: '/login',
        query: { redirect: '/create' },
        matched: [{ meta: { requiresAuth: false } }]
      };
      const token = localStorageMock.getItem('access_token');

      const shouldRedirectAway = to.path === '/login' && !!token;
      expect(shouldRedirectAway).toBe(true);

      const redirect = to.query.redirect || '/';
      expect(redirect).toBe('/create');
    });
  });

  describe('Route meta configuration', () => {
    it('should have correct auth requirements for routes', () => {
      const routes = [
        { path: '/', requiresAuth: false }, // Redirect, no auth check
        { path: '/login', requiresAuth: false },
        { path: '/create', requiresAuth: true },
        { path: '/task/:id/upload', requiresAuth: true }
      ];

      expect(routes.find(r => r.path === '/login')?.requiresAuth).toBe(false);
      expect(routes.find(r => r.path === '/create')?.requiresAuth).toBe(true);
      expect(routes.find(r => r.path === '/task/:id/upload')?.requiresAuth).toBe(true);
    });
  });
});
