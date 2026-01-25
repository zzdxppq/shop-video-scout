/**
 * Analysis API service.
 * Story 2.4: 分析进度展示页面
 */
import request from './request';
import type { ApiResponse } from '../types/task';
import type { AnalysisProgress, VideoFrame } from '../types/analysis';

/**
 * Get analysis progress for a task.
 * GET /api/v1/tasks/{id}/progress
 *
 * @param taskId task ID
 * @returns analysis progress
 */
export function getAnalysisProgress(taskId: number): Promise<ApiResponse<AnalysisProgress>> {
  return request.get(`/tasks/${taskId}/progress`);
}

/**
 * Start or retry analysis for a task.
 * POST /api/v1/tasks/{id}/analyze
 *
 * @param taskId task ID
 * @returns void response
 */
export function startAnalysis(taskId: number): Promise<ApiResponse<void>> {
  return request.post(`/tasks/${taskId}/analyze`);
}

/**
 * Get analyzed frames for a task.
 * GET /api/v1/tasks/{id}/frames
 *
 * @param taskId task ID
 * @returns list of analyzed frames
 */
export function getTaskFrames(taskId: number): Promise<ApiResponse<VideoFrame[]>> {
  return request.get(`/tasks/${taskId}/frames`);
}
