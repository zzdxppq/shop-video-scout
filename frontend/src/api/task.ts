/**
 * Task API service.
 */
import request from './request';
import type {
  CreateTaskRequest,
  TaskResponse,
  ApiResponse,
  GetUploadUrlRequest,
  GetUploadUrlResponse,
  ConfirmUploadRequest,
  TaskVideo,
  TaskSummary,
  PagedResponse,
  TaskListParams
} from '../types/task';

/**
 * Create a new task.
 * POST /api/v1/tasks
 *
 * @param data task creation data
 * @returns created task
 */
export function createTask(data: CreateTaskRequest): Promise<ApiResponse<TaskResponse>> {
  return request.post('/tasks', data);
}

/**
 * Get task list for current user (legacy - returns in-progress tasks only).
 * GET /api/v1/tasks
 *
 * @returns list of tasks
 */
export function getTasks(): Promise<ApiResponse<TaskResponse[]>> {
  return request.get('/tasks');
}

/**
 * Get paginated task history for current user.
 * GET /api/v1/tasks/history
 * Story 5.5: 历史任务管理
 *
 * @param params pagination params (page, size)
 * @returns paginated task summary list
 */
export function getTaskHistory(params: TaskListParams = {}): Promise<ApiResponse<PagedResponse<TaskSummary>>> {
  const queryParams = new URLSearchParams();
  if (params.page) queryParams.set('page', String(params.page));
  if (params.size) queryParams.set('size', String(params.size));
  const queryString = queryParams.toString();
  return request.get(`/tasks/history${queryString ? `?${queryString}` : ''}`);
}

/**
 * Delete a task.
 * DELETE /api/v1/tasks/{id}
 * Story 5.5: 历史任务管理
 *
 * @param taskId task ID
 */
export function deleteTask(taskId: number): Promise<ApiResponse<void>> {
  return request.delete(`/tasks/${taskId}`);
}

/**
 * Get task by ID.
 * GET /api/v1/tasks/{id}
 *
 * @param id task ID
 * @returns task details
 */
export function getTask(id: number): Promise<ApiResponse<TaskResponse>> {
  return request.get(`/tasks/${id}`);
}

/**
 * Get presigned URL for video upload.
 * POST /api/v1/tasks/{id}/videos/upload-url
 *
 * @param taskId task ID
 * @param data upload request data
 * @returns presigned upload URL and OSS key
 */
export function getVideoUploadUrl(
  taskId: number,
  data: GetUploadUrlRequest
): Promise<ApiResponse<GetUploadUrlResponse>> {
  return request.post(`/tasks/${taskId}/videos/upload-url`, data);
}

/**
 * Confirm video upload completion.
 * POST /api/v1/tasks/{id}/videos
 *
 * @param taskId task ID
 * @param data confirmation data
 * @returns created video record
 */
export function confirmVideoUpload(
  taskId: number,
  data: ConfirmUploadRequest
): Promise<ApiResponse<TaskVideo>> {
  return request.post(`/tasks/${taskId}/videos`, data);
}

/**
 * Get videos for a task.
 * GET /api/v1/tasks/{id}/videos
 *
 * @param taskId task ID
 * @returns list of videos
 */
export function getTaskVideos(taskId: number): Promise<ApiResponse<TaskVideo[]>> {
  return request.get(`/tasks/${taskId}/videos`);
}

/**
 * Delete a video from task.
 * DELETE /api/v1/tasks/{id}/videos/{videoId}
 *
 * @param taskId task ID
 * @param videoId video ID
 */
export function deleteTaskVideo(taskId: number, videoId: number): Promise<ApiResponse<void>> {
  return request.delete(`/tasks/${taskId}/videos/${videoId}`);
}
