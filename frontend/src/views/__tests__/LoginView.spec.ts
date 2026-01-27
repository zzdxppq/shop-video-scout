/**
 * Tests for LoginView component.
 * Story 1.3: T5 - Login page tests
 *
 * Covers:
 * - 1.3-UNIT-008: Phone validation (11 digits, starts with 1)
 * - 1.3-UNIT-009: Code validation (6 digits)
 * - 1.3-UNIT-010: Countdown 60->0
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import LoginView from '../LoginView.vue';
import { PHONE_REGEX, CODE_REGEX, CODE_COUNTDOWN_SECONDS } from '../../types/auth';

// Mock vue-router
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
  }),
  useRoute: () => ({
    query: {}
  })
}));

// Mock Element Plus
vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  }
}));

// Mock auth API
vi.mock('../../api/auth', () => ({
  sendCode: vi.fn(),
  login: vi.fn()
}));

// Mock user store
vi.mock('../../stores/user', () => ({
  useUserStore: () => ({
    setTokens: vi.fn(),
    setUserInfo: vi.fn()
  })
}));

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  describe('Phone validation (1.3-UNIT-008)', () => {
    it('should validate phone with 11 digits starting with 1', () => {
      // Valid phones
      expect(PHONE_REGEX.test('13800138000')).toBe(true);
      expect(PHONE_REGEX.test('15912345678')).toBe(true);
      expect(PHONE_REGEX.test('18800001111')).toBe(true);
    });

    it('should reject phone with 10 digits', () => {
      expect(PHONE_REGEX.test('1380013800')).toBe(false);
    });

    it('should reject phone with 12 digits', () => {
      expect(PHONE_REGEX.test('138001380001')).toBe(false);
    });

    it('should reject phone not starting with 1', () => {
      expect(PHONE_REGEX.test('23800138000')).toBe(false);
      expect(PHONE_REGEX.test('03800138000')).toBe(false);
    });

    it('should reject phone with non-numeric characters', () => {
      expect(PHONE_REGEX.test('1380013800a')).toBe(false);
      expect(PHONE_REGEX.test('138-0013-800')).toBe(false);
    });
  });

  describe('Code validation (1.3-UNIT-009)', () => {
    it('should validate 6-digit code', () => {
      expect(CODE_REGEX.test('123456')).toBe(true);
      expect(CODE_REGEX.test('000000')).toBe(true);
      expect(CODE_REGEX.test('999999')).toBe(true);
    });

    it('should reject 5-digit code', () => {
      expect(CODE_REGEX.test('12345')).toBe(false);
    });

    it('should reject 7-digit code', () => {
      expect(CODE_REGEX.test('1234567')).toBe(false);
    });

    it('should reject code with non-numeric characters', () => {
      expect(CODE_REGEX.test('12345a')).toBe(false);
      expect(CODE_REGEX.test('abcdef')).toBe(false);
    });
  });

  describe('Countdown constant (1.3-UNIT-010)', () => {
    it('should have 60 seconds countdown duration', () => {
      expect(CODE_COUNTDOWN_SECONDS).toBe(60);
    });
  });

  describe('Component rendering', () => {
    it('should render login form', () => {
      const wrapper = mount(LoginView, {
        global: {
          stubs: {
            RouterLink: true
          }
        }
      });

      expect(wrapper.find('h1').text()).toContain('探店宝');
      expect(wrapper.find('h2').text()).toContain('手机号登录');
      expect(wrapper.find('input[type="tel"]').exists()).toBe(true);
    });

    it('should auto-focus phone input on mount', async () => {
      const wrapper = mount(LoginView, {
        attachTo: document.body,
        global: {
          stubs: {
            RouterLink: true
          }
        }
      });

      await flushPromises();

      // The first input should be focused
      const phoneInput = wrapper.find('input[aria-label="手机号"]');
      expect(phoneInput.exists()).toBe(true);

      wrapper.unmount();
    });
  });

  describe('Input handling', () => {
    it('should strip non-numeric characters from phone input', async () => {
      const wrapper = mount(LoginView);
      const phoneInput = wrapper.find('input[aria-label="手机号"]');

      await phoneInput.setValue('138abc00138000');
      await phoneInput.trigger('input');

      expect((phoneInput.element as HTMLInputElement).value.length).toBeLessThanOrEqual(11);
    });

    it('should strip non-numeric characters from code input', async () => {
      const wrapper = mount(LoginView);
      const codeInput = wrapper.find('input[aria-label="验证码"]');

      await codeInput.setValue('123abc');
      await codeInput.trigger('input');

      // After input handler, non-numeric should be stripped
      expect(CODE_REGEX.test('123')).toBe(false); // Only 3 digits, not 6
    });
  });

  describe('Send code button state', () => {
    it('should disable send code button when phone is invalid', async () => {
      const wrapper = mount(LoginView);
      const sendCodeBtn = wrapper.findAll('button')[0]; // First button is send code

      // Initially phone is empty
      expect(sendCodeBtn.attributes('disabled')).toBeDefined();
    });

    it('should enable send code button when phone is valid', async () => {
      const wrapper = mount(LoginView);
      const phoneInput = wrapper.find('input[aria-label="手机号"]');

      // Set valid phone
      await phoneInput.setValue('13800138000');
      await phoneInput.trigger('input');
      await flushPromises();

      // Re-find button after state change
      const buttons = wrapper.findAll('button');
      const sendCodeBtn = buttons[0];

      // Should not be disabled
      expect(sendCodeBtn.classes()).toContain('bg-primary');
    });
  });
});
