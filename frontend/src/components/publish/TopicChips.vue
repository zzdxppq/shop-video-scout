<script setup lang="ts">
/**
 * Topic chips component.
 * Story 5.4: 发布辅助UI组件 - AC1
 *
 * Displays topic hashtags as clickable chips.
 * - Click chip to copy single topic
 * - "一键复制" button to copy all topics
 */
import { computed } from 'vue';
import { useClipboard } from '../../composables/useClipboard';

interface Props {
  /** Array of topic strings (e.g., ["#话题1", "#话题2"]) */
  topics: string[];
}

interface Emits {
  /** Emitted when a topic is copied */
  (e: 'copy', topic: string): void;
  /** Emitted when all topics are copied */
  (e: 'copyAll'): void;
  /** Emitted on copy error */
  (e: 'error', message: string): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const { copyToClipboard } = useClipboard();

/** Check if topics array is empty */
const isEmpty = computed(() => !props.topics || props.topics.length === 0);

/**
 * Copy a single topic to clipboard.
 */
async function handleChipClick(topic: string) {
  const success = await copyToClipboard(topic);
  if (success) {
    emit('copy', topic);
  } else {
    emit('error', '复制失败，请手动复制');
  }
}

/**
 * Copy all topics to clipboard.
 */
async function handleCopyAll() {
  const allTopics = props.topics.join(' ');
  const success = await copyToClipboard(allTopics);
  if (success) {
    emit('copyAll');
  } else {
    emit('error', '复制失败，请手动复制');
  }
}
</script>

<template>
  <div class="topic-chips">
    <!-- Empty state -->
    <div v-if="isEmpty" class="text-center py-4 text-gray-400">
      暂无话题推荐
    </div>

    <!-- Topics container -->
    <template v-else>
      <!-- Chips container -->
      <div class="flex flex-wrap gap-2 mb-3">
        <button
          v-for="(topic, index) in topics"
          :key="index"
          class="px-3 py-1 bg-blue-100 text-blue-700 rounded-full text-sm font-medium hover:bg-blue-200 transition-colors cursor-pointer"
          :title="`点击复制: ${topic}`"
          @click="handleChipClick(topic)"
        >
          {{ topic }}
        </button>
      </div>

      <!-- Copy all button -->
      <button
        class="w-full py-2 text-sm text-blue-600 bg-blue-50 rounded-lg hover:bg-blue-100 transition-colors font-medium"
        @click="handleCopyAll"
      >
        一键复制全部话题
      </button>
    </template>
  </div>
</template>
