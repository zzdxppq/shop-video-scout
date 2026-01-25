<script setup lang="ts">
/**
 * Task Analysis View - Step 3: AI Analysis Progress.
 * Story 2.4: 分析进度展示页面
 *
 * Features:
 * - Progress polling (BR-1.1: 2s interval)
 * - Stage display (BR-1.2)
 * - Auto-redirect on completion (BR-1.4)
 * - beforeunload handler (BR-1.5)
 * - Frame gallery with tabs (AC2)
 */
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import StepIndicator from '../components/task/StepIndicator.vue';
import AnalysisProgress from '../components/analysis/AnalysisProgress.vue';
import FrameGallery from '../components/analysis/FrameGallery.vue';
import { useAnalysisStore } from '../stores/analysis';
import { useProgressPolling } from '../composables/useProgressPolling';
import { getTask, getTaskVideos } from '../api/task';
import { getTaskFrames } from '../api/analysis';
import type { TaskResponse } from '../types/task';
import type { VideoFrame } from '../types/analysis';

const route = useRoute();
const router = useRouter();
const store = useAnalysisStore();

const taskId = computed(() => Number(route.params.id));

const steps = [
  { label: '填写信息', description: '店铺基本信息' },
  { label: '上传视频', description: '素材视频' },
  { label: 'AI分析', description: '智能分析' },
  { label: '编辑脚本', description: '口播文案' },
  { label: '预览导出', description: '生成视频' }
];

const currentStep = ref(2);
const task = ref<TaskResponse | null>(null);
const isLoadingTask = ref(true);
const taskError = ref('');
const previewFrame = ref<VideoFrame | null>(null);
const showPreviewModal = ref(false);

/**
 * Polling composable for progress updates.
 */
const {
  isPolling,
  showLongWaitWarning,
  showRetryOption,
  retry
} = useProgressPolling({
  taskId,
  onComplete: handleAnalysisComplete,
  onError: handleAnalysisError
});

/**
 * Computed properties from store.
 */
const progress = computed(() => store.progress);
const frames = computed(() => store.filteredFrames);
const tabs = computed(() => store.tabs);
const activeTab = computed(() => store.activeTab);
const isCompleted = computed(() => store.isCompleted);

/**
 * Load task details.
 */
async function loadTask(): Promise<void> {
  try {
    const response = await getTask(taskId.value);
    if (response.code === 200 || response.code === 0) {
      task.value = response.data;
    } else {
      taskError.value = response.message || '加载任务失败';
    }
  } catch (error) {
    taskError.value = error instanceof Error ? error.message : '加载任务失败';
  }
}

/**
 * Load analyzed frames after completion.
 */
async function loadFrames(): Promise<void> {
  try {
    const response = await getTaskFrames(taskId.value);
    if (response.code === 200 || response.code === 0) {
      store.setFrames(response.data || []);
    }
  } catch (error) {
    console.error('Failed to load frames:', error);
  }
}

/**
 * Handle analysis completion.
 */
async function handleAnalysisComplete(): Promise<void> {
  await loadFrames();
}

/**
 * Handle analysis error.
 */
function handleAnalysisError(error: string): void {
  console.error('Analysis error:', error);
}

/**
 * Handle retry button click.
 */
async function handleRetry(): Promise<void> {
  await retry();
}

/**
 * Handle tab change in frame gallery.
 */
function handleTabChange(tab: string): void {
  store.setActiveTab(tab as typeof activeTab.value);
}

/**
 * Handle frame preview.
 */
function handleFramePreview(frame: VideoFrame): void {
  previewFrame.value = frame;
  showPreviewModal.value = true;
}

/**
 * Close preview modal.
 */
function closePreviewModal(): void {
  showPreviewModal.value = false;
  previewFrame.value = null;
}

/**
 * Navigate back to upload page.
 */
function handleBack(): void {
  router.push(`/task/${taskId.value}/upload`);
}

onMounted(async () => {
  isLoadingTask.value = true;
  await loadTask();
  isLoadingTask.value = false;
});

// Load frames when analysis completes
watch(isCompleted, (completed) => {
  if (completed) {
    loadFrames();
  }
});
</script>

<template>
  <div class="task-analysis-page">
    <div class="page-header">
      <h1 class="page-title">AI分析</h1>
      <p class="page-subtitle">{{ task?.shop_name || '加载中...' }}</p>
    </div>

    <StepIndicator :steps="steps" :current-step="currentStep" />

    <div v-if="isLoadingTask" class="loading-container">
      <div class="loading-spinner" />
      <span>加载中...</span>
    </div>

    <div v-else-if="taskError" class="error-container">
      <div class="error-banner">
        <span class="error-icon">⚠️</span>
        <span>{{ taskError }}</span>
      </div>
      <button class="back-btn" @click="handleBack">返回上传页</button>
    </div>

    <div v-else class="page-content">
      <!-- Analysis Progress Section -->
      <div class="analysis-section">
        <div class="analysis-card">
          <AnalysisProgress
            v-if="progress"
            :progress="progress"
            :show-long-wait-warning="showLongWaitWarning"
            :show-retry-option="showRetryOption"
            @retry="handleRetry"
          />
          <div v-else class="loading-progress">
            <div class="loading-spinner" />
            <span>正在获取分析进度...</span>
          </div>
        </div>
      </div>

      <!-- Frame Gallery Section (shown after completion) -->
      <div v-if="isCompleted && frames.length > 0" class="gallery-section">
        <h2 class="section-title">分析结果</h2>
        <FrameGallery
          :frames="frames"
          :tabs="tabs"
          :active-tab="activeTab"
          @tab-change="handleTabChange"
          @preview="handleFramePreview"
        />
      </div>

      <!-- Actions -->
      <div class="page-actions">
        <button class="back-btn" @click="handleBack">
          ← 返回上传
        </button>
      </div>
    </div>

    <!-- Preview Modal -->
    <Teleport to="body">
      <div v-if="showPreviewModal && previewFrame" class="modal-overlay" @click="closePreviewModal">
        <div class="modal-content" @click.stop>
          <button class="modal-close" @click="closePreviewModal">✕</button>
          <div class="preview-image">
            <img :src="previewFrame.thumbnailUrl" :alt="`Frame ${previewFrame.id}`" />
          </div>
          <div class="preview-info">
            <div class="preview-row">
              <span class="preview-label">分类</span>
              <span class="preview-value">{{ previewFrame.category }}</span>
            </div>
            <div class="preview-row">
              <span class="preview-label">质量评分</span>
              <span class="preview-value">{{ previewFrame.qualityScore }}</span>
            </div>
            <div class="preview-row">
              <span class="preview-label">标签</span>
              <span class="preview-value">{{ previewFrame.tags.join(', ') || '-' }}</span>
            </div>
            <div v-if="previewFrame.isRecommended" class="preview-recommended">
              ⭐ 推荐镜头
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.task-analysis-page {
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

.loading-container,
.loading-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: #6b7280;
  gap: 16px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e5e7eb;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.error-container {
  text-align: center;
  padding: 48px 0;
}

.error-banner {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  margin-bottom: 24px;
}

.error-icon {
  font-size: 18px;
}

.page-content {
  margin-top: 32px;
}

.analysis-section {
  margin-bottom: 48px;
}

.analysis-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 32px;
}

.gallery-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 24px 0;
}

.page-actions {
  display: flex;
  justify-content: flex-start;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
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

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
}

.modal-content {
  background: white;
  border-radius: 12px;
  max-width: 500px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  position: relative;
}

.modal-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  background: rgba(0, 0, 0, 0.5);
  border: none;
  border-radius: 50%;
  color: white;
  font-size: 16px;
  cursor: pointer;
  z-index: 1;
}

.modal-close:hover {
  background: rgba(0, 0, 0, 0.7);
}

.preview-image {
  aspect-ratio: 9 / 16;
  background: #f3f4f6;
  border-radius: 12px 12px 0 0;
  overflow: hidden;
}

.preview-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-info {
  padding: 20px;
}

.preview-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
}

.preview-row:last-child {
  border-bottom: none;
}

.preview-label {
  font-size: 14px;
  color: #6b7280;
}

.preview-value {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
}

.preview-recommended {
  margin-top: 16px;
  padding: 12px;
  background: #fef3c7;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #92400e;
  text-align: center;
}
</style>
