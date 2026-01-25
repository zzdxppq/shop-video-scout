/**
 * Progress polling composable.
 * Story 2.4: 分析进度展示页面
 *
 * BR-1.1: Polls every 2 seconds
 * BR-1.5: Handles beforeunload during analysis
 */
import { ref, onMounted, onUnmounted, type Ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAnalysisStore } from '../stores/analysis';
import { getAnalysisProgress, startAnalysis } from '../api/analysis';
import {
  POLLING_INTERVAL_MS,
  REDIRECT_DELAY_MS,
  MAX_POLL_RETRIES,
  LONG_WAIT_WARNING_MS,
  LONG_WAIT_RETRY_MS
} from '../types/analysis';

export interface UseProgressPollingOptions {
  taskId: Ref<number>;
  onComplete?: () => void;
  onError?: (error: string) => void;
}

export interface UseProgressPollingReturn {
  isPolling: Ref<boolean>;
  startTime: Ref<number | null>;
  showLongWaitWarning: Ref<boolean>;
  showRetryOption: Ref<boolean>;
  startPolling: () => void;
  stopPolling: () => void;
  retry: () => Promise<void>;
}

export function useProgressPolling(options: UseProgressPollingOptions): UseProgressPollingReturn {
  const { taskId, onComplete, onError } = options;
  const router = useRouter();
  const store = useAnalysisStore();

  const isPolling = ref(false);
  const startTime = ref<number | null>(null);
  const showLongWaitWarning = ref(false);
  const showRetryOption = ref(false);
  let pollInterval: ReturnType<typeof setInterval> | null = null;
  let longWaitTimeout: ReturnType<typeof setTimeout> | null = null;
  let retryTimeout: ReturnType<typeof setTimeout> | null = null;

  /**
   * Fetch progress from API.
   */
  async function fetchProgress(): Promise<void> {
    try {
      const response = await getAnalysisProgress(taskId.value);

      if (response.code === 0 || response.code === 200) {
        store.setProgress(response.data);
        store.resetPollErrorCount();

        // Check for completion
        if (response.data.status === 'completed') {
          stopPolling();
          onComplete?.();

          // BR-1.4: Auto-redirect after 2 seconds
          setTimeout(() => {
            router.push(`/task/${taskId.value}`);
          }, REDIRECT_DELAY_MS);
        } else if (response.data.status === 'failed') {
          stopPolling();
          store.setError(response.data.error || '分析失败，请重试');
          onError?.(response.data.error || '分析失败，请重试');
        }
      } else {
        handlePollError(response.message || '获取进度失败');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '网络错误';
      handlePollError(errorMessage);
    }
  }

  /**
   * Handle polling errors with retry logic.
   */
  function handlePollError(message: string): void {
    store.incrementPollErrorCount();

    if (store.pollErrorCount >= MAX_POLL_RETRIES) {
      stopPolling();
      store.setError('网络不稳定，正在重试');
      onError?.(message);
    }
  }

  /**
   * Start polling for progress updates (BR-1.1).
   */
  function startPolling(): void {
    if (isPolling.value) return;

    isPolling.value = true;
    startTime.value = Date.now();
    store.resetPollErrorCount();

    // Initial fetch
    fetchProgress();

    // Set up interval (BR-1.1: 2 second interval)
    pollInterval = setInterval(fetchProgress, POLLING_INTERVAL_MS);

    // Set up long wait warning (3 minutes)
    longWaitTimeout = setTimeout(() => {
      showLongWaitWarning.value = true;
    }, LONG_WAIT_WARNING_MS);

    // Set up retry option (5 minutes)
    retryTimeout = setTimeout(() => {
      showRetryOption.value = true;
    }, LONG_WAIT_RETRY_MS);

    // Set up beforeunload handler (BR-1.5)
    window.addEventListener('beforeunload', handleBeforeUnload);
  }

  /**
   * Stop polling.
   */
  function stopPolling(): void {
    isPolling.value = false;

    if (pollInterval) {
      clearInterval(pollInterval);
      pollInterval = null;
    }

    if (longWaitTimeout) {
      clearTimeout(longWaitTimeout);
      longWaitTimeout = null;
    }

    if (retryTimeout) {
      clearTimeout(retryTimeout);
      retryTimeout = null;
    }

    // Remove beforeunload handler (BR-1.5)
    window.removeEventListener('beforeunload', handleBeforeUnload);
  }

  /**
   * Retry analysis.
   */
  async function retry(): Promise<void> {
    store.reset();
    showLongWaitWarning.value = false;
    showRetryOption.value = false;

    try {
      store.setLoading(true);
      const response = await startAnalysis(taskId.value);

      if (response.code === 0 || response.code === 200) {
        startPolling();
      } else {
        store.setError(response.message || '重新分析失败');
      }
    } catch (err) {
      store.setError(err instanceof Error ? err.message : '重新分析失败');
    } finally {
      store.setLoading(false);
    }
  }

  /**
   * Handle beforeunload event (BR-1.5).
   */
  function handleBeforeUnload(event: BeforeUnloadEvent): void {
    if (store.isAnalyzing) {
      event.preventDefault();
      // Modern browsers require returnValue to be set
      event.returnValue = '分析正在进行中，确定要离开吗？';
    }
  }

  // Lifecycle hooks
  onMounted(() => {
    // Start polling when component mounts
    startPolling();
  });

  onUnmounted(() => {
    // Clean up on unmount (memory leak prevention)
    stopPolling();
  });

  return {
    isPolling,
    startTime,
    showLongWaitWarning,
    showRetryOption,
    startPolling,
    stopPolling,
    retry
  };
}
