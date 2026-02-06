<script setup lang="ts">
/**
 * Regenerate button component.
 * Story 5.4: 发布辅助UI组件 - AC3
 *
 * Button to trigger content regeneration with remaining count display.
 * States: normal, loading, disabled (limit reached)
 */
import { computed } from 'vue';

interface Props {
  /** Remaining regeneration count (0-3) */
  remaining: number;
  /** Whether regeneration is in progress */
  loading?: boolean;
}

interface Emits {
  /** Emitted when regenerate is clicked */
  (e: 'regenerate'): void;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
});

const emit = defineEmits<Emits>();

/** Whether button should be disabled */
const isDisabled = computed(() => props.remaining <= 0 || props.loading);

/** Button text based on state */
const buttonText = computed(() => {
  if (props.loading) {
    return '正在生成...';
  }
  if (props.remaining <= 0) {
    return '已达上限';
  }
  return `换一批推荐 (剩余${props.remaining}次)`;
});

/**
 * Handle button click.
 */
function handleClick() {
  if (!isDisabled.value) {
    emit('regenerate');
  }
}
</script>

<template>
  <button
    :disabled="isDisabled"
    class="w-full py-2.5 rounded-lg text-sm font-medium transition-colors flex items-center justify-center gap-2"
    :class="{
      'bg-gray-100 text-gray-400 cursor-not-allowed': isDisabled && !loading,
      'bg-blue-50 text-blue-500 cursor-wait': loading,
      'bg-blue-500 text-white hover:bg-blue-600': !isDisabled && !loading
    }"
    @click="handleClick"
  >
    <!-- Loading spinner -->
    <svg
      v-if="loading"
      class="animate-spin h-4 w-4"
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
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
      />
    </svg>

    <!-- Refresh icon (when not loading and not disabled) -->
    <svg
      v-else-if="!isDisabled"
      class="w-4 h-4"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24"
    >
      <path
        stroke-linecap="round"
        stroke-linejoin="round"
        stroke-width="2"
        d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
      />
    </svg>

    {{ buttonText }}
  </button>
</template>
