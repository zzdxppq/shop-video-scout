/**
 * Task-related type definitions.
 */

export type ShopType = 'food' | 'beauty' | 'entertainment' | 'other';
export type VideoStyle = 'recommend' | 'review' | 'vlog';
export type TaskStatus = 'created' | 'uploading' | 'analyzing' | 'script_ready' |
                          'script_edited' | 'voice_set' | 'composing' | 'completed' | 'failed';

/**
 * Create task request payload.
 */
export interface CreateTaskRequest {
  shop_name: string;
  shop_type: ShopType;
  promotion_text?: string;
  video_style: VideoStyle;
}

/**
 * Task response from API.
 */
export interface TaskResponse {
  id: number;
  shop_name: string;
  shop_type: ShopType;
  promotion_text?: string;
  video_style: VideoStyle;
  status: TaskStatus;
  created_at: string;
}

/**
 * API response wrapper.
 */
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

/**
 * Shop type display labels.
 */
export const SHOP_TYPE_LABELS: Record<ShopType, string> = {
  food: '餐饮美食',
  beauty: '美容美发',
  entertainment: '休闲娱乐',
  other: '其他'
};

/**
 * Video style display labels.
 */
export const VIDEO_STYLE_LABELS: Record<VideoStyle, string> = {
  recommend: '种草安利型',
  review: '真实测评型',
  vlog: '探店vlog型'
};

/**
 * Video upload status.
 */
export type VideoUploadStatus = 'pending' | 'uploading' | 'uploaded' | 'failed';

/**
 * Video item in task.
 */
export interface TaskVideo {
  id: number;
  task_id: number;
  original_filename: string;
  oss_key: string;
  thumbnail_url?: string;
  duration_seconds?: number;
  file_size?: number;
  status: VideoUploadStatus;
  created_at: string;
}

/**
 * Get presigned upload URL request.
 */
export interface GetUploadUrlRequest {
  filename: string;
  file_size: number;
  content_type: string;
}

/**
 * Get presigned upload URL response.
 */
export interface GetUploadUrlResponse {
  upload_url: string;
  oss_key: string;
  expires_in: number;
}

/**
 * Confirm video upload request.
 */
export interface ConfirmUploadRequest {
  oss_key: string;
  original_filename: string;
  file_size: number;
  duration_seconds?: number;
}

/**
 * Video upload constraints.
 */
export const VIDEO_UPLOAD_CONSTRAINTS = {
  MAX_FILE_SIZE: 100 * 1024 * 1024, // 100MB
  MAX_DURATION_SECONDS: 180, // 3 minutes
  MIN_VIDEO_COUNT: 10,
  MAX_VIDEO_COUNT: 20,
  ACCEPTED_TYPES: ['video/mp4', 'video/quicktime', 'video/x-m4v'],
  ACCEPTED_EXTENSIONS: ['.mp4', '.mov', '.m4v']
} as const;

/**
 * Task summary for history list (Story 5.5).
 */
export interface TaskSummary {
  id: number;
  shop_name: string;
  shop_type: ShopType;
  status: TaskStatus;
  thumbnail_url?: string;
  created_at: string;
}

/**
 * Paginated response wrapper (Story 5.5).
 */
export interface PagedResponse<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
  has_more: boolean;
}

/**
 * Task list query params (Story 5.5).
 */
export interface TaskListParams {
  page?: number;
  size?: number;
}
