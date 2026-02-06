<script setup lang="ts">
/**
 * Publish assist panel component.
 * Story 5.4: 发布辅助UI组件 - AC1, AC2, AC3
 *
 * Main container that composes:
 * - TopicChips: Topic hashtag display
 * - TitleList: Title recommendations
 * - RegenerateButton: Regenerate with remaining count
 * - Toast notifications for copy feedback
 */
import { ref, onMounted, watch } from 'vue';
import { usePublishAssist } from '../../composables/usePublishAssist';
import TopicChips from './TopicChips.vue';
import TitleList from './TitleList.vue';
import RegenerateButton from './RegenerateButton.vue';

interface Props {
  /** Task ID to fetch publish assist for */
  taskId: number;
}

const props = defineProps<Props>();

const {
  topics,
  titles,
  regenerateRemaining,
  loading,
  regenerating,
  error,
  fetchPublishAssist,
  regenerate,
  clearError
} = usePublishAssist();

// Toast state
const toastMessage = ref<string | null>(null);
const toastType = ref<'success' | 'error'>('success');
let toastTimeout: ReturnType<typeof setTimeout> | null = null;

/**
 * Show toast notification.
 */
function showToast(message: string, type: 'success' | 'error' = 'success') {
  if (toastTimeout) {
    clearTimeout(toastTimeout);
  }
  toastMessage.value = message;
  toastType.value = type;
  toastTimeout = setTimeout(() => {
    toastMessage.value = null;
  }, 2000);
}

/**
 * Handle topic copied.
 */
function handleTopicCopy(topic: string) {
  showToast(`已复制: ${topic}`, 'success');
}

/**
 * Handle all topics copied.
 */
function handleCopyAll() {
  showToast('已复制全部话题', 'success');
}

/**
 * Handle title copied.
 */
function handleTitleCopy(_title: string) {
  showToast('已复制到剪贴板', 'success');
}

/**
 * Handle copy error.
 */
function handleCopyError(message: string) {
  showToast(message, 'error');
}

/**
 * Handle regenerate click.
 */
async function handleRegenerate() {
  clearError();
  const success = await regenerate(props.taskId);
  if (success) {
    showToast('已重新生成推荐内容', 'success');
  } else if (error.value) {
    showToast(error.value, 'error');
  }
}

/**
 * Handle retry on error.
 */
function handleRetry() {
  fetchPublishAssist(props.taskId);
}

// Fetch data on mount
onMounted(() => {
  fetchPublishAssist(props.taskId);
});

// Re-fetch when taskId changes
watch(() => props.taskId, (newId) => {
  if (newId) {
    fetchPublishAssist(newId);
  }
});
</script>

<template>
  <div class="publish-assist-panel bg-white rounded-xl shadow-sm p-4">
    <!-- Loading state -->
    <div v-if="loading" class="flex flex-col items-center justify-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-4 border-blue-500 border-t-transparent" />
      <p class="mt-3 text-sm text-gray-500">加载推荐内容...</p>
    </div>

    <!-- Error state -->
    <div v-else-if="error && !topics.length && !titles.length" class="text-center py-8">
      <div class="w-12 h-12 mx-auto mb-3 rounded-full bg-red-100 flex items-center justify-center">
        <svg class="w-6 h-6 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      </div>
      <p class="text-sm text-gray-600 mb-4">{{ error }}</p>
      <button
        class="px-4 py-2 text-sm text-blue-600 bg-blue-50 rounded-lg hover:bg-blue-100 transition-colors"
        @click="handleRetry"
      >
        重试
      </button>
    </div>

    <!-- Content -->
    <div v-else class="space-y-6">
      <!-- Section: Topics -->
      <section>
        <h3 class="text-base font-semibold text-gray-800 mb-3 flex items-center gap-2">
          <svg class="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 20l4-16m2 16l4-16M6 9h14M4 15h14" />
          </svg>
          推荐话题
        </h3>
        <TopicChips
          :topics="topics"
          @copy="handleTopicCopy"
          @copy-all="handleCopyAll"
          @error="handleCopyError"
        />
      </section>

      <!-- Section: Titles -->
      <section>
        <h3 class="text-base font-semibold text-gray-800 mb-3 flex items-center gap-2">
          <svg class="w-5 h-5 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
          </svg>
          推荐标题
        </h3>
        <TitleList
          :titles="titles"
          @copy="handleTitleCopy"
          @error="handleCopyError"
        />
      </section>

      <!-- Regenerate button -->
      <section class="pt-2">
        <RegenerateButton
          :remaining="regenerateRemaining"
          :loading="regenerating"
          @regenerate="handleRegenerate"
        />
      </section>
    </div>

    <!-- Toast notification -->
    <Transition
      enter-active-class="transition-all duration-300 ease-out"
      enter-from-class="opacity-0 translate-y-4"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition-all duration-200 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 translate-y-4"
    >
      <div
        v-if="toastMessage"
        class="fixed bottom-20 left-1/2 transform -translate-x-1/2 px-4 py-2 rounded-lg shadow-lg text-sm font-medium z-50"
        :class="{
          'bg-green-500 text-white': toastType === 'success',
          'bg-red-500 text-white': toastType === 'error'
        }"
      >
        {{ toastMessage }}
      </div>
    </Transition>
  </div>
</template>
