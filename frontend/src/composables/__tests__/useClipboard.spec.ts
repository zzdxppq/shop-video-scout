/**
 * Unit tests for useClipboard composable.
 * Story 5.4: 发布辅助UI组件
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { useClipboard } from '../useClipboard';

describe('useClipboard', () => {
  let originalClipboard: typeof navigator.clipboard;
  let originalExecCommand: typeof document.execCommand;

  beforeEach(() => {
    // Store originals
    originalClipboard = navigator.clipboard;
    originalExecCommand = document.execCommand;
  });

  afterEach(() => {
    // Restore
    vi.restoreAllMocks();
    Object.defineProperty(navigator, 'clipboard', {
      value: originalClipboard,
      writable: true
    });
    document.execCommand = originalExecCommand;
  });

  describe('copyToClipboard', () => {
    it('should return false for empty text', async () => {
      const { copyToClipboard } = useClipboard();
      const result = await copyToClipboard('');
      expect(result).toBe(false);
    });

    it('should use Clipboard API when available', async () => {
      const writeTextMock = vi.fn().mockResolvedValue(undefined);
      Object.defineProperty(navigator, 'clipboard', {
        value: { writeText: writeTextMock },
        writable: true
      });

      const { copyToClipboard, copiedText } = useClipboard();
      const result = await copyToClipboard('test text');

      expect(writeTextMock).toHaveBeenCalledWith('test text');
      expect(result).toBe(true);
      expect(copiedText.value).toBe('test text');
    });

    it('should return false when Clipboard API fails', async () => {
      Object.defineProperty(navigator, 'clipboard', {
        value: { writeText: vi.fn().mockRejectedValue(new Error('Permission denied')) },
        writable: true
      });

      const { copyToClipboard } = useClipboard();
      const result = await copyToClipboard('test text');

      expect(result).toBe(false);
    });

    it('should use execCommand fallback when Clipboard API is not available', async () => {
      Object.defineProperty(navigator, 'clipboard', {
        value: undefined,
        writable: true
      });

      const execCommandMock = vi.fn().mockReturnValue(true);
      document.execCommand = execCommandMock;

      // Mock document methods
      const mockTextarea = {
        value: '',
        style: {},
        focus: vi.fn(),
        select: vi.fn()
      };
      vi.spyOn(document, 'createElement').mockReturnValue(mockTextarea as any);
      vi.spyOn(document.body, 'appendChild').mockImplementation(() => mockTextarea as any);
      vi.spyOn(document.body, 'removeChild').mockImplementation(() => mockTextarea as any);

      const { copyToClipboard } = useClipboard();
      const result = await copyToClipboard('fallback text');

      expect(mockTextarea.value).toBe('fallback text');
      expect(execCommandMock).toHaveBeenCalledWith('copy');
      expect(result).toBe(true);
    });

    it('should return false when execCommand returns false', async () => {
      Object.defineProperty(navigator, 'clipboard', {
        value: undefined,
        writable: true
      });

      document.execCommand = vi.fn().mockReturnValue(false);

      const mockTextarea = {
        value: '',
        style: {},
        focus: vi.fn(),
        select: vi.fn()
      };
      vi.spyOn(document, 'createElement').mockReturnValue(mockTextarea as any);
      vi.spyOn(document.body, 'appendChild').mockImplementation(() => mockTextarea as any);
      vi.spyOn(document.body, 'removeChild').mockImplementation(() => mockTextarea as any);

      const { copyToClipboard } = useClipboard();
      const result = await copyToClipboard('test');

      expect(result).toBe(false);
    });
  });

  describe('copying state', () => {
    it('should update copying state during operation', async () => {
      let resolveCopy: () => void;
      const writeTextMock = vi.fn().mockImplementation(() => new Promise<void>(resolve => {
        resolveCopy = resolve;
      }));

      Object.defineProperty(navigator, 'clipboard', {
        value: { writeText: writeTextMock },
        writable: true
      });

      const { copyToClipboard, copying } = useClipboard();

      // Start copy - should be copying
      const copyPromise = copyToClipboard('test');
      expect(copying.value).toBe(true);

      // Resolve copy
      resolveCopy!();
      await copyPromise;

      // Should be done copying
      expect(copying.value).toBe(false);
    });
  });
});
