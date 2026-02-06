/**
 * Unit tests for useInfiniteScroll composable.
 * Story 5.5: 历史任务管理
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useInfiniteScroll } from '../useInfiniteScroll';
import { nextTick } from 'vue';

describe('useInfiniteScroll', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('loadMore', () => {
    it('should load first page and update items', async () => {
      const mockLoadFn = vi.fn().mockResolvedValue({
        items: [{ id: 1 }, { id: 2 }],
        has_more: true
      });

      const { items, loadMore, loading, hasMore } = useInfiniteScroll({
        loadFn: mockLoadFn
      });

      await loadMore();

      expect(mockLoadFn).toHaveBeenCalledWith(1);
      expect(items.value).toHaveLength(2);
      expect(loading.value).toBe(false);
      expect(hasMore.value).toBe(true);
    });

    it('should load subsequent pages and append items', async () => {
      const mockLoadFn = vi.fn()
        .mockResolvedValueOnce({ items: [{ id: 1 }], has_more: true })
        .mockResolvedValueOnce({ items: [{ id: 2 }], has_more: false });

      const { items, loadMore, hasMore, page } = useInfiniteScroll({
        loadFn: mockLoadFn
      });

      await loadMore();
      expect(items.value).toHaveLength(1);
      expect(page.value).toBe(2);

      await loadMore();
      expect(items.value).toHaveLength(2);
      expect(hasMore.value).toBe(false);
    });

    it('should set loading state during load', async () => {
      let resolveFn: (value: any) => void;
      const mockLoadFn = vi.fn().mockImplementation(() => new Promise(resolve => {
        resolveFn = resolve;
      }));

      const { loading, loadMore } = useInfiniteScroll({ loadFn: mockLoadFn });

      const loadPromise = loadMore();
      await nextTick();
      expect(loading.value).toBe(true);

      resolveFn!({ items: [], has_more: false });
      await loadPromise;
      expect(loading.value).toBe(false);
    });

    it('should set error on load failure', async () => {
      const mockLoadFn = vi.fn().mockRejectedValue(new Error('Network error'));

      const { error, loadMore } = useInfiniteScroll({ loadFn: mockLoadFn });

      await loadMore();
      expect(error.value).toBe('Network error');
    });

    it('should not load when already loading', async () => {
      let resolveFn: (value: any) => void;
      const mockLoadFn = vi.fn().mockImplementation(() => new Promise(resolve => {
        resolveFn = resolve;
      }));

      const { loadMore } = useInfiniteScroll({ loadFn: mockLoadFn });

      loadMore();
      loadMore();
      loadMore();

      expect(mockLoadFn).toHaveBeenCalledTimes(1);

      resolveFn!({ items: [], has_more: false });
    });

    it('should not load when hasMore is false', async () => {
      const mockLoadFn = vi.fn().mockResolvedValue({ items: [], has_more: false });

      const { loadMore } = useInfiniteScroll({ loadFn: mockLoadFn });

      await loadMore();
      await loadMore();

      expect(mockLoadFn).toHaveBeenCalledTimes(1);
    });
  });

  describe('refresh', () => {
    it('should reset state and reload from first page', async () => {
      const mockLoadFn = vi.fn()
        .mockResolvedValueOnce({ items: [{ id: 1 }], has_more: true })
        .mockResolvedValueOnce({ items: [{ id: 2 }], has_more: true })
        .mockResolvedValueOnce({ items: [{ id: 3 }], has_more: false });

      const { items, page, refresh, loadMore } = useInfiniteScroll({ loadFn: mockLoadFn });

      await loadMore();
      await loadMore();
      expect(items.value).toHaveLength(2);
      expect(page.value).toBe(3);

      await refresh();
      expect(items.value).toHaveLength(1);
      expect(page.value).toBe(2);
      expect(mockLoadFn).toHaveBeenLastCalledWith(1);
    });
  });
});
