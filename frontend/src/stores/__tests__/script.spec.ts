/**
 * Script store tests.
 * Story 3.2: 脚本编辑页面
 *
 * Test coverage:
 * - 3.2-UNIT-Store-001: Initial state
 * - 3.2-UNIT-Store-002: getParagraphText returns original or edited text
 * - 3.2-UNIT-Store-003: updateParagraph updates edit state
 * - 3.2-UNIT-Store-004: Undo stack functionality
 * - 3.2-UNIT-Store-005: hasChanges computed
 * - 3.2-UNIT-Store-006: cancelEdit restores original
 * - 3.2-UNIT-Store-007: Draft persistence
 * - 3.2-UNIT-Store-008: Save button state (BR-2.4)
 * - 3.2-UNIT-Store-009: Paragraph 500 char boundary (BLIND)
 */
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useScriptStore } from '../script';
import type { ScriptResponse, ScriptParagraph } from '../../types/script';

// Mock API module
vi.mock('../../api/script', () => ({
  getScript: vi.fn(),
  saveScript: vi.fn(),
  regenerateScript: vi.fn()
}));

import { getScript, saveScript, regenerateScript } from '../../api/script';

/**
 * Helper to create mock paragraph.
 */
function createMockParagraph(overrides: Partial<ScriptParagraph> = {}): ScriptParagraph {
  return {
    id: `para_${Math.floor(Math.random() * 1000)}`,
    section: '开场',
    shotId: 101,
    text: '这是一段测试脚本内容',
    estimatedDuration: 10,
    ...overrides
  };
}

/**
 * Helper to create mock script response.
 */
function createMockScript(overrides: Partial<ScriptResponse> = {}): ScriptResponse {
  return {
    id: 1,
    taskId: 100,
    paragraphs: [
      createMockParagraph({ id: 'para_1', section: '开场', shotId: 101 }),
      createMockParagraph({ id: 'para_2', section: '环境展示', shotId: 102 }),
      createMockParagraph({ id: 'para_3', section: '产品特写', shotId: 103 })
    ],
    version: 1,
    regenerateRemaining: 3,
    createdAt: '2026-01-25T10:00:00Z',
    updatedAt: '2026-01-25T10:00:00Z',
    ...overrides
  };
}

describe('useScriptStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
    // Clear localStorage
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  describe('initial state', () => {
    it('should have correct initial values', () => {
      const store = useScriptStore();

      expect(store.script).toBeNull();
      expect(store.editedParagraphs.size).toBe(0);
      expect(store.undoStack).toEqual([]);
      expect(store.isLoading).toBe(false);
      expect(store.isSaving).toBe(false);
      expect(store.isRegenerating).toBe(false);
      expect(store.error).toBeNull();
    });

    it('should have correct initial computed values', () => {
      const store = useScriptStore();

      expect(store.paragraphs).toEqual([]);
      expect(store.version).toBe(0);
      expect(store.regenerateRemaining).toBe(0);
      expect(store.canRegenerate).toBe(false);
      expect(store.hasChanges).toBe(false);
    });
  });

  describe('fetchScript', () => {
    it('should fetch and set script data', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);

      expect(store.script).toEqual(mockScript);
      expect(store.paragraphs).toHaveLength(3);
      expect(store.isLoading).toBe(false);
    });

    it('should set error on fetch failure', async () => {
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 500,
        message: '服务器错误',
        data: null
      });

      const store = useScriptStore();

      await expect(store.fetchScript(100)).rejects.toThrow('服务器错误');
      expect(store.error).toBe('服务器错误');
    });

    it('should restore draft if version matches', async () => {
      const mockScript = createMockScript({ taskId: 100, version: 1 });
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      // Store draft with matching version
      const draft = {
        taskId: 100,
        paragraphs: [{ id: 'para_1', text: '修改后的内容' }],
        savedAt: Date.now(),
        version: 1
      };
      localStorage.setItem('script_draft_100', JSON.stringify(draft));

      const store = useScriptStore();
      await store.fetchScript(100);

      expect(store.editedParagraphs.has('para_1')).toBe(true);
      expect(store.getParagraphText('para_1')).toBe('修改后的内容');
    });

    it('should clear stale draft if version mismatch', async () => {
      const mockScript = createMockScript({ taskId: 100, version: 2 });
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      // Store draft with old version
      const draft = {
        taskId: 100,
        paragraphs: [{ id: 'para_1', text: '旧内容' }],
        savedAt: Date.now(),
        version: 1
      };
      localStorage.setItem('script_draft_100', JSON.stringify(draft));

      const store = useScriptStore();
      await store.fetchScript(100);

      expect(store.editedParagraphs.size).toBe(0);
      expect(localStorage.getItem('script_draft_100')).toBeNull();
    });
  });

  describe('getParagraphText', () => {
    it('should return original text when not edited', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);

      expect(store.getParagraphText('para_1')).toBe(mockScript.paragraphs[0].text);
    });

    it('should return edited text when paragraph is edited', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);
      store.updateParagraph('para_1', '新的文案内容');

      expect(store.getParagraphText('para_1')).toBe('新的文案内容');
    });

    it('should return empty string for non-existent paragraph', () => {
      const store = useScriptStore();
      expect(store.getParagraphText('non_existent')).toBe('');
    });
  });

  describe('updateParagraph', () => {
    it('should add paragraph to editedParagraphs', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);
      store.updateParagraph('para_1', '修改后的内容');

      expect(store.editedParagraphs.has('para_1')).toBe(true);
      expect(store.hasChanges).toBe(true);
    });

    it('should remove from editedParagraphs if text matches original', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);

      store.updateParagraph('para_1', '临时修改');
      expect(store.editedParagraphs.has('para_1')).toBe(true);

      store.updateParagraph('para_1', mockScript.paragraphs[0].text);
      expect(store.editedParagraphs.has('para_1')).toBe(false);
    });

    it('should push to undo stack on text change', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);
      const originalText = mockScript.paragraphs[0].text;

      store.updateParagraph('para_1', '第一次修改');

      expect(store.undoStack).toHaveLength(1);
      expect(store.undoStack[0].paragraphId).toBe('para_1');
      expect(store.undoStack[0].previousText).toBe(originalText);
    });

    it('should limit undo stack to 20 entries', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);

      // Make 25 changes
      for (let i = 0; i < 25; i++) {
        store.updateParagraph('para_1', `修改${i}`);
      }

      expect(store.undoStack.length).toBeLessThanOrEqual(20);
    });

    it('should save draft to localStorage', async () => {
      const mockScript = createMockScript({ taskId: 100 });
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);
      store.updateParagraph('para_1', '修改内容');

      const draft = JSON.parse(localStorage.getItem('script_draft_100') || '{}');
      expect(draft.taskId).toBe(100);
      expect(draft.paragraphs).toBeDefined();
    });
  });

  describe('cancelEdit', () => {
    it('should remove paragraph from editedParagraphs', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);
      store.updateParagraph('para_1', '修改内容');

      expect(store.editedParagraphs.has('para_1')).toBe(true);

      store.cancelEdit('para_1');

      expect(store.editedParagraphs.has('para_1')).toBe(false);
    });

    it('should push to undo stack before canceling', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);
      store.updateParagraph('para_1', '修改内容');

      const undoCountBefore = store.undoStack.length;
      store.cancelEdit('para_1');

      expect(store.undoStack.length).toBe(undoCountBefore + 1);
    });
  });

  describe('undo', () => {
    it('should restore previous text from undo stack', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);
      const originalText = mockScript.paragraphs[0].text;

      store.updateParagraph('para_1', '第一次修改');
      store.updateParagraph('para_1', '第二次修改');

      expect(store.getParagraphText('para_1')).toBe('第二次修改');

      store.undo();
      expect(store.getParagraphText('para_1')).toBe('第一次修改');

      store.undo();
      expect(store.getParagraphText('para_1')).toBe(originalText);
    });

    it('should return false when undo stack is empty', () => {
      const store = useScriptStore();
      expect(store.undo()).toBe(false);
    });

    it('should return true when undo succeeds', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);
      store.updateParagraph('para_1', '修改');

      expect(store.undo()).toBe(true);
    });
  });

  describe('save', () => {
    it('should call saveScript API with all paragraphs', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });
      vi.mocked(saveScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: { ...mockScript, version: 2 }
      });

      const store = useScriptStore();
      await store.fetchScript(100);
      store.updateParagraph('para_1', '修改内容');

      const result = await store.save();

      expect(result).toBe(true);
      expect(saveScript).toHaveBeenCalledWith(100, {
        paragraphs: expect.arrayContaining([
          expect.objectContaining({ id: 'para_1', text: '修改内容' })
        ])
      });
    });

    it('should clear editedParagraphs and undoStack on successful save', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });
      vi.mocked(saveScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: { ...mockScript, version: 2 }
      });

      const store = useScriptStore();
      await store.fetchScript(100);
      store.updateParagraph('para_1', '修改内容');

      await store.save();

      expect(store.editedParagraphs.size).toBe(0);
      expect(store.undoStack).toEqual([]);
    });

    it('should clear localStorage draft on successful save', async () => {
      const mockScript = createMockScript({ taskId: 100 });
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });
      vi.mocked(saveScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: { ...mockScript, version: 2 }
      });

      const store = useScriptStore();
      await store.fetchScript(100);
      store.updateParagraph('para_1', '修改内容');

      expect(localStorage.getItem('script_draft_100')).not.toBeNull();

      await store.save();

      expect(localStorage.getItem('script_draft_100')).toBeNull();
    });

    it('should handle version conflict (409)', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });
      vi.mocked(saveScript).mockResolvedValueOnce({
        code: 409,
        message: '版本冲突',
        data: null
      });

      const store = useScriptStore();
      await store.fetchScript(100);
      store.updateParagraph('para_1', '修改内容');

      const result = await store.save();

      expect(result).toBe(false);
      expect(store.error).toContain('已被修改');
    });

    it('should not call API if no changes', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);

      const result = await store.save();

      expect(result).toBe(true);
      expect(saveScript).not.toHaveBeenCalled();
    });
  });

  describe('regenerate', () => {
    it('should call regenerateScript API', async () => {
      const mockScript = createMockScript({ regenerateRemaining: 3 });
      const newScript = createMockScript({ version: 2, regenerateRemaining: 2 });
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });
      vi.mocked(regenerateScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: newScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);

      const result = await store.regenerate();

      expect(result).toBe(true);
      expect(store.script).toEqual(newScript);
      expect(store.regenerateRemaining).toBe(2);
    });

    it('should handle rate limit (429)', async () => {
      const mockScript = createMockScript({ regenerateRemaining: 1 });
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });
      vi.mocked(regenerateScript).mockResolvedValueOnce({
        code: 429,
        message: '次数已达上限',
        data: null
      });

      const store = useScriptStore();
      await store.fetchScript(100);

      const result = await store.regenerate();

      expect(result).toBe(false);
      expect(store.error).toContain('次数已达上限');
    });

    it('should not call API if regenerateRemaining is 0', async () => {
      const mockScript = createMockScript({ regenerateRemaining: 0 });
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);

      expect(store.canRegenerate).toBe(false);

      const result = await store.regenerate();

      expect(result).toBe(false);
      expect(regenerateScript).not.toHaveBeenCalled();
    });
  });

  describe('isParagraphEdited', () => {
    it('should return true for edited paragraphs', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);
      store.updateParagraph('para_1', '修改内容');

      expect(store.isParagraphEdited('para_1')).toBe(true);
      expect(store.isParagraphEdited('para_2')).toBe(false);
    });
  });

  describe('reset', () => {
    it('should reset all state to initial values', async () => {
      const mockScript = createMockScript();
      vi.mocked(getScript).mockResolvedValueOnce({
        code: 0,
        message: 'success',
        data: mockScript
      });

      const store = useScriptStore();
      await store.fetchScript(100);
      store.updateParagraph('para_1', '修改');

      store.reset();

      expect(store.script).toBeNull();
      expect(store.editedParagraphs.size).toBe(0);
      expect(store.undoStack).toEqual([]);
      expect(store.isLoading).toBe(false);
      expect(store.isSaving).toBe(false);
      expect(store.isRegenerating).toBe(false);
      expect(store.error).toBeNull();
    });
  });
});
