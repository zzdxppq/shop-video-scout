/**
 * Clipboard composable.
 * Story 5.4: 发布辅助UI组件
 *
 * Provides clipboard copy functionality with fallback for older browsers.
 */
import { ref } from 'vue';

export interface UseClipboardReturn {
  /** Copy text to clipboard */
  copyToClipboard: (text: string) => Promise<boolean>;
  /** Whether a copy operation is in progress */
  copying: Readonly<typeof copying>;
  /** Last copied text */
  copiedText: Readonly<typeof copiedText>;
}

const copying = ref(false);
const copiedText = ref<string | null>(null);

/**
 * Composable for clipboard operations.
 *
 * @example
 * const { copyToClipboard } = useClipboard();
 * const success = await copyToClipboard('#话题1 #话题2');
 */
export function useClipboard(): UseClipboardReturn {
  /**
   * Copy text to clipboard.
   * Uses modern Clipboard API with fallback to execCommand.
   *
   * @param text - Text to copy
   * @returns true if copy succeeded, false otherwise
   */
  const copyToClipboard = async (text: string): Promise<boolean> => {
    if (!text) {
      return false;
    }

    copying.value = true;

    try {
      // Modern Clipboard API
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(text);
        copiedText.value = text;
        return true;
      }

      // Fallback for older browsers
      const textarea = document.createElement('textarea');
      textarea.value = text;
      textarea.style.position = 'fixed';
      textarea.style.left = '-9999px';
      textarea.style.top = '-9999px';
      document.body.appendChild(textarea);
      textarea.focus();
      textarea.select();

      const success = document.execCommand('copy');
      document.body.removeChild(textarea);

      if (success) {
        copiedText.value = text;
      }

      return success;
    } catch (error) {
      console.error('Clipboard copy failed:', error);
      return false;
    } finally {
      copying.value = false;
    }
  };

  return {
    copyToClipboard,
    copying,
    copiedText
  };
}
