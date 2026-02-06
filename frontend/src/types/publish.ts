/**
 * Type definitions for publish assist.
 * Story 5.4: 发布辅助UI组件
 */

/**
 * Response from GET /api/v1/publish/tasks/{id}/assist
 * and POST /api/v1/publish/tasks/{id}/assist/regenerate
 */
export interface PublishAssistResponse {
  topics: string[];
  titles: string[];
  regenerate_remaining: number;
}

/**
 * Publish assist state for composable.
 */
export interface PublishAssistState {
  topics: string[];
  titles: string[];
  regenerateRemaining: number;
  loading: boolean;
  regenerating: boolean;
  error: string | null;
}
