/**
 * Unit tests for usePublishAssist composable.
 * Story 5.4: 发布辅助UI组件
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { usePublishAssist } from '../usePublishAssist';
import * as publishApi from '../../api/publish';

// Mock the API module
vi.mock('../../api/publish');

const mockGetPublishAssist = vi.mocked(publishApi.getPublishAssist);
const mockRegeneratePublishAssist = vi.mocked(publishApi.regeneratePublishAssist);

describe('usePublishAssist', () => {
  const mockResponse = {
    code: 0,
    message: 'success',
    data: {
      topics: ['#话题1', '#话题2', '#话题3'],
      titles: ['标题1标题1标题1标题1', '标题2标题2标题2标题2'],
      regenerate_remaining: 3
    },
    timestamp: Date.now()
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('fetchPublishAssist', () => {
    it('should fetch and update state on success', async () => {
      mockGetPublishAssist.mockResolvedValue(mockResponse);

      const { topics, titles, regenerateRemaining, loading, error, fetchPublishAssist } = usePublishAssist();

      await fetchPublishAssist(123);

      expect(mockGetPublishAssist).toHaveBeenCalledWith(123);
      expect(topics.value).toEqual(['#话题1', '#话题2', '#话题3']);
      expect(titles.value).toEqual(['标题1标题1标题1标题1', '标题2标题2标题2标题2']);
      expect(regenerateRemaining.value).toBe(3);
      expect(loading.value).toBe(false);
      expect(error.value).toBeNull();
    });

    it('should set loading state during fetch', async () => {
      let resolveFetch: (value: any) => void;
      mockGetPublishAssist.mockImplementation(() => new Promise(resolve => {
        resolveFetch = resolve;
      }));

      const { loading, fetchPublishAssist } = usePublishAssist();

      const fetchPromise = fetchPublishAssist(123);
      expect(loading.value).toBe(true);

      resolveFetch!(mockResponse);
      await fetchPromise;

      expect(loading.value).toBe(false);
    });

    it('should set error on API failure', async () => {
      mockGetPublishAssist.mockRejectedValue(new Error('Network error'));

      const { error, fetchPublishAssist } = usePublishAssist();

      await fetchPublishAssist(123);

      expect(error.value).toBe('Network error');
    });

    it('should set error on non-zero response code', async () => {
      mockGetPublishAssist.mockResolvedValue({
        code: 40303,
        message: '任务不存在',
        data: null as any,
        timestamp: Date.now()
      });

      const { error, fetchPublishAssist } = usePublishAssist();

      await fetchPublishAssist(999);

      expect(error.value).toBe('任务不存在');
    });
  });

  describe('regenerate', () => {
    it('should regenerate and update state on success', async () => {
      const regenerateResponse = {
        ...mockResponse,
        data: {
          topics: ['#新话题1', '#新话题2'],
          titles: ['新标题1新标题1新标题1新标题1'],
          regenerate_remaining: 2
        }
      };
      mockRegeneratePublishAssist.mockResolvedValue(regenerateResponse);

      const { topics, titles, regenerateRemaining, regenerate, fetchPublishAssist } = usePublishAssist();

      // First set initial state
      mockGetPublishAssist.mockResolvedValue(mockResponse);
      await fetchPublishAssist(123);

      // Then regenerate
      const success = await regenerate(123);

      expect(success).toBe(true);
      expect(mockRegeneratePublishAssist).toHaveBeenCalledWith(123);
      expect(topics.value).toEqual(['#新话题1', '#新话题2']);
      expect(titles.value).toEqual(['新标题1新标题1新标题1新标题1']);
      expect(regenerateRemaining.value).toBe(2);
    });

    it('should return false when regenerateRemaining is 0', async () => {
      const { regenerateRemaining, regenerate, error } = usePublishAssist();

      // Manually set remaining to 0
      (regenerateRemaining as any).value = 0;

      const success = await regenerate(123);

      expect(success).toBe(false);
      expect(mockRegeneratePublishAssist).not.toHaveBeenCalled();
      expect(error.value).toBe('已达到重新生成次数上限');
    });

    it('should handle limit exceeded error (code 1031)', async () => {
      mockGetPublishAssist.mockResolvedValue(mockResponse);
      mockRegeneratePublishAssist.mockResolvedValue({
        code: 1031,
        message: '已达到重新生成次数上限（3次）',
        data: null as any,
        timestamp: Date.now()
      });

      const { regenerateRemaining, error, regenerate, fetchPublishAssist } = usePublishAssist();

      await fetchPublishAssist(123);
      const success = await regenerate(123);

      expect(success).toBe(false);
      expect(regenerateRemaining.value).toBe(0);
      expect(error.value).toContain('上限');
    });

    it('should set regenerating state during operation', async () => {
      mockGetPublishAssist.mockResolvedValue(mockResponse);

      let resolveRegenerate: (value: any) => void;
      mockRegeneratePublishAssist.mockImplementation(() => new Promise(resolve => {
        resolveRegenerate = resolve;
      }));

      const { regenerating, regenerate, fetchPublishAssist } = usePublishAssist();

      await fetchPublishAssist(123);

      const regeneratePromise = regenerate(123);
      expect(regenerating.value).toBe(true);

      resolveRegenerate!(mockResponse);
      await regeneratePromise;

      expect(regenerating.value).toBe(false);
    });
  });

  describe('clearError', () => {
    it('should clear error message', async () => {
      mockGetPublishAssist.mockRejectedValue(new Error('Test error'));

      const { error, fetchPublishAssist, clearError } = usePublishAssist();

      await fetchPublishAssist(123);
      expect(error.value).toBe('Test error');

      clearError();
      expect(error.value).toBeNull();
    });
  });
});
