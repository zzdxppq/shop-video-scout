/**
 * Unit tests for useTaskHistory composable.
 * Story 5.5: 历史任务管理
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useTaskHistory } from '../useTaskHistory';
import * as taskApi from '../../api/task';

// Mock the API module
vi.mock('../../api/task');

const mockGetTaskHistory = vi.mocked(taskApi.getTaskHistory);
const mockDeleteTask = vi.mocked(taskApi.deleteTask);

describe('useTaskHistory', () => {
  const mockTasks = [
    {
      id: 1,
      shop_name: '海底捞火锅',
      shop_type: 'food' as const,
      status: 'completed' as const,
      thumbnail_url: 'https://example.com/thumb1.jpg',
      created_at: '2026-02-05T10:00:00Z'
    },
    {
      id: 2,
      shop_name: '星巴克咖啡',
      shop_type: 'food' as const,
      status: 'composing' as const,
      thumbnail_url: null,
      created_at: '2026-02-04T15:30:00Z'
    }
  ];

  const mockPagedResponse = {
    code: 0,
    message: 'success',
    data: {
      items: mockTasks,
      total: 25,
      page: 1,
      size: 10,
      has_more: true
    },
    timestamp: Date.now()
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('loadMore', () => {
    it('should load tasks and update state', async () => {
      mockGetTaskHistory.mockResolvedValue(mockPagedResponse);

      const { tasks, loading, hasMore, loadMore } = useTaskHistory();

      await loadMore();

      expect(mockGetTaskHistory).toHaveBeenCalledWith({ page: 1, size: 10 });
      expect(tasks.value).toHaveLength(2);
      expect(loading.value).toBe(false);
      expect(hasMore.value).toBe(true);
    });

    it('should set error on API failure', async () => {
      mockGetTaskHistory.mockResolvedValue({
        ...mockPagedResponse,
        code: 500,
        message: '服务器错误'
      });

      const { error, loadMore } = useTaskHistory();

      await loadMore();

      expect(error.value).toContain('服务器错误');
    });
  });

  describe('deleteTaskById', () => {
    it('should delete task optimistically and confirm on success', async () => {
      mockGetTaskHistory.mockResolvedValue(mockPagedResponse);
      mockDeleteTask.mockResolvedValue({
        code: 0,
        message: 'success',
        data: undefined,
        timestamp: Date.now()
      });

      const { tasks, deleteTaskById, loadMore } = useTaskHistory();

      await loadMore();
      expect(tasks.value).toHaveLength(2);

      const success = await deleteTaskById(1);

      expect(success).toBe(true);
      expect(tasks.value).toHaveLength(1);
      expect(tasks.value[0].id).toBe(2);
    });

    it('should rollback on delete failure', async () => {
      mockGetTaskHistory.mockResolvedValue(mockPagedResponse);
      mockDeleteTask.mockResolvedValue({
        code: 500,
        message: '删除失败',
        data: undefined,
        timestamp: Date.now()
      });

      const { tasks, deleteTaskById, deleteError, loadMore } = useTaskHistory();

      await loadMore();
      expect(tasks.value).toHaveLength(2);

      const success = await deleteTaskById(1);

      expect(success).toBe(false);
      expect(tasks.value).toHaveLength(2);
      expect(deleteError.value).toBe('删除失败');
    });

    it('should handle task not found (already deleted)', async () => {
      mockGetTaskHistory.mockResolvedValue(mockPagedResponse);
      mockDeleteTask.mockResolvedValue({
        code: 40401,
        message: '任务不存在',
        data: undefined,
        timestamp: Date.now()
      });

      const { tasks, deleteTaskById, loadMore } = useTaskHistory();

      await loadMore();
      const success = await deleteTaskById(1);

      // Should succeed (task already gone)
      expect(success).toBe(true);
      expect(tasks.value).toHaveLength(1);
    });

    it('should set deleting state during operation', async () => {
      mockGetTaskHistory.mockResolvedValue(mockPagedResponse);

      let resolveDelete: (value: any) => void;
      mockDeleteTask.mockImplementation(() => new Promise(resolve => {
        resolveDelete = resolve;
      }));

      const { deleting, deleteTaskById, loadMore } = useTaskHistory();

      await loadMore();

      const deletePromise = deleteTaskById(1);
      expect(deleting.value).toBe(true);

      resolveDelete!({ code: 0, message: 'success', data: undefined, timestamp: Date.now() });
      await deletePromise;

      expect(deleting.value).toBe(false);
    });
  });

  describe('refresh', () => {
    it('should reload from first page', async () => {
      mockGetTaskHistory.mockResolvedValue(mockPagedResponse);

      const { tasks, refresh } = useTaskHistory();

      await refresh();

      expect(mockGetTaskHistory).toHaveBeenCalledWith({ page: 1, size: 10 });
      expect(tasks.value).toHaveLength(2);
    });
  });

  describe('isEmpty', () => {
    it('should be true when no tasks and not loading', async () => {
      mockGetTaskHistory.mockResolvedValue({
        ...mockPagedResponse,
        data: { ...mockPagedResponse.data, items: [], total: 0, has_more: false }
      });

      const { isEmpty, loadMore } = useTaskHistory();

      await loadMore();

      expect(isEmpty.value).toBe(true);
    });

    it('should be false when tasks exist', async () => {
      mockGetTaskHistory.mockResolvedValue(mockPagedResponse);

      const { isEmpty, loadMore } = useTaskHistory();

      await loadMore();

      expect(isEmpty.value).toBe(false);
    });
  });
});
