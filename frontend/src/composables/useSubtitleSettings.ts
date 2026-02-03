/**
 * Subtitle settings composable.
 * Story 4.5: 字幕设置页面
 *
 * Provides state management for subtitle toggle and style selection
 * with optimistic updates and rollback on error.
 */
import { ref } from 'vue';
import { updateSubtitleSettings } from '../api/subtitle';
import type { SubtitleStyleId } from '../types/subtitle';
import { DEFAULT_SUBTITLE_ENABLED, DEFAULT_SUBTITLE_STYLE } from '../types/subtitle';

export interface UseSubtitleSettingsReturn {
  subtitleEnabled: typeof subtitleEnabled;
  subtitleStyle: typeof subtitleStyle;
  loading: typeof loading;
  error: typeof error;
  initSettings: (enabled: boolean, style: SubtitleStyleId) => void;
  toggleSubtitle: (taskId: number) => Promise<void>;
  setStyle: (taskId: number, style: SubtitleStyleId) => Promise<void>;
  clearError: () => void;
  /** Reset state to defaults (for testing) */
  _reset: () => void;
}

// Reactive state - shared across components using this composable
const subtitleEnabled = ref<boolean>(DEFAULT_SUBTITLE_ENABLED);
const subtitleStyle = ref<SubtitleStyleId>(DEFAULT_SUBTITLE_STYLE);
const loading = ref<boolean>(false);
const error = ref<string | null>(null);

/**
 * Initialize settings from task data.
 * Call this when loading task details.
 */
function initSettings(enabled: boolean, style: SubtitleStyleId): void {
  subtitleEnabled.value = enabled;
  subtitleStyle.value = style;
  error.value = null;
}

/**
 * Toggle subtitle enabled state.
 * Uses optimistic update with rollback on error.
 *
 * BR-1.2: 开关状态实时保存到任务配置中
 */
async function toggleSubtitle(taskId: number): Promise<void> {
  if (loading.value) return;

  const previousEnabled = subtitleEnabled.value;
  const newEnabled = !previousEnabled;

  // Optimistic update
  subtitleEnabled.value = newEnabled;
  loading.value = true;
  error.value = null;

  try {
    const response = await updateSubtitleSettings(taskId, {
      subtitleEnabled: newEnabled,
      subtitleStyle: subtitleStyle.value
    });

    if (response.code !== 0 && response.code !== 200) {
      throw new Error(response.message || '设置保存失败');
    }

    // Update from server response
    subtitleEnabled.value = response.data.subtitleEnabled;
  } catch (err) {
    // Rollback on error
    subtitleEnabled.value = previousEnabled;
    error.value = '设置保存失败，请重试';
  } finally {
    loading.value = false;
  }
}

/**
 * Set subtitle style.
 * Uses optimistic update with rollback on error.
 *
 * BR-2.3: 样式选择实时保存到 tasks.subtitle_style 字段
 */
async function setStyle(taskId: number, style: SubtitleStyleId): Promise<void> {
  if (loading.value) return;

  // Skip if same style
  if (subtitleStyle.value === style) return;

  const previousStyle = subtitleStyle.value;

  // Optimistic update
  subtitleStyle.value = style;
  loading.value = true;
  error.value = null;

  try {
    const response = await updateSubtitleSettings(taskId, {
      subtitleEnabled: subtitleEnabled.value,
      subtitleStyle: style
    });

    if (response.code !== 0 && response.code !== 200) {
      throw new Error(response.message || '样式保存失败');
    }

    // Update from server response
    subtitleStyle.value = response.data.subtitleStyle;
  } catch (err) {
    // Rollback on error
    subtitleStyle.value = previousStyle;
    error.value = '样式保存失败，请重试';
  } finally {
    loading.value = false;
  }
}

/**
 * Clear error state.
 */
function clearError(): void {
  error.value = null;
}

/**
 * Reset state to defaults (for testing).
 * @internal
 */
function _reset(): void {
  subtitleEnabled.value = DEFAULT_SUBTITLE_ENABLED;
  subtitleStyle.value = DEFAULT_SUBTITLE_STYLE;
  loading.value = false;
  error.value = null;
}

/**
 * Subtitle settings composable.
 * Provides reactive state and actions for subtitle configuration.
 */
export function useSubtitleSettings(): UseSubtitleSettingsReturn {
  return {
    subtitleEnabled,
    subtitleStyle,
    loading,
    error,
    initSettings,
    toggleSubtitle,
    setStyle,
    clearError,
    _reset
  };
}
