/**
 * Script types for Story 3.2: 脚本编辑页面
 */

/**
 * Script paragraph from API response.
 */
export interface ScriptParagraph {
  id: string;             // e.g., "para_1"
  section: string;        // e.g., "开场", "环境展示"
  shotId: number;         // 对应镜头ID
  text: string;           // 文案内容
  estimatedDuration: number; // 预计时长(秒)
}

/**
 * Script response from GET /api/v1/tasks/{id}/script.
 */
export interface ScriptResponse {
  id: number;
  taskId: number;
  paragraphs: ScriptParagraph[];
  version: number;
  regenerateRemaining: number;  // 5 - script_regenerate_count
  createdAt: string;
  updatedAt: string;
}

/**
 * Request payload for PUT /api/v1/tasks/{id}/script.
 */
export interface SaveScriptRequest {
  paragraphs: Array<{
    id: string;
    text: string;
  }>;
}

/**
 * Edit state for a single paragraph.
 */
export interface ParagraphEditState {
  originalText: string;
  currentText: string;
  isEditing: boolean;
  hasError: boolean;
  errorMessage: string | null;
}

/**
 * Undo stack entry.
 */
export interface UndoEntry {
  paragraphId: string;
  previousText: string;
  timestamp: number;
}

/**
 * Local draft stored in localStorage.
 */
export interface ScriptDraft {
  taskId: number;
  paragraphs: Array<{ id: string; text: string }>;
  savedAt: number;
  version: number;
}

/**
 * Character count info for paragraph.
 */
export interface CharCountInfo {
  current: number;
  suggested: number;  // estimatedDuration * 4 (BR-1.2)
  isOverLimit: boolean;
}

/**
 * Validation constants.
 */
export const SCRIPT_VALIDATION = {
  MAX_PARAGRAPH_LENGTH: 500,
  CHARS_PER_SECOND: 4,  // BR-1.2
  MAX_UNDO_STACK: 20,
  DRAFT_EXPIRY_HOURS: 24
} as const;
