/**
 * useVideoUpload composable tests
 * Covers: 2.2-UNIT-001 to 2.2-UNIT-005, 2.2-BLIND-BOUNDARY-001 to 2.2-BLIND-BOUNDARY-004
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  validateFile,
  validateFileCount,
  formatDuration,
  hasMinimumVideos,
  useVideoUpload,
} from '../useVideoUpload';
import { VIDEO_UPLOAD_CONSTRAINTS } from '../../types/task';

// Mock the API module
vi.mock('../../api/task', () => ({
  getVideoUploadUrl: vi.fn(),
  confirmVideoUpload: vi.fn(),
}));

/**
 * Helper to create a mock File object with specified size
 * Uses Object.defineProperty to mock size without allocating actual memory
 */
function createMockFile(
  name: string,
  size: number,
  type: string = 'video/mp4'
): File {
  const blob = new Blob(['x'], { type });
  const file = new File([blob], name, { type });
  // Override size property without allocating memory
  Object.defineProperty(file, 'size', { value: size, writable: false });
  return file;
}

describe('validateFile', () => {
  // 2.2-UNIT-001: File type validation - accepts video files
  describe('file type validation - accepts video files', () => {
    it('should accept .mp4 files', () => {
      const file = createMockFile('test.mp4', 1024, 'video/mp4');
      const result = validateFile(file);
      expect(result.valid).toBe(true);
      expect(result.error).toBeUndefined();
    });

    it('should accept .mov files', () => {
      const file = createMockFile('test.mov', 1024, 'video/quicktime');
      const result = validateFile(file);
      expect(result.valid).toBe(true);
    });

    it('should accept .m4v files', () => {
      const file = createMockFile('test.m4v', 1024, 'video/x-m4v');
      const result = validateFile(file);
      expect(result.valid).toBe(true);
    });

    it('should accept video files by extension even with generic MIME type', () => {
      const file = createMockFile('test.mp4', 1024, 'application/octet-stream');
      const result = validateFile(file);
      expect(result.valid).toBe(true);
    });
  });

  // 2.2-UNIT-002: File type validation - rejects non-video files
  describe('file type validation - rejects non-video files', () => {
    it('should reject .txt files', () => {
      const file = createMockFile('test.txt', 1024, 'text/plain');
      const result = validateFile(file);
      expect(result.valid).toBe(false);
      expect(result.error).toContain('不支持的文件格式');
    });

    it('should reject .pdf files', () => {
      const file = createMockFile('test.pdf', 1024, 'application/pdf');
      const result = validateFile(file);
      expect(result.valid).toBe(false);
      expect(result.error).toContain('不支持的文件格式');
    });

    it('should reject .jpg image files', () => {
      const file = createMockFile('test.jpg', 1024, 'image/jpeg');
      const result = validateFile(file);
      expect(result.valid).toBe(false);
    });

    it('should reject .avi files (unsupported video format)', () => {
      const file = createMockFile('test.avi', 1024, 'video/x-msvideo');
      const result = validateFile(file);
      expect(result.valid).toBe(false);
    });
  });

  // 2.2-UNIT-004: File size validation
  describe('file size validation', () => {
    it('should accept files within size limit', () => {
      const file = createMockFile('test.mp4', 50 * 1024 * 1024); // 50MB
      const result = validateFile(file);
      expect(result.valid).toBe(true);
    });

    // 2.2-BLIND-BOUNDARY-003: File size at exact limit
    it('should accept file at exactly 100MB', () => {
      const file = createMockFile('test.mp4', VIDEO_UPLOAD_CONSTRAINTS.MAX_FILE_SIZE);
      const result = validateFile(file);
      expect(result.valid).toBe(true);
    });

    // 2.2-BLIND-BOUNDARY-004: File size exceeds limit
    it('should reject file exceeding 100MB', () => {
      const file = createMockFile('test.mp4', VIDEO_UPLOAD_CONSTRAINTS.MAX_FILE_SIZE + 1);
      const result = validateFile(file);
      expect(result.valid).toBe(false);
      expect(result.error).toContain('文件过大');
      expect(result.error).toContain('100MB');
    });

    it('should show file size in error message', () => {
      const fileSize = 150 * 1024 * 1024; // 150MB
      const file = createMockFile('large.mp4', fileSize);
      const result = validateFile(file);
      expect(result.error).toContain('150.0MB');
    });
  });
});

describe('validateFileCount', () => {
  // 2.2-UNIT-003: File count validation
  describe('file count within limits', () => {
    it('should accept adding files when under max limit', () => {
      const result = validateFileCount(5, 3);
      expect(result.valid).toBe(true);
    });

    it('should accept adding files up to exactly max limit', () => {
      const result = validateFileCount(15, 5); // 15 + 5 = 20
      expect(result.valid).toBe(true);
    });

    // 2.2-BLIND-BOUNDARY-002: Max file count exceeded
    it('should reject when total exceeds max limit', () => {
      const result = validateFileCount(15, 6); // 15 + 6 = 21
      expect(result.valid).toBe(false);
      expect(result.error).toContain('视频数量超出限制');
    });

    it('should reject adding files when already at max', () => {
      const result = validateFileCount(20, 1);
      expect(result.valid).toBe(false);
    });
  });
});

describe('formatDuration', () => {
  // 2.2-UNIT-007: Duration display - formats seconds to mm:ss
  it('should format 0 seconds as 0:00', () => {
    expect(formatDuration(0)).toBe('0:00');
  });

  it('should format 65 seconds as 1:05', () => {
    expect(formatDuration(65)).toBe('1:05');
  });

  it('should format 125 seconds as 2:05', () => {
    expect(formatDuration(125)).toBe('2:05');
  });

  it('should format 600 seconds as 10:00', () => {
    expect(formatDuration(600)).toBe('10:00');
  });

  it('should handle undefined as --:--', () => {
    expect(formatDuration(undefined)).toBe('--:--');
  });

  it('should handle decimal seconds by flooring', () => {
    expect(formatDuration(65.9)).toBe('1:05');
  });
});

describe('hasMinimumVideos', () => {
  it('should return false when count is below minimum', () => {
    expect(hasMinimumVideos(5)).toBe(false);
    expect(hasMinimumVideos(9)).toBe(false);
  });

  it('should return true when count equals minimum', () => {
    expect(hasMinimumVideos(10)).toBe(true);
  });

  it('should return true when count exceeds minimum', () => {
    expect(hasMinimumVideos(15)).toBe(true);
  });
});

describe('useVideoUpload', () => {
  const taskId = 123;

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // 2.2-UNIT-005: Multiple file handling
  describe('addFiles', () => {
    it('should add valid files to upload queue', () => {
      const { uploadingFiles, addFiles } = useVideoUpload(taskId);
      const files = [
        createMockFile('video1.mp4', 1024),
        createMockFile('video2.mp4', 2048),
      ];

      const errors = addFiles(files);

      expect(errors).toHaveLength(0);
      expect(uploadingFiles.value).toHaveLength(2);
      expect(uploadingFiles.value[0].status).toBe('pending');
    });

    it('should skip duplicate files by name and size', () => {
      const { uploadingFiles, addFiles } = useVideoUpload(taskId);
      const file = createMockFile('video1.mp4', 1024);

      addFiles([file]);
      addFiles([file]);

      expect(uploadingFiles.value).toHaveLength(1);
    });

    it('should return errors for invalid files', () => {
      const { addFiles } = useVideoUpload(taskId);
      const files = [
        createMockFile('valid.mp4', 1024),
        createMockFile('invalid.txt', 1024, 'text/plain'),
      ];

      const errors = addFiles(files);

      expect(errors.length).toBeGreaterThan(0);
      expect(errors[0]).toContain('不支持的文件格式');
    });

    // 2.2-BLIND-BOUNDARY-001: Empty file drop
    it('should handle empty file list gracefully', () => {
      const { uploadingFiles, addFiles } = useVideoUpload(taskId);
      const errors = addFiles([]);

      expect(errors).toHaveLength(0);
      expect(uploadingFiles.value).toHaveLength(0);
    });
  });

  // 2.2-BLIND-FLOW-001: Cancel upload mid-progress
  describe('cancelUpload', () => {
    it('should mark upload as failed when cancelled', () => {
      const { uploadingFiles, addFiles, cancelUpload } = useVideoUpload(taskId);
      const file = createMockFile('video.mp4', 1024);

      addFiles([file]);
      const uploadId = uploadingFiles.value[0].id;

      // Simulate upload started
      uploadingFiles.value[0].status = 'uploading';
      uploadingFiles.value[0].progress = 50;
      uploadingFiles.value[0].abortController = new AbortController();

      cancelUpload(uploadId);

      expect(uploadingFiles.value[0].status).toBe('failed');
      expect(uploadingFiles.value[0].error).toBe('上传已取消');
    });
  });

  describe('removeFromQueue', () => {
    it('should remove file from queue', () => {
      const { uploadingFiles, addFiles, removeFromQueue } = useVideoUpload(taskId);
      const file = createMockFile('video.mp4', 1024);

      addFiles([file]);
      const uploadId = uploadingFiles.value[0].id;

      removeFromQueue(uploadId);

      expect(uploadingFiles.value).toHaveLength(0);
    });
  });

  describe('retryUpload', () => {
    it('should reset failed upload to pending', () => {
      const { uploadingFiles, addFiles, retryUpload } = useVideoUpload(taskId);
      const file = createMockFile('video.mp4', 1024);

      addFiles([file]);
      const uploadId = uploadingFiles.value[0].id;

      // Simulate failed upload
      uploadingFiles.value[0].status = 'failed';
      uploadingFiles.value[0].error = 'Network error';
      uploadingFiles.value[0].progress = 50;

      retryUpload(uploadId);

      expect(uploadingFiles.value[0].status).toBe('pending');
      expect(uploadingFiles.value[0].progress).toBe(0);
      expect(uploadingFiles.value[0].error).toBeUndefined();
    });
  });

  describe('computed properties', () => {
    it('should calculate totalCount correctly', () => {
      const { totalCount, addFiles, uploadedVideos } = useVideoUpload(taskId);

      addFiles([createMockFile('v1.mp4', 1024)]);
      expect(totalCount.value).toBe(1);

      uploadedVideos.value.push({
        id: 1,
        task_id: taskId,
        original_filename: 'uploaded.mp4',
        oss_key: 'key',
        status: 'uploaded',
        created_at: new Date().toISOString(),
      });
      expect(totalCount.value).toBe(2);
    });

    it('should calculate canAddMore correctly', () => {
      const { canAddMore, uploadedVideos } = useVideoUpload(taskId);

      expect(canAddMore.value).toBe(true);

      // Fill up to max
      for (let i = 0; i < 20; i++) {
        uploadedVideos.value.push({
          id: i,
          task_id: taskId,
          original_filename: `video${i}.mp4`,
          oss_key: `key${i}`,
          status: 'uploaded',
          created_at: new Date().toISOString(),
        });
      }

      expect(canAddMore.value).toBe(false);
    });

    it('should calculate hasMinimum correctly', () => {
      const { hasMinimum, uploadedVideos } = useVideoUpload(taskId);

      expect(hasMinimum.value).toBe(false);

      // Add minimum required videos
      for (let i = 0; i < 10; i++) {
        uploadedVideos.value.push({
          id: i,
          task_id: taskId,
          original_filename: `video${i}.mp4`,
          oss_key: `key${i}`,
          status: 'uploaded',
          created_at: new Date().toISOString(),
        });
      }

      expect(hasMinimum.value).toBe(true);
    });
  });
});
