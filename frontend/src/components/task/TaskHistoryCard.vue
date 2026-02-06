<!--
  Task History Card Component
  Story 5.5: 历史任务管理

  Displays a task summary card with status, shop info, and delete button.
-->
<script setup lang="ts">
import { computed } from 'vue';
import type { TaskSummary, TaskStatus } from '../../types/task';
import { SHOP_TYPE_LABELS } from '../../types/task';

const props = defineProps<{
  task: TaskSummary;
}>();

const emit = defineEmits<{
  /** Emitted when card is clicked */
  click: [task: TaskSummary];
  /** Emitted when delete button is clicked */
  delete: [task: TaskSummary];
}>();

/**
 * Status icon configuration.
 */
const statusConfig = computed(() => {
  const status = props.task.status;
  switch (status) {
    case 'completed':
      return { icon: '✓', bgClass: 'bg-green-100', textClass: 'text-green-600', label: '已完成' };
    case 'composing':
    case 'analyzing':
      return { icon: '◌', bgClass: 'bg-blue-100', textClass: 'text-blue-600', label: '处理中', spin: true };
    case 'failed':
      return { icon: '✕', bgClass: 'bg-red-100', textClass: 'text-red-600', label: '失败' };
    default:
      return { icon: '○', bgClass: 'bg-gray-100', textClass: 'text-gray-500', label: '进行中' };
  }
});

/**
 * Format relative time from ISO date string.
 */
const relativeTime = computed(() => {
  const date = new Date(props.task.created_at);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);

  if (diffSec < 60) return '刚刚';
  if (diffMin < 60) return `${diffMin}分钟前`;
  if (diffHour < 24) return `${diffHour}小时前`;
  if (diffDay < 7) return `${diffDay}天前`;

  // Format as date for older entries
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  });
});

/**
 * Shop type label.
 */
const shopTypeLabel = computed(() => SHOP_TYPE_LABELS[props.task.shop_type] || '其他');

/**
 * Check if task can be deleted (not in progress).
 */
const canDelete = computed(() => {
  const status = props.task.status as TaskStatus;
  return status !== 'analyzing' && status !== 'composing';
});

const handleCardClick = (): void => {
  emit('click', props.task);
};

const handleDeleteClick = (e: Event): void => {
  e.stopPropagation();
  if (canDelete.value) {
    emit('delete', props.task);
  }
};
</script>

<template>
  <div
    class="task-history-card bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition-shadow cursor-pointer"
    @click="handleCardClick"
  >
    <div class="flex items-start space-x-3">
      <!-- Thumbnail or placeholder -->
      <div class="flex-shrink-0 w-16 h-16 rounded-lg overflow-hidden bg-gray-100">
        <img
          v-if="task.thumbnail_url"
          :src="task.thumbnail_url"
          :alt="task.shop_name"
          class="w-full h-full object-cover"
          @error="($event.target as HTMLImageElement).style.display = 'none'"
        />
        <div
          v-else
          class="w-full h-full flex items-center justify-center text-gray-400 text-2xl"
        >
          🎬
        </div>
      </div>

      <!-- Content -->
      <div class="flex-1 min-w-0">
        <!-- Shop name -->
        <h3 class="text-sm font-medium text-gray-900 truncate">
          {{ task.shop_name }}
        </h3>

        <!-- Shop type tag and status -->
        <div class="flex items-center space-x-2 mt-1">
          <!-- Shop type -->
          <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-600">
            {{ shopTypeLabel }}
          </span>

          <!-- Status -->
          <span
            :class="[
              'inline-flex items-center px-2 py-0.5 rounded text-xs font-medium',
              statusConfig.bgClass,
              statusConfig.textClass
            ]"
          >
            <span
              :class="{ 'animate-spin': statusConfig.spin }"
              class="mr-1"
            >
              {{ statusConfig.icon }}
            </span>
            {{ statusConfig.label }}
          </span>
        </div>

        <!-- Time -->
        <p class="text-xs text-gray-400 mt-1">
          {{ relativeTime }}
        </p>
      </div>

      <!-- Delete button -->
      <button
        type="button"
        class="flex-shrink-0 p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded transition-colors"
        :class="{ 'opacity-50 cursor-not-allowed': !canDelete }"
        :disabled="!canDelete"
        :title="canDelete ? '删除任务' : '任务处理中，无法删除'"
        @click="handleDeleteClick"
      >
        <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
          />
        </svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
.task-history-card {
  transition: box-shadow 0.2s ease, transform 0.1s ease;
}

.task-history-card:active {
  transform: scale(0.99);
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  display: inline-block;
  animation: spin 1s linear infinite;
}
</style>
