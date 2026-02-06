<script setup lang="ts">
/**
 * Title list component.
 * Story 5.4: 发布辅助UI组件 - AC2
 *
 * Displays a list of recommended titles.
 * First title is marked as recommended.
 */
import { computed } from 'vue';
import TitleCard from './TitleCard.vue';

interface Props {
  /** Array of title strings */
  titles: string[];
}

interface Emits {
  /** Emitted when a title is copied */
  (e: 'copy', title: string): void;
  /** Emitted on copy error */
  (e: 'error', message: string): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

/** Check if titles array is empty */
const isEmpty = computed(() => !props.titles || props.titles.length === 0);

/**
 * Handle copy event from TitleCard.
 */
function handleCopy(title: string) {
  emit('copy', title);
}

/**
 * Handle error event from TitleCard.
 */
function handleError(message: string) {
  emit('error', message);
}
</script>

<template>
  <div class="title-list">
    <!-- Empty state -->
    <div v-if="isEmpty" class="text-center py-4 text-gray-400">
      暂无标题推荐
    </div>

    <!-- Titles -->
    <div v-else class="space-y-2">
      <TitleCard
        v-for="(title, index) in titles"
        :key="index"
        :title="title"
        :index="index"
        :is-recommended="index === 0"
        @copy="handleCopy"
        @error="handleError"
      />
    </div>
  </div>
</template>
