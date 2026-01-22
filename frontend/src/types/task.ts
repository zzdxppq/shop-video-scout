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
