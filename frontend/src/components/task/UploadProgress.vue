<script setup lang="ts">
/**
 * Upload progress component.
 * Displays file upload progress with cancel and retry options.
 */
import type { UploadingFile } from '../../composables/useVideoUpload';

defineProps<{
  files: UploadingFile[];
}>();

const emit = defineEmits<{
  (e: 'cancel', uploadId: string): void;
  (e: 'remove', uploadId: string): void;
  (e: 'retry', uploadId: string): void;
}>();

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function getStatusText(status: UploadingFile['status']): string {
  switch (status) {
    case 'pending': return '等待上传';
    case 'uploading': return '上传中';
    case 'uploaded': return '上传完成';
    case 'failed': return '上传失败';
    default: return '';
  }
}
</script>

<template>
  <div v-if="files.length > 0" class="upload-progress-list">
    <div
      v-for="file in files"
      :key="file.id"
      class="upload-item"
      :class="`status-${file.status}`"
    >
      <div class="upload-item-info">
        <div class="file-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="23 7 16 12 23 17 23 7" />
            <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
          </svg>
        </div>
        <div class="file-details">
          <span class="file-name" :title="file.file.name">{{ file.file.name }}</span>
          <span class="file-meta">
            {{ formatFileSize(file.file.size) }}
            <span class="status-badge">{{ getStatusText(file.status) }}</span>
          </span>
        </div>
      </div>

      <div v-if="file.status === 'uploading'" class="progress-section">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: `${file.progress}%` }" />
        </div>
        <span class="progress-text">{{ file.progress }}%</span>
        <button
          class="action-btn cancel-btn"
          title="取消上传"
          @click="emit('cancel', file.id)"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <div v-else-if="file.status === 'failed'" class="action-section">
        <span class="error-text" :title="file.error">{{ file.error }}</span>
        <button
          class="action-btn retry-btn"
          title="重试"
          @click="emit('retry', file.id)"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 4 23 10 17 10" />
            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
          </svg>
        </button>
        <button
          class="action-btn remove-btn"
          title="移除"
          @click="emit('remove', file.id)"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <div v-else-if="file.status === 'pending'" class="action-section">
        <button
          class="action-btn remove-btn"
          title="移除"
          @click="emit('remove', file.id)"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <div v-else-if="file.status === 'uploaded'" class="success-icon">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="20 6 9 17 4 12" />
        </svg>
      </div>
    </div>
  </div>
</template>

<style scoped>
.upload-progress-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
}

.upload-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.upload-item.status-uploading {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.upload-item.status-failed {
  background: #fef2f2;
  border-color: #fecaca;
}

.upload-item.status-uploaded {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.upload-item-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.file-icon {
  color: #6b7280;
  flex-shrink: 0;
}

.status-uploading .file-icon {
  color: #3b82f6;
}

.status-failed .file-icon {
  color: #ef4444;
}

.status-uploaded .file-icon {
  color: #22c55e;
}

.file-details {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-meta {
  font-size: 12px;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-badge {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  background: #e5e7eb;
  color: #374151;
}

.status-uploading .status-badge {
  background: #dbeafe;
  color: #1d4ed8;
}

.status-failed .status-badge {
  background: #fee2e2;
  color: #dc2626;
}

.status-uploaded .status-badge {
  background: #dcfce7;
  color: #16a34a;
}

.progress-section {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.progress-bar {
  width: 120px;
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #3b82f6;
  border-radius: 3px;
  transition: width 0.2s ease;
}

.progress-text {
  font-size: 12px;
  color: #3b82f6;
  font-weight: 500;
  min-width: 36px;
}

.action-section {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.error-text {
  font-size: 12px;
  color: #dc2626;
  max-width: 150px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn {
  background: #fee2e2;
  color: #dc2626;
}

.cancel-btn:hover {
  background: #fecaca;
}

.retry-btn {
  background: #dbeafe;
  color: #3b82f6;
}

.retry-btn:hover {
  background: #bfdbfe;
}

.remove-btn {
  background: #f3f4f6;
  color: #6b7280;
}

.remove-btn:hover {
  background: #e5e7eb;
  color: #374151;
}

.success-icon {
  color: #22c55e;
  flex-shrink: 0;
}
</style>
