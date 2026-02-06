/**
 * Publish assist API module.
 * Story 5.4: 发布辅助UI组件
 *
 * Consumes endpoints from Story 5.3:
 * - GET /api/v1/publish/tasks/{id}/assist
 * - POST /api/v1/publish/tasks/{id}/assist/regenerate
 */
import request from './request';
import type { ApiResponse } from '../types/task';
import type { PublishAssistResponse } from '../types/publish';

/**
 * Get publish assist content (topics and titles).
 * GET /api/v1/publish/tasks/{id}/assist
 *
 * @param taskId - Task ID
 * @returns Publish assist with topics, titles, and remaining regenerate count
 */
export async function getPublishAssist(
  taskId: number
): Promise<ApiResponse<PublishAssistResponse>> {
  return request.get(`/publish/tasks/${taskId}/assist`);
}

/**
 * Regenerate publish assist content.
 * POST /api/v1/publish/tasks/{id}/assist/regenerate
 *
 * @param taskId - Task ID
 * @returns New publish assist with updated topics, titles, and remaining count
 */
export async function regeneratePublishAssist(
  taskId: number
): Promise<ApiResponse<PublishAssistResponse>> {
  return request.post(`/publish/tasks/${taskId}/assist/regenerate`);
}
