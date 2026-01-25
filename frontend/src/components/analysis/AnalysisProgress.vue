<script setup lang="ts">
/**
 * Analysis Progress component.
 * Story 2.4: 分析进度展示页面 - AC1
 *
 * Displays:
 * - Progress bar with percentage (BR-1.3: linear easing, 100ms)
 * - Video count "已分析 X/Y 个视频"
 * - Estimated time remaining
 * - Stage list with status markers (BR-1.2)
 * - Completion/error states
 */
import { computed } from 'vue';
import type { AnalysisProgress } from '../../types/analysis';

const props = withDefaults(defineProps<{
  progress: AnalysisProgress;
  showLongWaitWarning?: boolean;
  showRetryOption?: boolean;
}>(), {
  showLongWaitWarning: false,
  showRetryOption: false
});

const emit = defineEmits<{
  (e: 'retry'): void;
}>();

/**
 * Format time in minutes and seconds.
 */
function formatTime(seconds: number): string {
  if (seconds <= 0) return '0 秒';
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  if (mins === 0) return `${secs} 秒`;
  if (secs === 0) return `${mins} 分`;
  return `${mins} 分 ${secs} 秒`;
}

const isCompleted = computed(() => props.progress.status === 'completed');
const isFailed = computed(() => props.progress.status === 'failed');
const isAnalyzing = computed(() => props.progress.status === 'analyzing');

/**
 * Get marker symbol for stage status.
 * ✓ = completed, ● = in_progress, ○ = pending
 */
function getStageMarker(status: string): string {
  switch (status) {
    case 'completed': return '✓';
    case 'in_progress': return '●';
    default: return '○';
  }
}

function handleRetry(): void {
  emit('retry');
}
</script>

<template>
  <div class="analysis-progress">
    <!-- Completion State -->
    <div v-if="isCompleted" class="completion-state">
      <div class="completion-icon">✓</div>
      <h2 class="completion-title">分析完成</h2>
      <p class="completion-message">正在跳转到脚本编辑页...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="isFailed" class="error-state">
      <div class="error-icon">⚠️</div>
      <h2 class="error-title">分析失败</h2>
      <p class="error-message">{{ progress.error || '分析失败，请重试' }}</p>
      <button class="retry-btn" @click="handleRetry">重新分析</button>
    </div>

    <!-- Progress State -->
    <div v-else class="progress-state">
      <!-- Progress Bar -->
      <div class="progress-section">
        <div class="progress-header">
          <span class="progress-label">分析进度</span>
          <span class="progress-percent">{{ progress.percent }}%</span>
        </div>
        <div class="progress-bar-bg">
          <div
            class="progress-bar-fill"
            :style="{ width: `${progress.percent}%`, transition: 'width 100ms linear' }"
          />
        </div>
      </div>

      <!-- Video Count -->
      <div class="video-count">
        已分析 {{ progress.currentVideo }}/{{ progress.totalVideos }} 个视频
      </div>

      <!-- Estimated Time -->
      <div class="estimated-time">
        预计剩余 {{ formatTime(progress.estimatedTimeRemaining) }}
      </div>

      <!-- Stage List (BR-1.2) -->
      <div class="stages-section">
        <h3 class="stages-title">处理阶段</h3>
        <ul class="stage-list">
          <li
            v-for="stage in progress.stages"
            :key="stage.id"
            class="stage-item"
            :class="{ 'stage-active': stage.status === 'in_progress' }"
          >
            <span
              :class="[
                'stage-marker',
                `stage-marker-${stage.status}`
              ]"
            >
              {{ getStageMarker(stage.status) }}
            </span>
            <span class="stage-label">{{ stage.label }}</span>
          </li>
        </ul>
      </div>

      <!-- Long Wait Warning -->
      <div v-if="showLongWaitWarning" class="long-wait-warning">
        <span class="warning-icon">⏳</span>
        分析时间较长，请耐心等待
      </div>

      <!-- Retry Option (after 5 minutes) -->
      <div v-if="showRetryOption" class="retry-section">
        <p class="retry-hint">分析时间过长？</p>
        <button class="retry-btn" @click="handleRetry">重新分析</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.analysis-progress {
  max-width: 500px;
  margin: 0 auto;
  padding: 32px;
}

/* Completion State */
.completion-state {
  text-align: center;
  padding: 48px 24px;
  animation: fadeIn 0.3s ease;
}

.completion-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  background: #10b981;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  animation: scaleIn 0.3s ease;
}

.completion-title {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 8px 0;
}

.completion-message {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

/* Error State */
.error-state {
  text-align: center;
  padding: 48px 24px;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.error-title {
  font-size: 20px;
  font-weight: 600;
  color: #dc2626;
  margin: 0 0 8px 0;
}

.error-message {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 24px 0;
}

/* Progress State */
.progress-state {
  animation: fadeIn 0.3s ease;
}

.progress-section {
  margin-bottom: 24px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.progress-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.progress-percent {
  font-size: 14px;
  font-weight: 600;
  color: #3b82f6;
}

.progress-bar-bg {
  height: 12px;
  background: #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #60a5fa);
  border-radius: 6px;
  /* BR-1.3: linear easing, 100ms transition */
  transition: width 100ms linear;
}

.video-count {
  font-size: 16px;
  font-weight: 500;
  color: #111827;
  text-align: center;
  margin-bottom: 8px;
}

.estimated-time {
  font-size: 14px;
  color: #6b7280;
  text-align: center;
  margin-bottom: 32px;
}

/* Stages Section */
.stages-section {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
}

.stages-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 16px 0;
}

.stage-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.stage-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  font-size: 14px;
  color: #6b7280;
}

.stage-item.stage-active {
  color: #111827;
  font-weight: 500;
}

.stage-marker {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.stage-marker-completed {
  color: #10b981;
}

.stage-marker-in_progress {
  color: #3b82f6;
  animation: pulse 1s infinite;
}

.stage-marker-pending {
  color: #d1d5db;
}

.stage-label {
  flex: 1;
}

/* Long Wait Warning */
.long-wait-warning {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  padding: 12px 16px;
  background: #fef3c7;
  border: 1px solid #fde68a;
  border-radius: 8px;
  font-size: 14px;
  color: #92400e;
}

.warning-icon {
  font-size: 16px;
}

/* Retry Section */
.retry-section {
  text-align: center;
  margin-top: 24px;
}

.retry-hint {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 12px 0;
}

.retry-btn {
  padding: 10px 24px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.retry-btn:hover {
  background: #2563eb;
}

/* Animations */
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes scaleIn {
  from {
    transform: scale(0);
  }
  to {
    transform: scale(1);
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>
