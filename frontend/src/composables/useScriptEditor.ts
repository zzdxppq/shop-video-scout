/**
 * Script editor composable for managing edit state and keyboard shortcuts.
 * Story 3.2: 脚本编辑页面 - T1
 */
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useScriptStore } from '../stores/script';
import type { CharCountInfo } from '../types/script';
import { SCRIPT_VALIDATION } from '../types/script';

export function useScriptEditor(taskId: number) {
  const store = useScriptStore();
  const editingParagraphId = ref<string | null>(null);
  const editText = ref('');
  const saveDebounceTimer = ref<number | null>(null);

  // Computed
  const isEditing = computed(() => editingParagraphId.value !== null);

  /**
   * Get character count info for a paragraph.
   * BR-1.2: Suggested chars = estimatedDuration * 4
   */
  function getCharCountInfo(paragraphId: string): CharCountInfo {
    const text = store.getParagraphText(paragraphId);
    const paragraph = store.paragraphs.find(p => p.id === paragraphId);
    const suggestedChars = (paragraph?.estimatedDuration ?? 10) * SCRIPT_VALIDATION.CHARS_PER_SECOND;

    return {
      current: text.length,
      suggested: suggestedChars,
      isOverLimit: text.length > SCRIPT_VALIDATION.MAX_PARAGRAPH_LENGTH
    };
  }

  /**
   * Start editing a paragraph.
   */
  function startEdit(paragraphId: string): void {
    if (editingParagraphId.value && editingParagraphId.value !== paragraphId) {
      // Save current edit before switching
      commitEdit();
    }

    editingParagraphId.value = paragraphId;
    editText.value = store.getParagraphText(paragraphId);
  }

  /**
   * Update edit text (called on input).
   */
  function updateEditText(text: string): void {
    editText.value = text;

    // Debounced update to store
    if (saveDebounceTimer.value) {
      clearTimeout(saveDebounceTimer.value);
    }
    saveDebounceTimer.value = window.setTimeout(() => {
      if (editingParagraphId.value) {
        store.updateParagraph(editingParagraphId.value, editText.value);
      }
    }, 300);
  }

  /**
   * Commit current edit to store.
   */
  function commitEdit(): void {
    if (saveDebounceTimer.value) {
      clearTimeout(saveDebounceTimer.value);
      saveDebounceTimer.value = null;
    }

    if (editingParagraphId.value) {
      store.updateParagraph(editingParagraphId.value, editText.value);
      editingParagraphId.value = null;
      editText.value = '';
    }
  }

  /**
   * Cancel current edit.
   */
  function cancelEdit(): void {
    if (saveDebounceTimer.value) {
      clearTimeout(saveDebounceTimer.value);
      saveDebounceTimer.value = null;
    }

    if (editingParagraphId.value) {
      store.cancelEdit(editingParagraphId.value);
      editingParagraphId.value = null;
      editText.value = '';
    }
  }

  /**
   * Validate paragraph text.
   */
  function validateParagraph(text: string): { isValid: boolean; error: string | null } {
    if (!text || text.trim().length === 0) {
      return { isValid: false, error: '脚本内容不能为空' };
    }
    if (text.length > SCRIPT_VALIDATION.MAX_PARAGRAPH_LENGTH) {
      return { isValid: false, error: `段落内容不能超过${SCRIPT_VALIDATION.MAX_PARAGRAPH_LENGTH}字` };
    }
    return { isValid: true, error: null };
  }

  /**
   * Check if all paragraphs are valid.
   */
  function validateAll(): { isValid: boolean; errors: string[] } {
    const errors: string[] = [];

    for (const p of store.paragraphs) {
      const text = store.getParagraphText(p.id);
      const validation = validateParagraph(text);
      if (!validation.isValid) {
        errors.push(`${p.section}: ${validation.error}`);
      }
    }

    return { isValid: errors.length === 0, errors };
  }

  // Keyboard handlers
  function handleKeyDown(event: KeyboardEvent): void {
    // Ctrl+Z / Cmd+Z for undo
    if ((event.ctrlKey || event.metaKey) && event.key === 'z') {
      event.preventDefault();
      store.undo();

      // Update edit text if currently editing the undone paragraph
      if (editingParagraphId.value) {
        editText.value = store.getParagraphText(editingParagraphId.value);
      }
    }

    // Escape to cancel edit
    if (event.key === 'Escape' && isEditing.value) {
      event.preventDefault();
      cancelEdit();
    }
  }

  // beforeunload handler (BR-1.4, BR-2.2)
  function handleBeforeUnload(event: BeforeUnloadEvent): void {
    if (store.hasChanges) {
      event.preventDefault();
      event.returnValue = '您有未保存的修改，确定要离开吗？';
    }
  }

  // Lifecycle
  onMounted(() => {
    document.addEventListener('keydown', handleKeyDown);
    window.addEventListener('beforeunload', handleBeforeUnload);

    // Fetch script
    store.fetchScript(taskId);
  });

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeyDown);
    window.removeEventListener('beforeunload', handleBeforeUnload);

    if (saveDebounceTimer.value) {
      clearTimeout(saveDebounceTimer.value);
    }
  });

  return {
    // State
    editingParagraphId,
    editText,
    isEditing,

    // Methods
    getCharCountInfo,
    startEdit,
    updateEditText,
    commitEdit,
    cancelEdit,
    validateParagraph,
    validateAll
  };
}
