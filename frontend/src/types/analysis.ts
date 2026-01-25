/**
 * Analysis-related type definitions.
 * Story 2.4: 分析进度展示页面
 */

/**
 * Stage status in analysis progress.
 */
export type StageStatus = 'pending' | 'in_progress' | 'completed';

/**
 * Analysis status.
 */
export type AnalysisStatus = 'analyzing' | 'completed' | 'failed';

/**
 * Frame category types.
 */
export type FrameCategory = 'food' | 'person' | 'environment' | 'other';

/**
 * Analysis stage item.
 */
export interface AnalysisStage {
  id: string;
  label: string;
  status: StageStatus;
}

/**
 * Analysis progress response from API.
 * Endpoint: GET /api/v1/tasks/{id}/progress
 */
export interface AnalysisProgress {
  percent: number;
  currentVideo: number;
  totalVideos: number;
  stage: string;
  stages: AnalysisStage[];
  estimatedTimeRemaining: number;
  status: AnalysisStatus;
  error?: string;
}

/**
 * Video frame from analysis results.
 */
export interface VideoFrame {
  id: number;
  videoId: number;
  thumbnailUrl: string;
  category: FrameCategory;
  tags: string[];
  qualityScore: number;
  isRecommended: boolean;
}

/**
 * Tab category for frame filtering.
 */
export type TabCategory = 'all' | FrameCategory;

/**
 * Tab definition for frame gallery.
 */
export interface FrameTab {
  key: TabCategory;
  label: string;
  count: number;
}

/**
 * Default analysis stages (BR-1.2).
 */
export const DEFAULT_ANALYSIS_STAGES: AnalysisStage[] = [
  { id: 'upload', label: '上传完成', status: 'completed' },
  { id: 'content_recognition', label: '镜头内容识别', status: 'pending' },
  { id: 'quality_assessment', label: '画面质量评估', status: 'pending' },
  { id: 'shot_marking', label: '推荐镜头标记', status: 'pending' },
  { id: 'script_generation', label: '脚本生成', status: 'pending' }
];

/**
 * Category labels for display.
 */
export const CATEGORY_LABELS: Record<TabCategory, string> = {
  all: '全部',
  food: '食物',
  person: '人物',
  environment: '环境',
  other: '其他'
};

/**
 * Polling interval in milliseconds (BR-1.1).
 */
export const POLLING_INTERVAL_MS = 2000;

/**
 * Auto-redirect delay after completion (BR-1.4).
 */
export const REDIRECT_DELAY_MS = 2000;

/**
 * Long wait warning threshold (3 minutes).
 */
export const LONG_WAIT_WARNING_MS = 3 * 60 * 1000;

/**
 * Long wait retry threshold (5 minutes).
 */
export const LONG_WAIT_RETRY_MS = 5 * 60 * 1000;

/**
 * Max polling retry attempts on error.
 */
export const MAX_POLL_RETRIES = 3;
