/**
 * Subtitle API module.
 * Story 4.5: 字幕设置页面
 */
import request from './request';
import type { ApiResponse } from '../types/task';
import type { SubtitleSettingsRequest, SubtitleSettingsResponse } from '../types/subtitle';

/**
 * Update subtitle settings for a task.
 * PUT /api/v1/tasks/{id}/subtitle-settings
 *
 * @param taskId - Task ID
 * @param settings - Subtitle settings to update
 */
export async function updateSubtitleSettings(
  taskId: number,
  settings: SubtitleSettingsRequest
): Promise<ApiResponse<SubtitleSettingsResponse>> {
  return request.put(`/tasks/${taskId}/subtitle-settings`, settings);
}
