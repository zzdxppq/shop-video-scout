/**
 * Task history composable.
 * Story 5.5: 历史任务管理
 *
 * Manages task history list with pagination and delete functionality.
 */
import { ref, computed, type Ref, type ComputedRef } from 'vue';
import { useInfiniteScroll } from './useInfiniteScroll';
import { getTaskHistory, deleteTask } from '../api/task';
import type { TaskSummary } from '../types/task';

const PAGE_SIZE = 10;

export interface UseTaskHistoryReturn {
  tasks: Ref<TaskSummary[]>;
  loading: Ref<boolean>;
  loadingMore: Ref<boolean>;
  hasMore: Ref<boolean>;
  error: Ref<string | null>;
  isEmpty: ComputedRef<boolean>;
  deleting: Ref<boolean>;
  deleteError: Ref<string | null>;
  loadMore: () => Promise<void>;
  refresh: () => Promise<void>;
  deleteTaskById: (taskId: number) => Promise<boolean>;
  sentinelRef: Ref<HTMLElement | null>;
}

/**
 * Composable for task history management.
 */
export function useTaskHistory(): UseTaskHistoryReturn {
  const deleting = ref(false);
  const deleteError = ref<string | null>(null);

  // Track tasks to delete for optimistic removal
  const pendingDeleteId = ref<number | null>(null);
  const deletedTask = ref<{ task: TaskSummary; index: number } | null>(null);

  const {
    items: tasks,
    loading,
    loadingMore,
    hasMore,
    error,
    loadMore,
    refresh: baseRefresh,
    sentinelRef
  } = useInfiniteScroll<TaskSummary>({
    loadFn: async (page: number) => {
      const response = await getTaskHistory({ page, size: PAGE_SIZE });
      if (response.code !== 0) {
        throw new Error(response.message || '加载任务列表失败');
      }
      return {
        items: response.data.items,
        has_more: response.data.has_more
      };
    }
  });

  const isEmpty = computed(() => !loading.value && tasks.value.length === 0);

  /**
   * Refresh and reload from first page.
   */
  const refresh = async (): Promise<void> => {
    deleteError.value = null;
    await baseRefresh();
  };

  /**
   * Delete a task with optimistic removal.
   *
   * @param taskId task ID to delete
   * @returns true if deleted successfully, false otherwise
   */
  const deleteTaskById = async (taskId: number): Promise<boolean> => {
    if (deleting.value) return false;

    deleting.value = true;
    deleteError.value = null;
    pendingDeleteId.value = taskId;

    // Find and remove task optimistically
    const taskIndex = tasks.value.findIndex((t) => t.id === taskId);
    if (taskIndex !== -1) {
      deletedTask.value = { task: tasks.value[taskIndex], index: taskIndex };
      tasks.value = tasks.value.filter((t) => t.id !== taskId);
    }

    try {
      const response = await deleteTask(taskId);
      if (response.code !== 0) {
        // Handle specific error codes
        if (response.code === 40401 || response.code === 404) {
          // Task not found - already deleted, don't rollback
          return true;
        }
        throw new Error(response.message || '删除失败');
      }
      // Success - clear stored deleted task
      deletedTask.value = null;
      return true;
    } catch (e) {
      // Rollback optimistic removal
      if (deletedTask.value && deletedTask.value.task.id === taskId) {
        const { task, index } = deletedTask.value;
        const newTasks = [...tasks.value];
        newTasks.splice(index, 0, task);
        tasks.value = newTasks;
      }
      deleteError.value = e instanceof Error ? e.message : '删除失败，请重试';
      return false;
    } finally {
      deleting.value = false;
      pendingDeleteId.value = null;
      deletedTask.value = null;
    }
  };

  return {
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
  };
}
