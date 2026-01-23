<script setup lang="ts">
/**
 * Task Upload page - Step 2: Upload videos.
 * Allows users to drag-drop or select videos for upload.
 */
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import StepIndicator from '../components/task/StepIndicator.vue';
import VideoUploader from '../components/task/VideoUploader.vue';
import UploadProgress from '../components/task/UploadProgress.vue';
import VideoList from '../components/task/VideoList.vue';
import { useVideoUpload } from '../composables/useVideoUpload';
import { getTask, getTaskVideos, deleteTaskVideo } from '../api/task';
import { VIDEO_UPLOAD_CONSTRAINTS } from '../types/task';
import type { TaskResponse } from '../types/task';

const route = useRoute();
const router = useRouter();

const taskId = computed(() => Number(route.params.id));

const steps = [
  { label: '填写信息', description: '店铺基本信息' },
  { label: '上传视频', description: '素材视频' },
  { label: 'AI分析', description: '智能分析' },
  { label: '编辑脚本', description: '口播文案' },
  { label: '预览导出', description: '生成视频' }
];

const currentStep = ref(1);
const task = ref<TaskResponse | null>(null);
const isLoading = ref(true);
const isLoadingVideos = ref(false);
const isDeleting = ref(false);
const errorMessage = ref('');
const validationErrors = ref<string[]>([]);

const {
  uploadingFiles,
  uploadedVideos,
  isUploading,
  uploadError,
  totalCount,
  hasMinimum,
  addFiles,
  startUpload,
  cancelUpload,
  removeFromQueue,
  removeVideo,
  setVideos,
  retryUpload
} = useVideoUpload(taskId.value);

const canProceed = computed(() => hasMinimum.value && !isUploading.value);
const hasPendingUploads = computed(() =>
  uploadingFiles.value.some(f => f.status === 'pending')
);

async function loadTask(): Promise<void> {
  try {
    const response = await getTask(taskId.value);
    if (response.code === 200 || response.code === 0) {
      task.value = response.data;
    } else {
      errorMessage.value = response.message || '加载任务失败';
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载任务失败';
  }
}

async function loadVideos(): Promise<void> {
  isLoadingVideos.value = true;
  try {
    const response = await getTaskVideos(taskId.value);
    if (response.code === 200 || response.code === 0) {
      setVideos(response.data || []);
    }
  } catch (error) {
    console.error('Failed to load videos:', error);
  } finally {
    isLoadingVideos.value = false;
  }
}

function handleFilesSelected(files: File[]): void {
  const errors = addFiles(files);
  if (errors.length > 0) {
    validationErrors.value = errors;
    setTimeout(() => {
      validationErrors.value = [];
    }, 5000);
  }
}

function handleValidationError(errors: string[]): void {
  validationErrors.value = errors;
  setTimeout(() => {
    validationErrors.value = [];
  }, 5000);
}

async function handleStartUpload(): Promise<void> {
  await startUpload();
}

async function handleDeleteVideo(videoId: number): Promise<void> {
  if (isDeleting.value) return;

  isDeleting.value = true;
  try {
    const response = await deleteTaskVideo(taskId.value, videoId);
    if (response.code === 200 || response.code === 0) {
      removeVideo(videoId);
    } else {
      errorMessage.value = response.message || '删除失败';
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '删除失败';
  } finally {
    isDeleting.value = false;
  }
}

function handleProceed(): void {
  if (canProceed.value) {
    router.push(`/task/${taskId.value}`);
  }
}

function handleBack(): void {
  router.push('/create');
}

onMounted(async () => {
  isLoading.value = true;
  await Promise.all([loadTask(), loadVideos()]);
  isLoading.value = false;
});
</script>

<template>
  <div class="task-upload-page">
    <div class="page-header">
      <h1 class="page-title">上传视频</h1>
      <p class="page-subtitle">{{ task?.shop_name || '加载中...' }}</p>
    </div>

    <StepIndicator :steps="steps" :current-step="currentStep" />

    <div v-if="isLoading" class="loading-container">
      <div class="loading-spinner" />
      <span>加载中...</span>
    </div>

    <div v-else class="page-content">
      <!-- Error Messages -->
      <div v-if="errorMessage" class="error-banner">
        <span class="error-icon">⚠️</span>
        <span>{{ errorMessage }}</span>
        <button class="dismiss-btn" @click="errorMessage = ''">✕</button>
      </div>

      <div v-if="validationErrors.length > 0" class="warning-banner">
        <div v-for="(error, index) in validationErrors" :key="index">
          {{ error }}
        </div>
      </div>

      <div v-if="uploadError" class="error-banner">
        <span class="error-icon">⚠️</span>
        <span>{{ uploadError }}</span>
      </div>

      <!-- Upload Card -->
      <div class="upload-card">
        <div class="card-header">
          <h2 class="card-title">上传探店视频</h2>
          <p class="card-description">
            请上传 {{ VIDEO_UPLOAD_CONSTRAINTS.MIN_VIDEO_COUNT }}-{{ VIDEO_UPLOAD_CONSTRAINTS.MAX_VIDEO_COUNT }} 个探店视频，
            AI 将分析并筛选最佳镜头
          </p>
        </div>

        <!-- Video Uploader -->
        <VideoUploader
          :current-count="totalCount"
          :max-count="VIDEO_UPLOAD_CONSTRAINTS.MAX_VIDEO_COUNT"
          :disabled="isUploading"
          @files-selected="handleFilesSelected"
          @validation-error="handleValidationError"
        />

        <!-- Upload Progress -->
        <UploadProgress
          :files="uploadingFiles"
          @cancel="cancelUpload"
          @remove="removeFromQueue"
          @retry="retryUpload"
        />

        <!-- Start Upload Button -->
        <div v-if="hasPendingUploads" class="upload-actions">
          <button
            class="upload-btn"
            :disabled="isUploading"
            @click="handleStartUpload"
          >
            <span v-if="isUploading">
              <span class="btn-spinner" />
              上传中...
            </span>
            <span v-else>
              开始上传 ({{ uploadingFiles.filter(f => f.status === 'pending').length }} 个文件)
            </span>
          </button>
        </div>

        <!-- Uploaded Videos -->
        <div v-if="uploadedVideos.length > 0" class="uploaded-section">
          <h3 class="section-title">
            已上传视频 ({{ uploadedVideos.length }}/{{ VIDEO_UPLOAD_CONSTRAINTS.MAX_VIDEO_COUNT }})
          </h3>
          <VideoList
            :videos="uploadedVideos"
            :loading="isLoadingVideos"
            @delete="handleDeleteVideo"
          />
        </div>

        <!-- Progress Indicator -->
        <div class="progress-indicator">
          <div class="progress-bar-bg">
            <div
              class="progress-bar-fill"
              :style="{ width: `${Math.min((uploadedVideos.length / VIDEO_UPLOAD_CONSTRAINTS.MIN_VIDEO_COUNT) * 100, 100)}%` }"
            />
          </div>
          <span class="progress-text">
            {{ uploadedVideos.length }} / {{ VIDEO_UPLOAD_CONSTRAINTS.MIN_VIDEO_COUNT }} (最少)
          </span>
        </div>
      </div>

      <!-- Actions -->
      <div class="page-actions">
        <button class="back-btn" @click="handleBack">
          ← 返回修改信息
        </button>
        <button
          class="proceed-btn"
          :disabled="!canProceed"
          @click="handleProceed"
        >
          开始AI分析 →
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.task-upload-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: #6b7280;
  gap: 16px;
}

.loading-spinner,
.btn-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e5e7eb;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.btn-spinner {
  width: 16px;
  height: 16px;
  border-width: 2px;
  display: inline-block;
  vertical-align: middle;
  margin-right: 8px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.page-content {
  margin-top: 32px;
}

.error-banner,
.warning-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 16px;
}

.error-banner {
  background-color: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
}

.warning-banner {
  background-color: #fffbeb;
  border: 1px solid #fde68a;
  color: #d97706;
  flex-direction: column;
  align-items: flex-start;
}

.error-icon {
  flex-shrink: 0;
}

.dismiss-btn {
  margin-left: auto;
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  padding: 4px;
}

.upload-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 32px;
}

.card-header {
  margin-bottom: 24px;
}

.card-title {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 8px 0;
}

.card-description {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.upload-actions {
  margin-top: 16px;
  text-align: center;
}

.upload-btn {
  padding: 12px 32px;
  background-color: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.upload-btn:hover:not(:disabled) {
  background-color: #2563eb;
}

.upload-btn:disabled {
  background-color: #9ca3af;
  cursor: not-allowed;
}

.uploaded-section {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 16px 0;
}

.progress-indicator {
  margin-top: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-bar-bg {
  flex: 1;
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: #3b82f6;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 14px;
  color: #6b7280;
  white-space: nowrap;
}

.page-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 32px;
}

.back-btn {
  padding: 12px 24px;
  background: none;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  color: #374151;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover {
  background: #f9fafb;
  border-color: #9ca3af;
}

.proceed-btn {
  padding: 12px 32px;
  background-color: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.proceed-btn:hover:not(:disabled) {
  background-color: #2563eb;
}

.proceed-btn:disabled {
  background-color: #9ca3af;
  cursor: not-allowed;
}
</style>
