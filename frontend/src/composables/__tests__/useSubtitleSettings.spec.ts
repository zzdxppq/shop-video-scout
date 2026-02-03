/**
 * Tests for useSubtitleSettings composable.
 * Story 4.5: 字幕设置页面
 *
 * Test IDs: 4.5-UNIT-027 to 4.5-UNIT-036
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { nextTick } from 'vue';
import { useSubtitleSettings } from '../useSubtitleSettings';
import * as subtitleApi from '../../api/subtitle';

// Mock the API module
vi.mock('../../api/subtitle', () => ({
  updateSubtitleSettings: vi.fn()
}));

describe('useSubtitleSettings', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Reset shared state before each test
    const { _reset } = useSubtitleSettings();
    _reset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('initial state', () => {
    // 4.5-UNIT-027: Initial state subtitleEnabled=true
    it('should have subtitleEnabled=true by default', () => {
      const { subtitleEnabled } = useSubtitleSettings();
      expect(subtitleEnabled.value).toBe(true);
    });

    // 4.5-UNIT-028: Initial state subtitleStyle=simple_white
    it('should have subtitleStyle=simple_white by default', () => {
      const { subtitleStyle } = useSubtitleSettings();
      expect(subtitleStyle.value).toBe('simple_white');
    });

    it('should have loading=false by default', () => {
      const { loading } = useSubtitleSettings();
      expect(loading.value).toBe(false);
    });

    it('should have error=null by default', () => {
      const { error } = useSubtitleSettings();
      expect(error.value).toBeNull();
    });
  });

  describe('initSettings', () => {
    it('should initialize settings from provided values', () => {
      const { initSettings, subtitleEnabled, subtitleStyle } = useSubtitleSettings();

      initSettings(false, 'neon');

      expect(subtitleEnabled.value).toBe(false);
      expect(subtitleStyle.value).toBe('neon');
    });
  });

  describe('toggleSubtitle', () => {
    // 4.5-UNIT-029: toggleSubtitle calls API
    it('should call API with new enabled value', async () => {
      const mockResponse = {
        code: 200,
        message: 'success',
        data: { subtitleEnabled: false, subtitleStyle: 'simple_white' },
        timestamp: Date.now()
      };
      vi.mocked(subtitleApi.updateSubtitleSettings).mockResolvedValue(mockResponse);

      const { toggleSubtitle, subtitleEnabled } = useSubtitleSettings();
      expect(subtitleEnabled.value).toBe(true);

      await toggleSubtitle(1);

      expect(subtitleApi.updateSubtitleSettings).toHaveBeenCalledWith(1, {
        subtitleEnabled: false,
        subtitleStyle: 'simple_white'
      });
    });

    // 4.5-UNIT-030: toggleSubtitle success updates state
    it('should update state on success', async () => {
      const mockResponse = {
        code: 200,
        message: 'success',
        data: { subtitleEnabled: false, subtitleStyle: 'simple_white' },
        timestamp: Date.now()
      };
      vi.mocked(subtitleApi.updateSubtitleSettings).mockResolvedValue(mockResponse);

      const { toggleSubtitle, subtitleEnabled } = useSubtitleSettings();
      expect(subtitleEnabled.value).toBe(true);

      await toggleSubtitle(1);

      expect(subtitleEnabled.value).toBe(false);
    });

    // 4.5-UNIT-031: toggleSubtitle error rolls back state
    it('should roll back state on error', async () => {
      vi.mocked(subtitleApi.updateSubtitleSettings).mockRejectedValue(new Error('API Error'));

      const { toggleSubtitle, subtitleEnabled, error } = useSubtitleSettings();
      expect(subtitleEnabled.value).toBe(true);

      await toggleSubtitle(1);

      expect(subtitleEnabled.value).toBe(true); // Rolled back
      expect(error.value).toBe('设置保存失败，请重试');
    });

    // 4.5-UNIT-035: loading state during API call
    it('should set loading=true during API call', async () => {
      let resolvePromise: (value: unknown) => void;
      const pendingPromise = new Promise((resolve) => {
        resolvePromise = resolve;
      });
      vi.mocked(subtitleApi.updateSubtitleSettings).mockReturnValue(pendingPromise as never);

      const { toggleSubtitle, loading } = useSubtitleSettings();
      expect(loading.value).toBe(false);

      const togglePromise = toggleSubtitle(1);
      await nextTick();

      expect(loading.value).toBe(true);

      resolvePromise!({
        code: 200,
        message: 'success',
        data: { subtitleEnabled: false, subtitleStyle: 'simple_white' },
        timestamp: Date.now()
      });

      await togglePromise;
      expect(loading.value).toBe(false);
    });
  });

  describe('setStyle', () => {
    // 4.5-UNIT-032: setStyle calls API
    it('should call API with new style', async () => {
      const mockResponse = {
        code: 200,
        message: 'success',
        data: { subtitleEnabled: true, subtitleStyle: 'neon' },
        timestamp: Date.now()
      };
      vi.mocked(subtitleApi.updateSubtitleSettings).mockResolvedValue(mockResponse);

      const { setStyle } = useSubtitleSettings();

      await setStyle(1, 'neon');

      expect(subtitleApi.updateSubtitleSettings).toHaveBeenCalledWith(1, {
        subtitleEnabled: true,
        subtitleStyle: 'neon'
      });
    });

    // 4.5-UNIT-033: setStyle success updates state
    it('should update state on success', async () => {
      const mockResponse = {
        code: 200,
        message: 'success',
        data: { subtitleEnabled: true, subtitleStyle: 'neon' },
        timestamp: Date.now()
      };
      vi.mocked(subtitleApi.updateSubtitleSettings).mockResolvedValue(mockResponse);

      const { setStyle, subtitleStyle } = useSubtitleSettings();
      expect(subtitleStyle.value).toBe('simple_white');

      await setStyle(1, 'neon');

      expect(subtitleStyle.value).toBe('neon');
    });

    // 4.5-UNIT-034: setStyle error rolls back state
    it('should roll back state on error', async () => {
      vi.mocked(subtitleApi.updateSubtitleSettings).mockRejectedValue(new Error('API Error'));

      const { setStyle, subtitleStyle, error } = useSubtitleSettings();
      expect(subtitleStyle.value).toBe('simple_white');

      await setStyle(1, 'neon');

      expect(subtitleStyle.value).toBe('simple_white'); // Rolled back
      expect(error.value).toBe('样式保存失败，请重试');
    });

    it('should not call API if same style selected', async () => {
      const { setStyle, subtitleStyle } = useSubtitleSettings();
      expect(subtitleStyle.value).toBe('simple_white');

      await setStyle(1, 'simple_white');

      expect(subtitleApi.updateSubtitleSettings).not.toHaveBeenCalled();
    });
  });

  describe('clearError', () => {
    it('should clear error state', async () => {
      vi.mocked(subtitleApi.updateSubtitleSettings).mockRejectedValue(new Error('API Error'));

      const { toggleSubtitle, error, clearError } = useSubtitleSettings();
      await toggleSubtitle(1);
      expect(error.value).not.toBeNull();

      clearError();

      expect(error.value).toBeNull();
    });
  });
});
