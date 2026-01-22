/**
 * Task API service.
 */
import request from './request';
import type { CreateTaskRequest, TaskResponse, ApiResponse } from '../types/task';

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
 * Get task list for current user.
 * GET /api/v1/tasks
 *
 * @returns list of tasks
 */
export function getTasks(): Promise<ApiResponse<TaskResponse[]>> {
  return request.get('/tasks');
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
