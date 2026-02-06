/**
 * Generic infinite scroll composable.
 * Story 5.5: 历史任务管理
 *
 * Uses IntersectionObserver for scroll detection.
 */
import { ref, onMounted, onUnmounted, type Ref } from 'vue';

export interface UseInfiniteScrollOptions<T> {
  /**
   * Function to load data for a specific page.
   * @param page page number (1-indexed)
   * @returns Promise resolving to items and has_more flag
   */
  loadFn: (page: number) => Promise<{ items: T[]; has_more: boolean }>;

  /**
   * Initial page number (default: 1)
   */
  initialPage?: number;
}

export interface UseInfiniteScrollReturn<T> {
  items: Ref<T[]>;
  loading: Ref<boolean>;
  loadingMore: Ref<boolean>;
  hasMore: Ref<boolean>;
  error: Ref<string | null>;
  page: Ref<number>;
  loadMore: () => Promise<void>;
  refresh: () => Promise<void>;
  sentinelRef: Ref<HTMLElement | null>;
}

/**
 * Composable for infinite scroll pagination.
 */
export function useInfiniteScroll<T>(options: UseInfiniteScrollOptions<T>): UseInfiniteScrollReturn<T> {
  const { loadFn, initialPage = 1 } = options;

  const items = ref<T[]>([]) as Ref<T[]>;
  const loading = ref(false);
  const loadingMore = ref(false);
  const hasMore = ref(true);
  const error = ref<string | null>(null);
  const page = ref(initialPage);
  const sentinelRef = ref<HTMLElement | null>(null);

  let observer: IntersectionObserver | null = null;

  /**
   * Load the next page of items.
   */
  const loadMore = async (): Promise<void> => {
    if (loading.value || loadingMore.value || !hasMore.value) {
      return;
    }

    const isInitialLoad = items.value.length === 0;
    if (isInitialLoad) {
      loading.value = true;
    } else {
      loadingMore.value = true;
    }
    error.value = null;

    try {
      const result = await loadFn(page.value);
      items.value = [...items.value, ...result.items];
      hasMore.value = result.has_more;
      page.value += 1;
    } catch (e) {
      error.value = e instanceof Error ? e.message : '加载失败，请重试';
    } finally {
      loading.value = false;
      loadingMore.value = false;
    }
  };

  /**
   * Refresh from the first page.
   */
  const refresh = async (): Promise<void> => {
    items.value = [];
    page.value = initialPage;
    hasMore.value = true;
    error.value = null;
    await loadMore();
  };

  /**
   * Setup IntersectionObserver for sentinel element.
   */
  const setupObserver = (): void => {
    if (observer) {
      observer.disconnect();
    }

    observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (entry && entry.isIntersecting && hasMore.value && !loading.value && !loadingMore.value) {
          loadMore();
        }
      },
      {
        rootMargin: '100px',
        threshold: 0.1
      }
    );

    if (sentinelRef.value) {
      observer.observe(sentinelRef.value);
    }
  };

  onMounted(() => {
    // Watch for sentinel element changes
    const checkSentinel = setInterval(() => {
      if (sentinelRef.value && observer) {
        observer.observe(sentinelRef.value);
        clearInterval(checkSentinel);
      } else if (sentinelRef.value && !observer) {
        setupObserver();
        clearInterval(checkSentinel);
      }
    }, 100);

    // Clear interval after 5 seconds
    setTimeout(() => clearInterval(checkSentinel), 5000);
  });

  onUnmounted(() => {
    if (observer) {
      observer.disconnect();
      observer = null;
    }
  });

  return {
    items,
    loading,
    loadingMore,
    hasMore,
    error,
    page,
    loadMore,
    refresh,
    sentinelRef
  };
}
