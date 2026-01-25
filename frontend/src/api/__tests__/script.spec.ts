/**
 * Script API client tests.
 * Story 3.2: 脚本编辑页面 - T3
 *
 * Test coverage:
 * - 3.2-UNIT-API-001: getScript calls correct endpoint
 * - 3.2-UNIT-API-002: saveScript calls PUT with correct payload
 * - 3.2-UNIT-API-003: regenerateScript calls POST endpoint
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { getScript, saveScript, regenerateScript } from '../script';
import request from '../request';

// Mock the request module
vi.mock('../request', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
    post: vi.fn()
  }
}));

describe('Script API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getScript', () => {
    it('should call GET /tasks/{id}/script', async () => {
      const mockResponse = {
        code: 0,
        message: 'success',
        data: {
          id: 1,
          taskId: 100,
          paragraphs: [],
          version: 1,
          regenerateRemaining: 3
        }
      };
      vi.mocked(request.get).mockResolvedValueOnce(mockResponse);

      const result = await getScript(100);

      expect(request.get).toHaveBeenCalledWith('/tasks/100/script');
      expect(result).toEqual(mockResponse);
    });

    it('should handle different task IDs', async () => {
      vi.mocked(request.get).mockResolvedValueOnce({ code: 0, data: {} });

      await getScript(999);

      expect(request.get).toHaveBeenCalledWith('/tasks/999/script');
    });
  });

  describe('saveScript', () => {
    it('should call PUT /tasks/{id}/script with paragraphs', async () => {
      const mockResponse = {
        code: 0,
        message: 'success',
        data: {
          id: 1,
          taskId: 100,
          paragraphs: [],
          version: 2,
          regenerateRemaining: 3
        }
      };
      vi.mocked(request.put).mockResolvedValueOnce(mockResponse);

      const payload = {
        paragraphs: [
          { id: 'para_1', text: '修改后的内容' },
          { id: 'para_2', text: '另一段内容' }
        ]
      };

      const result = await saveScript(100, payload);

      expect(request.put).toHaveBeenCalledWith('/tasks/100/script', payload);
      expect(result).toEqual(mockResponse);
    });

    it('should pass all paragraph data correctly', async () => {
      vi.mocked(request.put).mockResolvedValueOnce({ code: 0 });

      const payload = {
        paragraphs: [
          { id: 'para_1', text: '第一段' },
          { id: 'para_2', text: '第二段' },
          { id: 'para_3', text: '第三段' }
        ]
      };

      await saveScript(200, payload);

      expect(request.put).toHaveBeenCalledWith('/tasks/200/script', {
        paragraphs: expect.arrayContaining([
          expect.objectContaining({ id: 'para_1', text: '第一段' }),
          expect.objectContaining({ id: 'para_2', text: '第二段' }),
          expect.objectContaining({ id: 'para_3', text: '第三段' })
        ])
      });
    });
  });

  describe('regenerateScript', () => {
    it('should call POST /tasks/{id}/regenerate-script', async () => {
      const mockResponse = {
        code: 0,
        message: 'success',
        data: {
          id: 1,
          taskId: 100,
          paragraphs: [],
          version: 2,
          regenerateRemaining: 2
        }
      };
      vi.mocked(request.post).mockResolvedValueOnce(mockResponse);

      const result = await regenerateScript(100);

      expect(request.post).toHaveBeenCalledWith('/tasks/100/regenerate-script');
      expect(result).toEqual(mockResponse);
    });

    it('should handle different task IDs', async () => {
      vi.mocked(request.post).mockResolvedValueOnce({ code: 0, data: {} });

      await regenerateScript(555);

      expect(request.post).toHaveBeenCalledWith('/tasks/555/regenerate-script');
    });
  });
});
