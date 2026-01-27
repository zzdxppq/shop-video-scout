<script setup lang="ts">
/**
 * Login Page Component.
 * Story 1.3: Frontend Login Page - T5, T6
 *
 * BR-2.1: Phone real-time validation
 * BR-2.2: 60s countdown after sending code
 * BR-2.3: Token stored to localStorage on success
 */
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '../stores/user';
import { sendCode as sendCodeApi, login as loginApi } from '../api/auth';
import { PHONE_REGEX, CODE_REGEX, CODE_COUNTDOWN_SECONDS } from '../types/auth';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

// Form data
const phone = ref('');
const code = ref('');

// UI states
const loading = ref(false);
const sendingCode = ref(false);
const countdown = ref(0);
const phoneInputRef = ref<HTMLInputElement>();

// Validation
const phoneError = ref('');
const codeError = ref('');

/**
 * Validate phone number format.
 * BR-2.1: Real-time format validation
 */
function validatePhone(): boolean {
  if (!phone.value) {
    phoneError.value = '请输入手机号';
    return false;
  }
  if (!PHONE_REGEX.test(phone.value)) {
    phoneError.value = '请输入正确的手机号';
    return false;
  }
  phoneError.value = '';
  return true;
}

/**
 * Validate verification code format.
 */
function validateCode(): boolean {
  if (!code.value) {
    codeError.value = '请输入验证码';
    return false;
  }
  if (!CODE_REGEX.test(code.value)) {
    codeError.value = '请输入6位验证码';
    return false;
  }
  codeError.value = '';
  return true;
}

/**
 * Phone input handler for real-time validation.
 */
function onPhoneInput() {
  // Only allow digits
  phone.value = phone.value.replace(/\D/g, '').slice(0, 11);
  if (phone.value && !PHONE_REGEX.test(phone.value) && phone.value.length === 11) {
    phoneError.value = '请输入正确的手机号';
  } else {
    phoneError.value = '';
  }
}

/**
 * Code input handler.
 */
function onCodeInput() {
  // Only allow digits
  code.value = code.value.replace(/\D/g, '').slice(0, 6);
  if (codeError.value) {
    codeError.value = '';
  }
}

/**
 * Whether send code button is disabled.
 */
const canSendCode = computed(() => {
  return PHONE_REGEX.test(phone.value) && countdown.value === 0 && !sendingCode.value;
});

/**
 * Send code button text.
 */
const sendCodeText = computed(() => {
  if (countdown.value > 0) {
    return `${countdown.value}秒后重发`;
  }
  return '获取验证码';
});

/**
 * Start countdown timer.
 * BR-2.2: 60s countdown after sending code
 */
function startCountdown() {
  countdown.value = CODE_COUNTDOWN_SECONDS;
  const timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(timer);
    }
  }, 1000);
}

/**
 * Send verification code.
 */
async function handleSendCode() {
  if (!validatePhone()) return;
  if (sendingCode.value || countdown.value > 0) return;

  sendingCode.value = true;
  try {
    const response = await sendCodeApi(phone.value);
    if (response.code === 0) {
      ElMessage.success('验证码已发送');
      startCountdown();
    } else {
      ElMessage.error(response.message || '发送验证码失败');
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '网络错误，请重试');
  } finally {
    sendingCode.value = false;
  }
}

/**
 * Submit login form.
 */
async function handleLogin() {
  if (!validatePhone() || !validateCode()) return;
  if (loading.value) return;

  loading.value = true;
  try {
    const response = await loginApi(phone.value, code.value);
    if (response.code === 0 && response.data) {
      const { access_token, refresh_token, user } = response.data;

      // BR-2.3: Store tokens
      userStore.setTokens(access_token, refresh_token);
      userStore.setUserInfo({
        id: user.id,
        phone: user.phone,
        nickname: user.nickname,
        avatarUrl: user.avatar_url,
        membershipType: user.membership_type
      });

      ElMessage.success('登录成功');

      // Redirect to original page or home
      const redirect = route.query.redirect as string || '/';
      setTimeout(() => {
        router.push(redirect);
      }, 1000);
    } else {
      ElMessage.error(response.message || '登录失败');
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '网络错误，请重试');
  } finally {
    loading.value = false;
  }
}

/**
 * Handle enter key press.
 */
function handleKeyPress(event: KeyboardEvent) {
  if (event.key === 'Enter') {
    if (phone.value && !code.value && canSendCode.value) {
      handleSendCode();
    } else if (phone.value && code.value) {
      handleLogin();
    }
  }
}

// Auto-focus phone input on mount
onMounted(() => {
  phoneInputRef.value?.focus();
});
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-neutral-100 px-4">
    <div class="w-full max-w-md">
      <!-- Logo and title -->
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-neutral-900 mb-2">探店宝</h1>
        <p class="text-neutral-600">探店达人的AI视频助手</p>
      </div>

      <!-- Login card -->
      <div class="bg-white rounded-lg shadow-md p-8">
        <h2 class="text-xl font-semibold text-neutral-900 mb-6 text-center">
          手机号登录
        </h2>

        <!-- Phone input -->
        <div class="mb-4">
          <label class="block text-sm font-medium text-neutral-700 mb-1">
            手机号
          </label>
          <input
            ref="phoneInputRef"
            v-model="phone"
            type="tel"
            inputmode="numeric"
            maxlength="11"
            placeholder="请输入手机号"
            class="w-full px-4 py-3 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary transition-colors"
            :class="{
              'border-neutral-300': !phoneError,
              'border-error': phoneError,
              'focus:border-primary': !phoneError,
              'focus:border-error': phoneError
            }"
            @input="onPhoneInput"
            @keypress="handleKeyPress"
            aria-label="手机号"
            aria-describedby="phone-error"
          />
          <p
            v-if="phoneError"
            id="phone-error"
            class="mt-1 text-sm text-error"
            role="alert"
          >
            {{ phoneError }}
          </p>
        </div>

        <!-- Code input with send button -->
        <div class="mb-6">
          <label class="block text-sm font-medium text-neutral-700 mb-1">
            验证码
          </label>
          <div class="flex gap-3">
            <input
              v-model="code"
              type="tel"
              inputmode="numeric"
              maxlength="6"
              placeholder="请输入6位验证码"
              class="flex-1 px-4 py-3 border rounded-md focus:outline-none focus:ring-2 focus:ring-primary transition-colors"
              :class="{
                'border-neutral-300': !codeError,
                'border-error': codeError,
                'focus:border-primary': !codeError,
                'focus:border-error': codeError
              }"
              @input="onCodeInput"
              @keypress="handleKeyPress"
              aria-label="验证码"
              aria-describedby="code-error"
            />
            <button
              type="button"
              class="px-4 py-3 text-sm font-medium rounded-md whitespace-nowrap transition-colors"
              :class="{
                'bg-primary text-white hover:bg-primary-dark': canSendCode,
                'bg-neutral-200 text-neutral-500 cursor-not-allowed': !canSendCode
              }"
              :disabled="!canSendCode"
              @click="handleSendCode"
            >
              {{ sendCodeText }}
            </button>
          </div>
          <p
            v-if="codeError"
            id="code-error"
            class="mt-1 text-sm text-error"
            role="alert"
          >
            {{ codeError }}
          </p>
        </div>

        <!-- Login button -->
        <button
          type="button"
          class="w-full py-3 px-4 font-medium text-white rounded-md transition-colors"
          :class="{
            'bg-primary hover:bg-primary-dark': !loading,
            'bg-primary-light cursor-not-allowed': loading
          }"
          :disabled="loading"
          @click="handleLogin"
        >
          <span v-if="loading" class="flex items-center justify-center">
            <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            登录中...
          </span>
          <span v-else>登录</span>
        </button>

        <!-- Terms notice -->
        <p class="mt-4 text-xs text-neutral-500 text-center">
          登录即表示同意《用户协议》和《隐私政策》
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Custom colors based on design system */
.bg-neutral-100 { background-color: #F7FAFC; }
.text-neutral-900 { color: #1A202C; }
.text-neutral-700 { color: #4A5568; }
.text-neutral-600 { color: #718096; }
.text-neutral-500 { color: #A0AEC0; }
.border-neutral-300 { border-color: #CBD5E0; }
.bg-neutral-200 { background-color: #EDF2F7; }

/* Primary colors */
.bg-primary { background-color: #FF6B35; }
.bg-primary-dark { background-color: #E55A2B; }
.bg-primary-light { background-color: #FF8F66; }
.text-primary { color: #FF6B35; }
.focus\:ring-primary:focus { --tw-ring-color: #FF6B35; }
.focus\:border-primary:focus { border-color: #FF6B35; }

/* Error colors */
.text-error { color: #F56565; }
.border-error { border-color: #F56565; }
.focus\:border-error:focus { border-color: #F56565; }
</style>
