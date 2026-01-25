/**
 * Script API client.
 * Story 3.2: 脚本编辑页面 - T3
 */
import request from './request';
import type { ScriptResponse, SaveScriptRequest } from '../types/script';
import type { ApiResponse } from '../types/task';

/**
 * Get script for a task.
 * Calls GET /api/v1/tasks/{id}/script
 */
export async function getScript(taskId: number): Promise<ApiResponse<ScriptResponse>> {
  return request.get(`/tasks/${taskId}/script`);
}

/**
 * Save script modifications.
 * Calls PUT /api/v1/tasks/{id}/script
 */
export async function saveScript(
  taskId: number,
  data: SaveScriptRequest
): Promise<ApiResponse<ScriptResponse>> {
  return request.put(`/tasks/${taskId}/script`, data);
}

/**
 * Regenerate script using AI.
 * Calls POST /api/v1/tasks/{id}/regenerate-script
 */
export async function regenerateScript(taskId: number): Promise<ApiResponse<ScriptResponse>> {
  return request.post(`/tasks/${taskId}/regenerate-script`);
}
