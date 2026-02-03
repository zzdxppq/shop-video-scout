<script setup lang="ts">
/**
 * Subtitle toggle component.
 * Story 4.5: 字幕设置页面 - AC1
 *
 * Displays a toggle switch with label for enabling/disabling subtitles.
 * BR-1.1: 字幕开关默认为开启状态
 * BR-1.3: 开关状态与UI状态同步
 */
import { computed } from 'vue';

const props = withDefaults(defineProps<{
  /** Whether subtitles are enabled */
  enabled: boolean;
  /** Whether the toggle is disabled */
  disabled?: boolean;
  /** Whether an API call is in progress */
  loading?: boolean;
}>(), {
  disabled: false,
  loading: false
});

const emit = defineEmits<{
  /** Emitted when user toggles the switch */
  (e: 'update:enabled', value: boolean): void;
}>();

const isInteractive = computed(() => !props.disabled && !props.loading);

function handleClick() {
  if (isInteractive.value) {
    emit('update:enabled', !props.enabled);
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    handleClick();
  }
}
</script>

<template>
  <div class="flex items-center justify-between py-3">
    <span class="text-gray-700">为视频添加字幕</span>

    <button
      type="button"
      role="switch"
      :aria-checked="enabled"
      :aria-disabled="disabled || loading"
      :disabled="disabled || loading"
      class="relative inline-flex h-6 w-11 flex-shrink-0 rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
      :class="[
        enabled ? 'bg-blue-500' : 'bg-gray-200',
        isInteractive ? 'cursor-pointer' : 'cursor-not-allowed opacity-50'
      ]"
      @click="handleClick"
      @keydown="handleKeydown"
    >
      <span class="sr-only">{{ enabled ? '关闭字幕' : '开启字幕' }}</span>

      <!-- Toggle knob -->
      <span
        class="pointer-events-none relative inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out"
        :class="enabled ? 'translate-x-5' : 'translate-x-0'"
      >
        <!-- Loading spinner -->
        <span
          v-if="loading"
          class="absolute inset-0 flex items-center justify-center"
        >
          <svg
            class="h-3 w-3 animate-spin text-blue-500"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              class="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              stroke-width="4"
            />
            <path
              class="opacity-75"
              fill="currentColor"
              d="m4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
            />
          </svg>
        </span>
      </span>
    </button>
  </div>
</template>
