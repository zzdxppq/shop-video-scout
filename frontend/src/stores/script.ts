/**
 * Script store for managing script state.
 * Story 3.2: 脚本编辑页面
 */
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type {
  ScriptResponse,
  ScriptParagraph,
  ScriptDraft,
  UndoEntry
} from '../types/script';
import { SCRIPT_VALIDATION } from '../types/script';
import { getScript, saveScript, regenerateScript } from '../api/script';

const DRAFT_KEY_PREFIX = 'script_draft_';

export const useScriptStore = defineStore('script', () => {
  // State
  const script = ref<ScriptResponse | null>(null);
  const editedParagraphs = ref<Map<string, string>>(new Map());
  const undoStack = ref<UndoEntry[]>([]);
  const isLoading = ref(false);
  const isSaving = ref(false);
  const isRegenerating = ref(false);
  const error = ref<string | null>(null);

  // Getters
  const paragraphs = computed(() => script.value?.paragraphs ?? []);

  const version = computed(() => script.value?.version ?? 0);

  const regenerateRemaining = computed(() => script.value?.regenerateRemaining ?? 0);

  const canRegenerate = computed(() => regenerateRemaining.value > 0);

  const hasChanges = computed(() => editedParagraphs.value.size > 0);

  /**
   * Get current text for a paragraph (edited or original).
   */
  function getParagraphText(paragraphId: string): string {
    if (editedParagraphs.value.has(paragraphId)) {
      return editedParagraphs.value.get(paragraphId)!;
    }
    const para = paragraphs.value.find(p => p.id === paragraphId);
    return para?.text ?? '';
  }

  /**
   * Check if a paragraph has been edited.
   */
  function isParagraphEdited(paragraphId: string): boolean {
    return editedParagraphs.value.has(paragraphId);
  }

  // Actions
  /**
   * Fetch script from API.
   */
  async function fetchScript(taskId: number): Promise<void> {
    isLoading.value = true;
    error.value = null;

    try {
      const response = await getScript(taskId);

      if (response.code === 0 && response.data) {
        script.value = response.data;

        // Check for local draft
        const draft = loadDraft(taskId);
        if (draft && draft.version === response.data.version) {
          // Restore draft edits
          draft.paragraphs.forEach(p => {
            const original = response.data!.paragraphs.find(op => op.id === p.id);
            if (original && original.text !== p.text) {
              editedParagraphs.value.set(p.id, p.text);
            }
          });
        } else if (draft) {
          // Draft is stale, clear it
          clearDraft(taskId);
        }
      } else {
        throw new Error(response.message || '加载脚本失败');
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '加载脚本失败';
      throw err;
    } finally {
      isLoading.value = false;
    }
  }

  /**
   * Update paragraph text (local edit).
   */
  function updateParagraph(paragraphId: string, text: string): void {
    const original = paragraphs.value.find(p => p.id === paragraphId);
    if (!original) return;

    // Add to undo stack
    const previousText = getParagraphText(paragraphId);
    if (previousText !== text) {
      pushUndo(paragraphId, previousText);
    }

    // Update or remove from edited map
    if (text === original.text) {
      editedParagraphs.value.delete(paragraphId);
    } else {
      editedParagraphs.value.set(paragraphId, text);
    }

    // Auto-save draft
    if (script.value) {
      saveDraft(script.value.taskId);
    }
  }

  /**
   * Cancel edit for a paragraph (restore original).
   */
  function cancelEdit(paragraphId: string): void {
    const original = paragraphs.value.find(p => p.id === paragraphId);
    if (!original) return;

    const currentText = getParagraphText(paragraphId);
    if (currentText !== original.text) {
      pushUndo(paragraphId, currentText);
    }

    editedParagraphs.value.delete(paragraphId);

    if (script.value) {
      saveDraft(script.value.taskId);
    }
  }

  /**
   * Undo last edit.
   */
  function undo(): boolean {
    if (undoStack.value.length === 0) return false;

    const entry = undoStack.value.pop()!;
    const original = paragraphs.value.find(p => p.id === entry.paragraphId);

    if (original) {
      if (entry.previousText === original.text) {
        editedParagraphs.value.delete(entry.paragraphId);
      } else {
        editedParagraphs.value.set(entry.paragraphId, entry.previousText);
      }

      if (script.value) {
        saveDraft(script.value.taskId);
      }
    }

    return true;
  }

  /**
   * Push entry to undo stack.
   */
  function pushUndo(paragraphId: string, previousText: string): void {
    undoStack.value.push({
      paragraphId,
      previousText,
      timestamp: Date.now()
    });

    // Limit undo stack size
    if (undoStack.value.length > SCRIPT_VALIDATION.MAX_UNDO_STACK) {
      undoStack.value.shift();
    }
  }

  /**
   * Save script to server.
   */
  async function save(): Promise<boolean> {
    if (!script.value || !hasChanges.value) return true;

    isSaving.value = true;
    error.value = null;

    try {
      // Build save request with all paragraphs (edited text or original)
      const paragraphsToSave = paragraphs.value.map(p => ({
        id: p.id,
        text: getParagraphText(p.id)
      }));

      const response = await saveScript(script.value.taskId, {
        paragraphs: paragraphsToSave
      });

      if (response.code === 0 && response.data) {
        script.value = response.data;
        editedParagraphs.value.clear();
        undoStack.value = [];
        clearDraft(response.data.taskId);
        return true;
      } else if (response.code === 409) {
        error.value = '脚本已被修改，请刷新后重试';
        return false;
      } else {
        throw new Error(response.message || '保存失败');
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '保存失败，请重试';
      return false;
    } finally {
      isSaving.value = false;
    }
  }

  /**
   * Regenerate script using AI.
   */
  async function regenerate(): Promise<boolean> {
    if (!script.value || !canRegenerate.value) return false;

    isRegenerating.value = true;
    error.value = null;

    try {
      const response = await regenerateScript(script.value.taskId);

      if (response.code === 0 && response.data) {
        script.value = response.data;
        editedParagraphs.value.clear();
        undoStack.value = [];
        clearDraft(response.data.taskId);
        return true;
      } else if (response.code === 429) {
        error.value = '重新生成次数已达上限，请手动编辑';
        return false;
      } else {
        throw new Error(response.message || '重新生成失败');
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '重新生成失败，请重试';
      return false;
    } finally {
      isRegenerating.value = false;
    }
  }

  // Draft management (localStorage)
  function saveDraft(taskId: number): void {
    if (!script.value) return;

    const draft: ScriptDraft = {
      taskId,
      paragraphs: paragraphs.value.map(p => ({
        id: p.id,
        text: getParagraphText(p.id)
      })),
      savedAt: Date.now(),
      version: script.value.version
    };

    try {
      localStorage.setItem(`${DRAFT_KEY_PREFIX}${taskId}`, JSON.stringify(draft));
    } catch {
      // localStorage full or unavailable, ignore
    }
  }

  function loadDraft(taskId: number): ScriptDraft | null {
    try {
      const data = localStorage.getItem(`${DRAFT_KEY_PREFIX}${taskId}`);
      if (!data) return null;

      const draft: ScriptDraft = JSON.parse(data);

      // Check expiry (24 hours)
      const expiryMs = SCRIPT_VALIDATION.DRAFT_EXPIRY_HOURS * 60 * 60 * 1000;
      if (Date.now() - draft.savedAt > expiryMs) {
        clearDraft(taskId);
        return null;
      }

      return draft;
    } catch {
      return null;
    }
  }

  function clearDraft(taskId: number): void {
    try {
      localStorage.removeItem(`${DRAFT_KEY_PREFIX}${taskId}`);
    } catch {
      // Ignore
    }
  }

  /**
   * Reset store state.
   */
  function reset(): void {
    script.value = null;
    editedParagraphs.value.clear();
    undoStack.value = [];
    isLoading.value = false;
    isSaving.value = false;
    isRegenerating.value = false;
    error.value = null;
  }

  return {
    // State
    script,
    editedParagraphs,
    undoStack,
    isLoading,
    isSaving,
    isRegenerating,
    error,

    // Getters
    paragraphs,
    version,
    regenerateRemaining,
    canRegenerate,
    hasChanges,

    // Methods
    getParagraphText,
    isParagraphEdited,

    // Actions
    fetchScript,
    updateParagraph,
    cancelEdit,
    undo,
    save,
    regenerate,
    reset
  };
});
