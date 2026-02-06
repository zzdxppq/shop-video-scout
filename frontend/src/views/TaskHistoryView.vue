<!--
  Task History View
  Story 5.5: 历史任务管理

  Displays paginated list of user's tasks with infinite scroll and delete functionality.
-->
<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useTaskHistory } from '../composables/useTaskHistory';
import TaskHistoryCard from '../components/task/TaskHistoryCard.vue';
import EmptyState from '../components/common/EmptyState.vue';
import DeleteConfirmModal from '../components/common/DeleteConfirmModal.vue';
import type { TaskSummary, TaskStatus } from '../types/task';

const router = useRouter();

const {
  tasks,
  loading,
  loadingMore,
  hasMore,
  error,
  isEmpty,
  deleting,
  deleteError,
  loadMore,
  refresh,
  deleteTaskById,
  sentinelRef
} = useTaskHistory();

// Delete modal state
const showDeleteModal = ref(false);
const taskToDelete = ref<TaskSummary | null>(null);
const toastMessage = ref<string | null>(null);
const toastType = ref<'success' | 'error'>('success');

/**
 * Navigate to appropriate page based on task status.
 */
const navigateToTask = (task: TaskSummary): void => {
  const status = task.status as TaskStatus;
  switch (status) {
    case 'completed':
      router.push(`/task/${task.id}/preview`);
      break;
    case 'script_ready':
    case 'script_edited':
      router.push(`/task/${task.id}/script`);
      break;
    case 'composing':
      router.push(`/task/${task.id}/progress`);
      break;
    case 'voice_set':
      router.push(`/task/${task.id}/voice`);
      break;
    default:
      router.push(`/task/${task.id}`);
  }
};

/**
 * Handle task card click.
 */
const handleTaskClick = (task: TaskSummary): void => {
  navigateToTask(task);
};

/**
 * Handle delete button click.
 */
const handleDeleteClick = (task: TaskSummary): void => {
  taskToDelete.value = task;
  showDeleteModal.value = true;
};

/**
 * Confirm delete action.
 */
const confirmDelete = async (): Promise<void> => {
  if (!taskToDelete.value) return;

  const success = await deleteTaskById(taskToDelete.value.id);

  if (success) {
    showToast('任务已删除', 'success');
  } else {
    showToast(deleteError.value || '删除失败', 'error');
  }

  showDeleteModal.value = false;
  taskToDelete.value = null;
};

/**
 * Cancel delete action.
 */
const cancelDelete = (): void => {
  showDeleteModal.value = false;
  taskToDelete.value = null;
};

/**
 * Show toast notification.
 */
const showToast = (message: string, type: 'success' | 'error'): void => {
  toastMessage.value = message;
  toastType.value = type;
  setTimeout(() => {
    toastMessage.value = null;
  }, 3000);
};

/**
 * Handle create task action from empty state.
 */
const handleCreateTask = (): void => {
  router.push('/create');
};

/**
 * Handle retry on error.
 */
const handleRetry = (): void => {
  refresh();
};

// Load initial data
onMounted(() => {
  loadMore();
});
</script>

<template>
  <div class="task-history-view min-h-screen bg-gray-50">
    <!-- Header -->
    <header class="bg-white shadow-sm sticky top-0 z-10">
      <div class="max-w-lg mx-auto px-4 py-4">
        <h1 class="text-xl font-semibold text-gray-900">历史任务</h1>
      </div>
    </header>

    <!-- Main content -->
    <main class="max-w-lg mx-auto px-4 py-4">
      <!-- Loading skeleton -->
      <div v-if="loading" class="space-y-3">
        <div
          v-for="i in 3"
          :key="i"
          class="bg-white rounded-lg border border-gray-200 p-4 animate-pulse"
        >
          <div class="flex items-start space-x-3">
            <div class="w-16 h-16 rounded-lg bg-gray-200"></div>
            <div class="flex-1 space-y-2">
              <div class="h-4 bg-gray-200 rounded w-3/4"></div>
              <div class="h-3 bg-gray-200 rounded w-1/2"></div>
              <div class="h-3 bg-gray-200 rounded w-1/4"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Error state -->
      <div
        v-else-if="error"
        class="bg-white rounded-lg border border-red-200 p-6 text-center"
      >
        <p class="text-red-600 mb-4">{{ error }}</p>
        <button
          type="button"
          class="px-4 py-2 text-sm font-medium text-white bg-blue-500 rounded-md hover:bg-blue-600 transition-colors"
          @click="handleRetry"
        >
          重试
        </button>
      </div>

      <!-- Empty state -->
      <EmptyState
        v-else-if="isEmpty"
        title="暂无任务记录"
        description="开始创建您的第一个视频任务吧"
        action-text="创建任务"
        @action="handleCreateTask"
      />

      <!-- Task list -->
      <div v-else class="space-y-3">
        <TaskHistoryCard
          v-for="task in tasks"
          :key="task.id"
          :task="task"
          @click="handleTaskClick"
          @delete="handleDeleteClick"
        />

        <!-- Loading more indicator -->
        <div
          v-if="loadingMore"
          class="flex justify-center py-4"
        >
          <svg
            class="animate-spin h-6 w-6 text-blue-500"
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
        </div>

        <!-- End of list -->
        <div
          v-else-if="!hasMore && tasks.length > 0"
          class="text-center py-4 text-sm text-gray-400"
        >
          已加载全部任务
        </div>

        <!-- Infinite scroll sentinel -->
        <div ref="sentinelRef" class="h-1"></div>
      </div>
    </main>

    <!-- Delete confirmation modal -->
    <DeleteConfirmModal
      :visible="showDeleteModal"
      title="删除任务"
      message="删除后无法恢复，相关视频和素材将被永久删除。确认删除吗？"
      :loading="deleting"
      @confirm="confirmDelete"
      @cancel="cancelDelete"
    />

    <!-- Toast notification -->
    <Transition name="toast">
      <div
        v-if="toastMessage"
        :class="[
          'fixed bottom-20 left-1/2 transform -translate-x-1/2 px-4 py-2 rounded-lg shadow-lg text-sm text-white z-50',
          toastType === 'success' ? 'bg-green-500' : 'bg-red-500'
        ]"
      >
        {{ toastMessage }}
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(20px);
}
</style>
