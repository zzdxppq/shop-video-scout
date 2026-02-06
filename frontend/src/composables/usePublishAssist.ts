/**
 * Publish assist composable.
 * Story 5.4: 发布辅助UI组件
 *
 * Manages state for publish assist panel (topics, titles, regeneration).
 */
import { ref, readonly } from 'vue';
import { getPublishAssist, regeneratePublishAssist } from '../api/publish';

export interface UsePublishAssistReturn {
  /** Recommended topics */
  topics: Readonly<typeof topics>;
  /** Recommended titles */
  titles: Readonly<typeof titles>;
  /** Remaining regeneration count */
  regenerateRemaining: Readonly<typeof regenerateRemaining>;
  /** Loading state for initial fetch */
  loading: Readonly<typeof loading>;
  /** Loading state for regeneration */
  regenerating: Readonly<typeof regenerating>;
  /** Error message */
  error: Readonly<typeof error>;
  /** Fetch publish assist data */
  fetchPublishAssist: (taskId: number) => Promise<void>;
  /** Regenerate content */
  regenerate: (taskId: number) => Promise<boolean>;
  /** Clear error */
  clearError: () => void;
}

const topics = ref<string[]>([]);
const titles = ref<string[]>([]);
const regenerateRemaining = ref(3);
const loading = ref(false);
const regenerating = ref(false);
const error = ref<string | null>(null);

/**
 * Composable for publish assist state management.
 *
 * @example
 * const { topics, titles, fetchPublishAssist, regenerate } = usePublishAssist();
 * await fetchPublishAssist(taskId);
 */
export function usePublishAssist(): UsePublishAssistReturn {
  /**
   * Fetch publish assist data.
   *
   * @param taskId - Task ID
   */
  const fetchPublishAssist = async (taskId: number): Promise<void> => {
    loading.value = true;
    error.value = null;

    try {
      const response = await getPublishAssist(taskId);

      if (response.code !== 0 && response.code !== 200) {
        throw new Error(response.message || '加载失败');
      }

      topics.value = response.data.topics || [];
      titles.value = response.data.titles || [];
      regenerateRemaining.value = response.data.regenerate_remaining ?? 3;
    } catch (e) {
      error.value = e instanceof Error ? e.message : '加载失败，请重试';
      console.error('Failed to fetch publish assist:', e);
    } finally {
      loading.value = false;
    }
  };

  /**
   * Regenerate publish assist content.
   *
   * @param taskId - Task ID
   * @returns true if regeneration succeeded
   */
  const regenerate = async (taskId: number): Promise<boolean> => {
    if (regenerateRemaining.value <= 0) {
      error.value = '已达到重新生成次数上限';
      return false;
    }

    regenerating.value = true;
    error.value = null;

    try {
      const response = await regeneratePublishAssist(taskId);

      if (response.code !== 0 && response.code !== 200) {
        // Handle limit exceeded error
        if (response.code === 1031) {
          regenerateRemaining.value = 0;
          throw new Error('已达到重新生成次数上限（3次）');
        }
        throw new Error(response.message || '重新生成失败');
      }

      topics.value = response.data.topics || [];
      titles.value = response.data.titles || [];
      regenerateRemaining.value = response.data.regenerate_remaining ?? 0;
      return true;
    } catch (e) {
      error.value = e instanceof Error ? e.message : '重新生成失败，请重试';
      console.error('Failed to regenerate publish assist:', e);
      return false;
    } finally {
      regenerating.value = false;
    }
  };

  /**
   * Clear error message.
   */
  const clearError = (): void => {
    error.value = null;
  };

  return {
    topics: readonly(topics),
    titles: readonly(titles),
    regenerateRemaining: readonly(regenerateRemaining),
    loading: readonly(loading),
    regenerating: readonly(regenerating),
    error: readonly(error),
    fetchPublishAssist,
    regenerate,
    clearError
  };
}
