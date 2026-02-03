/**
 * Subtitle type definitions.
 * Story 4.5: 字幕设置页面
 */

/**
 * Subtitle style IDs matching backend enum.
 * BR-2.1: 5 preset styles
 */
export type SubtitleStyleId =
  | 'simple_white'
  | 'vibrant_yellow'
  | 'xiaohongshu'
  | 'douyin_hot'
  | 'neon';

/**
 * Subtitle style definition with display info.
 */
export interface SubtitleStyle {
  id: SubtitleStyleId;
  name: string;
  previewUrl: string;
}

/**
 * Subtitle settings for a task.
 */
export interface SubtitleSettings {
  subtitleEnabled: boolean;
  subtitleStyle: SubtitleStyleId;
}

/**
 * API request for updating subtitle settings.
 */
export interface SubtitleSettingsRequest {
  subtitleEnabled: boolean;
  subtitleStyle: SubtitleStyleId;
}

/**
 * API response for subtitle settings.
 */
export interface SubtitleSettingsResponse {
  subtitleEnabled: boolean;
  subtitleStyle: SubtitleStyleId;
}

/**
 * Available subtitle styles.
 * BR-2.1: simple_white, vibrant_yellow, xiaohongshu, douyin_hot, neon
 * BR-2.2: Default is simple_white
 */
export const SUBTITLE_STYLES: SubtitleStyle[] = [
  { id: 'simple_white', name: '简约白字', previewUrl: '/images/subtitle-styles/simple_white.png' },
  { id: 'vibrant_yellow', name: '活力黄字', previewUrl: '/images/subtitle-styles/vibrant_yellow.png' },
  { id: 'xiaohongshu', name: '小红书风', previewUrl: '/images/subtitle-styles/xiaohongshu.png' },
  { id: 'douyin_hot', name: '抖音热门', previewUrl: '/images/subtitle-styles/douyin_hot.png' },
  { id: 'neon', name: '霓虹炫彩', previewUrl: '/images/subtitle-styles/neon.png' },
];

/**
 * Default subtitle style.
 * BR-2.2: Default is simple_white
 */
export const DEFAULT_SUBTITLE_STYLE: SubtitleStyleId = 'simple_white';

/**
 * Default subtitle enabled state.
 * BR-1.1: Default is true
 */
export const DEFAULT_SUBTITLE_ENABLED = true;
