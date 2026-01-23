/**
 * Video upload composable.
 * Handles file validation, upload progress tracking, and upload state management.
 */
import { ref, computed } from 'vue';
import { VIDEO_UPLOAD_CONSTRAINTS } from '../types/task';
import type { TaskVideo } from '../types/task';
import { getVideoUploadUrl, confirmVideoUpload } from '../api/task';

export interface UploadingFile {
  id: string;
  file: File;
  progress: number;
  status: 'pending' | 'uploading' | 'uploaded' | 'failed';
  error?: string;
  abortController?: AbortController;
  ossKey?: string;
  video?: TaskVideo;
}

export interface FileValidationResult {
  valid: boolean;
  error?: string;
}

/**
 * Validate a single file for upload.
 */
export function validateFile(file: File): FileValidationResult {
  // Check file type
  const acceptedTypes: readonly string[] = VIDEO_UPLOAD_CONSTRAINTS.ACCEPTED_TYPES;
  const isValidType = acceptedTypes.includes(file.type) ||
    VIDEO_UPLOAD_CONSTRAINTS.ACCEPTED_EXTENSIONS.some(ext =>
      file.name.toLowerCase().endsWith(ext)
    );

  if (!isValidType) {
    return {
      valid: false,
      error: `不支持的文件格式: ${file.name}，请上传 MP4/MOV 格式视频`
    };
  }

  // Check file size
  if (file.size > VIDEO_UPLOAD_CONSTRAINTS.MAX_FILE_SIZE) {
    const sizeMB = (file.size / (1024 * 1024)).toFixed(1);
    return {
      valid: false,
      error: `文件过大: ${file.name} (${sizeMB}MB)，最大支持 100MB`
    };
  }

  return { valid: true };
}

/**
 * Validate file count.
 */
export function validateFileCount(
  currentCount: number,
  newFilesCount: number
): FileValidationResult {
  const totalCount = currentCount + newFilesCount;

  if (totalCount > VIDEO_UPLOAD_CONSTRAINTS.MAX_VIDEO_COUNT) {
    return {
      valid: false,
      error: `视频数量超出限制，最多上传 ${VIDEO_UPLOAD_CONSTRAINTS.MAX_VIDEO_COUNT} 个视频`
    };
  }

  return { valid: true };
}

/**
 * Check if minimum video count is reached.
 */
export function hasMinimumVideos(count: number): boolean {
  return count >= VIDEO_UPLOAD_CONSTRAINTS.MIN_VIDEO_COUNT;
}

/**
 * Format duration from seconds to mm:ss.
 */
export function formatDuration(seconds: number | undefined): string {
  if (seconds === undefined || seconds === null) {
    return '--:--';
  }
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}

/**
 * Generate unique ID for upload tracking.
 */
function generateUploadId(): string {
  return `upload-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
}

/**
 * Video upload composable.
 */
export function useVideoUpload(taskId: number) {
  const uploadingFiles = ref<UploadingFile[]>([]);
  const uploadedVideos = ref<TaskVideo[]>([]);
  const isUploading = ref(false);
  const uploadError = ref<string | null>(null);

  const totalCount = computed(() =>
    uploadedVideos.value.length + uploadingFiles.value.filter(f => f.status !== 'failed').length
  );

  const canAddMore = computed(() =>
    totalCount.value < VIDEO_UPLOAD_CONSTRAINTS.MAX_VIDEO_COUNT
  );

  const hasMinimum = computed(() =>
    uploadedVideos.value.length >= VIDEO_UPLOAD_CONSTRAINTS.MIN_VIDEO_COUNT
  );

  const remainingSlots = computed(() =>
    VIDEO_UPLOAD_CONSTRAINTS.MAX_VIDEO_COUNT - totalCount.value
  );

  /**
   * Add files to upload queue.
   */
  function addFiles(files: FileList | File[]): string[] {
    const errors: string[] = [];
    const fileArray = Array.from(files);

    // Validate count
    const countValidation = validateFileCount(totalCount.value, fileArray.length);
    if (!countValidation.valid) {
      errors.push(countValidation.error!);
      return errors;
    }

    // Validate and add each file
    for (const file of fileArray) {
      const validation = validateFile(file);
      if (!validation.valid) {
        errors.push(validation.error!);
        continue;
      }

      // Check if already added (by name and size)
      const isDuplicate = uploadingFiles.value.some(
        f => f.file.name === file.name && f.file.size === file.size
      );
      if (isDuplicate) {
        continue;
      }

      uploadingFiles.value.push({
        id: generateUploadId(),
        file,
        progress: 0,
        status: 'pending'
      });
    }

    return errors;
  }

  /**
   * Upload a single file to OSS.
   */
  async function uploadFile(uploadingFile: UploadingFile): Promise<void> {
    const { file } = uploadingFile;
    uploadingFile.status = 'uploading';
    uploadingFile.abortController = new AbortController();

    try {
      // Get presigned URL
      const urlResponse = await getVideoUploadUrl(taskId, {
        filename: file.name,
        file_size: file.size,
        content_type: file.type || 'video/mp4'
      });

      if (urlResponse.code !== 200 && urlResponse.code !== 0) {
        throw new Error(urlResponse.message || '获取上传地址失败');
      }

      const { upload_url, oss_key } = urlResponse.data;
      uploadingFile.ossKey = oss_key;

      // Upload to OSS with progress tracking
      await new Promise<void>((resolve, reject) => {
        const xhr = new XMLHttpRequest();

        xhr.upload.addEventListener('progress', (event) => {
          if (event.lengthComputable) {
            uploadingFile.progress = Math.round((event.loaded / event.total) * 100);
          }
        });

        xhr.addEventListener('load', () => {
          if (xhr.status >= 200 && xhr.status < 300) {
            resolve();
          } else {
            reject(new Error(`上传失败: ${xhr.status}`));
          }
        });

        xhr.addEventListener('error', () => reject(new Error('网络错误')));
        xhr.addEventListener('abort', () => reject(new Error('上传已取消')));

        // Handle abort
        uploadingFile.abortController!.signal.addEventListener('abort', () => {
          xhr.abort();
        });

        xhr.open('PUT', upload_url);
        xhr.setRequestHeader('Content-Type', file.type || 'video/mp4');
        xhr.send(file);
      });

      // Confirm upload
      const confirmResponse = await confirmVideoUpload(taskId, {
        oss_key,
        original_filename: file.name,
        file_size: file.size
      });

      if (confirmResponse.code !== 200 && confirmResponse.code !== 0) {
        throw new Error(confirmResponse.message || '确认上传失败');
      }

      uploadingFile.status = 'uploaded';
      uploadingFile.video = confirmResponse.data;
      uploadedVideos.value.push(confirmResponse.data);

    } catch (error) {
      uploadingFile.status = 'failed';
      uploadingFile.error = error instanceof Error ? error.message : '上传失败';
      throw error;
    }
  }

  /**
   * Start uploading all pending files.
   */
  async function startUpload(): Promise<void> {
    const pendingFiles = uploadingFiles.value.filter(f => f.status === 'pending');
    if (pendingFiles.length === 0) return;

    isUploading.value = true;
    uploadError.value = null;

    try {
      // Upload files one by one to avoid overwhelming the server
      for (const file of pendingFiles) {
        if (file.status === 'pending') {
          await uploadFile(file);
        }
      }
    } catch (error) {
      uploadError.value = error instanceof Error ? error.message : '上传过程中出现错误';
    } finally {
      isUploading.value = false;
      // Clean up completed uploads
      uploadingFiles.value = uploadingFiles.value.filter(f => f.status !== 'uploaded');
    }
  }

  /**
   * Cancel a specific upload.
   */
  function cancelUpload(uploadId: string): void {
    const file = uploadingFiles.value.find(f => f.id === uploadId);
    if (file) {
      file.abortController?.abort();
      file.status = 'failed';
      file.error = '上传已取消';
    }
  }

  /**
   * Remove a file from upload queue.
   */
  function removeFromQueue(uploadId: string): void {
    const index = uploadingFiles.value.findIndex(f => f.id === uploadId);
    if (index !== -1) {
      const file = uploadingFiles.value[index];
      if (file && file.status === 'uploading') {
        file.abortController?.abort();
      }
      uploadingFiles.value.splice(index, 1);
    }
  }

  /**
   * Remove an uploaded video.
   */
  function removeVideo(videoId: number): void {
    const index = uploadedVideos.value.findIndex(v => v.id === videoId);
    if (index !== -1) {
      uploadedVideos.value.splice(index, 1);
    }
  }

  /**
   * Set initial videos (for resuming).
   */
  function setVideos(videos: TaskVideo[]): void {
    uploadedVideos.value = [...videos];
  }

  /**
   * Retry failed upload.
   */
  function retryUpload(uploadId: string): void {
    const file = uploadingFiles.value.find(f => f.id === uploadId);
    if (file && file.status === 'failed') {
      file.status = 'pending';
      file.progress = 0;
      file.error = undefined;
    }
  }

  return {
    // State
    uploadingFiles,
    uploadedVideos,
    isUploading,
    uploadError,
    // Computed
    totalCount,
    canAddMore,
    hasMinimum,
    remainingSlots,
    // Methods
    addFiles,
    startUpload,
    cancelUpload,
    removeFromQueue,
    removeVideo,
    setVideos,
    retryUpload
  };
}
