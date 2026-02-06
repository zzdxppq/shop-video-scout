<script setup lang="ts">
/**
 * Title card component.
 * Story 5.4: 发布辅助UI组件 - AC2
 *
 * Displays a single title with copy button and optional "推荐" badge.
 */
import { useClipboard } from '../../composables/useClipboard';

interface Props {
  /** Title text */
  title: string;
  /** Whether this is the recommended (first) title */
  isRecommended?: boolean;
  /** Index of the title (for display) */
  index: number;
}

interface Emits {
  /** Emitted when title is copied */
  (e: 'copy', title: string): void;
  /** Emitted on copy error */
  (e: 'error', message: string): void;
}

const props = withDefaults(defineProps<Props>(), {
  isRecommended: false
});

const emit = defineEmits<Emits>();

const { copyToClipboard } = useClipboard();

/**
 * Copy title to clipboard.
 */
async function handleCopy() {
  const success = await copyToClipboard(props.title);
  if (success) {
    emit('copy', props.title);
  } else {
    emit('error', '复制失败，请手动复制');
  }
}
</script>

<template>
  <div class="title-card bg-white border border-gray-200 rounded-lg p-3 flex items-start justify-between gap-3 hover:border-gray-300 transition-colors">
    <!-- Title content -->
    <div class="flex-1 min-w-0">
      <!-- Recommended badge -->
      <span
        v-if="isRecommended"
        class="inline-block px-2 py-0.5 bg-orange-100 text-orange-600 text-xs font-medium rounded mb-1"
      >
        推荐
      </span>

      <!-- Title text -->
      <p
        class="text-gray-800 text-sm leading-relaxed"
        :class="{ 'line-clamp-2': title.length > 50 }"
        :title="title.length > 50 ? title : undefined"
      >
        {{ title }}
      </p>
    </div>

    <!-- Copy button -->
    <button
      class="flex-shrink-0 p-2 text-gray-400 hover:text-blue-500 hover:bg-blue-50 rounded-lg transition-colors"
      title="点击复制"
      @click="handleCopy"
    >
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"
        />
      </svg>
    </button>
  </div>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
