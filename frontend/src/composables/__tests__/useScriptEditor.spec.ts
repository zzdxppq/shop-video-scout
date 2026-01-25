/**
 * useScriptEditor composable tests.
 * Story 3.2: 脚本编辑页面 - T1
 *
 * Test coverage:
 * - 3.2-UNIT-Editor-001: startEdit initializes edit state
 * - 3.2-UNIT-Editor-002: commitEdit updates store
 * - 3.2-UNIT-Editor-003: cancelEdit restores state
 * - 3.2-UNIT-Editor-004: getCharCountInfo calculation
 * - 3.2-UNIT-Editor-005: validateParagraph rules
 * - 3.2-UNIT-Editor-006: validateAll checks all paragraphs
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useScriptEditor } from '../useScriptEditor';
import { useScriptStore } from '../../stores/script';
import { SCRIPT_VALIDATION } from '../../types/script';

// Mock API to prevent actual calls
vi.mock('../../api/script', () => ({
  getScript: vi.fn(() => Promise.resolve({
    code: 0,
    message: 'success',
    data: {
      id: 1,
      taskId: 100,
      paragraphs: [
        { id: 'para_1', section: '开场', shotId: 101, text: '原始文案', estimatedDuration: 10 },
        { id: 'para_2', section: '环境', shotId: 102, text: '第二段', estimatedDuration: 8 }
      ],
      version: 1,
      regenerateRemaining: 3,
      createdAt: '2026-01-25T10:00:00Z',
      updatedAt: '2026-01-25T10:00:00Z'
    }
  })),
  saveScript: vi.fn(),
  regenerateScript: vi.fn()
}));

describe('useScriptEditor', () => {
  beforeEach(() => {
    // Create fresh Pinia for each test
    const pinia = createPinia();
    setActivePinia(pinia);
    vi.clearAllMocks();
    // Clear localStorage to prevent draft restoration
    localStorage.clear();
    // Mock document event listeners
    vi.spyOn(document, 'addEventListener').mockImplementation(() => {});
    vi.spyOn(document, 'removeEventListener').mockImplementation(() => {});
    vi.spyOn(window, 'addEventListener').mockImplementation(() => {});
    vi.spyOn(window, 'removeEventListener').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  describe('initial state', () => {
    it('should have null editing paragraph initially', () => {
      const editor = useScriptEditor(100);

      expect(editor.editingParagraphId.value).toBeNull();
      expect(editor.editText.value).toBe('');
      expect(editor.isEditing.value).toBe(false);
    });
  });

  describe('startEdit', () => {
    it('should set editingParagraphId and editText', async () => {
      const store = useScriptStore();
      await store.fetchScript(100);

      const editor = useScriptEditor(100);

      editor.startEdit('para_1');

      expect(editor.editingParagraphId.value).toBe('para_1');
      expect(editor.editText.value).toBe('原始文案');
      expect(editor.isEditing.value).toBe(true);
    });

    it('should commit current edit before switching paragraphs', async () => {
      const store = useScriptStore();
      await store.fetchScript(100);

      const editor = useScriptEditor(100);

      editor.startEdit('para_1');
      editor.updateEditText('修改的内容');

      // Switch to another paragraph
      editor.startEdit('para_2');

      // First paragraph should have been committed
      expect(store.getParagraphText('para_1')).toBe('修改的内容');
      expect(editor.editingParagraphId.value).toBe('para_2');
    });
  });

  describe('updateEditText', () => {
    it('should update editText value', async () => {
      const store = useScriptStore();
      await store.fetchScript(100);

      const editor = useScriptEditor(100);

      editor.startEdit('para_1');
      editor.updateEditText('新的内容');

      expect(editor.editText.value).toBe('新的内容');
    });
  });

  describe('commitEdit', () => {
    it('should update store and clear edit state', async () => {
      const store = useScriptStore();
      await store.fetchScript(100);

      const editor = useScriptEditor(100);

      editor.startEdit('para_1');
      editor.updateEditText('最终内容');

      // Wait for debounce (or directly commit)
      editor.commitEdit();

      expect(store.getParagraphText('para_1')).toBe('最终内容');
      expect(editor.editingParagraphId.value).toBeNull();
      expect(editor.editText.value).toBe('');
    });
  });

  describe('cancelEdit', () => {
    it('should clear edit state without updating store', async () => {
      const store = useScriptStore();
      await store.fetchScript(100);

      const editor = useScriptEditor(100);

      editor.startEdit('para_1');
      editor.updateEditText('不保存的修改');

      editor.cancelEdit();

      // Original text should be preserved (cancelEdit calls store.cancelEdit which restores)
      expect(editor.editingParagraphId.value).toBeNull();
      expect(editor.editText.value).toBe('');
    });
  });

  describe('getCharCountInfo', () => {
    it('should calculate character count correctly', async () => {
      const store = useScriptStore();
      await store.fetchScript(100);

      const editor = useScriptEditor(100);

      const info = editor.getCharCountInfo('para_1');

      // 原始文案 = 4 characters
      expect(info.current).toBe(4);
      // estimatedDuration=10 * CHARS_PER_SECOND=4 = 40
      expect(info.suggested).toBe(10 * SCRIPT_VALIDATION.CHARS_PER_SECOND);
      expect(info.isOverLimit).toBe(false);
    });

    it('should mark isOverLimit when exceeding max length', async () => {
      const store = useScriptStore();
      await store.fetchScript(100);

      // Update with long text
      const longText = 'a'.repeat(SCRIPT_VALIDATION.MAX_PARAGRAPH_LENGTH + 1);
      store.updateParagraph('para_1', longText);

      const editor = useScriptEditor(100);
      const info = editor.getCharCountInfo('para_1');

      expect(info.current).toBe(SCRIPT_VALIDATION.MAX_PARAGRAPH_LENGTH + 1);
      expect(info.isOverLimit).toBe(true);
    });

    it('should return correct suggested based on estimatedDuration', async () => {
      const store = useScriptStore();
      await store.fetchScript(100);

      const editor = useScriptEditor(100);

      // para_2 has estimatedDuration=8
      const info = editor.getCharCountInfo('para_2');

      expect(info.suggested).toBe(8 * SCRIPT_VALIDATION.CHARS_PER_SECOND);
    });
  });

  describe('validateParagraph', () => {
    it('should return invalid for empty text', async () => {
      const store = useScriptStore();
      await store.fetchScript(100);

      const editor = useScriptEditor(100);

      const result = editor.validateParagraph('');

      expect(result.isValid).toBe(false);
      expect(result.error).toContain('不能为空');
    });

    it('should return invalid for whitespace-only text', async () => {
      const store = useScriptStore();
      await store.fetchScript(100);

      const editor = useScriptEditor(100);

      const result = editor.validateParagraph('   \n\t  ');

      expect(result.isValid).toBe(false);
      expect(result.error).toContain('不能为空');
    });

    it('should return invalid for text exceeding max length', async () => {
      const store = useScriptStore();
      await store.fetchScript(100);

      const editor = useScriptEditor(100);
      const longText = 'a'.repeat(SCRIPT_VALIDATION.MAX_PARAGRAPH_LENGTH + 1);

      const result = editor.validateParagraph(longText);

      expect(result.isValid).toBe(false);
      expect(result.error).toContain(`${SCRIPT_VALIDATION.MAX_PARAGRAPH_LENGTH}`);
    });

    it('should return valid for exactly max length', async () => {
      const store = useScriptStore();
      await store.fetchScript(100);

      const editor = useScriptEditor(100);
      const exactText = 'a'.repeat(SCRIPT_VALIDATION.MAX_PARAGRAPH_LENGTH);

      const result = editor.validateParagraph(exactText);

      expect(result.isValid).toBe(true);
      expect(result.error).toBeNull();
    });

    it('should return valid for normal text', async () => {
      const store = useScriptStore();
      await store.fetchScript(100);

      const editor = useScriptEditor(100);

      const result = editor.validateParagraph('这是有效的脚本内容');

      expect(result.isValid).toBe(true);
      expect(result.error).toBeNull();
    });
  });

  describe('validateAll', () => {
    it('should return valid when all paragraphs are valid', async () => {
      const store = useScriptStore();
      await store.fetchScript(100);

      // Verify store has paragraphs loaded
      expect(store.paragraphs.length).toBe(2);

      const editor = useScriptEditor(100);

      const result = editor.validateAll();

      expect(result.isValid).toBe(true);
      expect(result.errors).toEqual([]);
    });

    it('should return invalid when any paragraph is empty', async () => {
      const store = useScriptStore();
      await store.fetchScript(100);

      // Make one paragraph empty
      store.updateParagraph('para_1', '');

      const editor = useScriptEditor(100);

      const result = editor.validateAll();

      expect(result.isValid).toBe(false);
      expect(result.errors.length).toBeGreaterThan(0);
      expect(result.errors[0]).toContain('开场');
    });

    it('should collect errors from multiple invalid paragraphs', async () => {
      const store = useScriptStore();
      await store.fetchScript(100);

      // Make both paragraphs invalid
      store.updateParagraph('para_1', '');
      store.updateParagraph('para_2', '');

      const editor = useScriptEditor(100);

      const result = editor.validateAll();

      expect(result.isValid).toBe(false);
      expect(result.errors.length).toBe(2);
    });
  });
});
