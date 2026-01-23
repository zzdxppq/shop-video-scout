<script setup lang="ts">
/**
 * Video list component.
 * Displays uploaded videos in a grid with thumbnails, duration, and delete option.
 */
import type { TaskVideo } from '../../types/task';
import { formatDuration } from '../../composables/useVideoUpload';

defineProps<{
  videos: TaskVideo[];
  loading?: boolean;
}>();

const emit = defineEmits<{
  (e: 'delete', videoId: number): void;
}>();

function getThumbnailUrl(video: TaskVideo): string {
  return video.thumbnail_url || '/placeholder-video.svg';
}

function handleDelete(video: TaskVideo): void {
  emit('delete', video.id);
}
</script>

<template>
  <div class="video-list">
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner" />
      <span>加载中...</span>
    </div>

    <div v-else-if="videos.length === 0" class="empty-state">
      <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18" />
        <line x1="7" y1="2" x2="7" y2="22" />
        <line x1="17" y1="2" x2="17" y2="22" />
        <line x1="2" y1="12" x2="22" y2="12" />
        <line x1="2" y1="7" x2="7" y2="7" />
        <line x1="2" y1="17" x2="7" y2="17" />
        <line x1="17" y1="17" x2="22" y2="17" />
        <line x1="17" y1="7" x2="22" y2="7" />
      </svg>
      <span>暂无视频</span>
    </div>

    <div v-else class="video-grid">
      <div
        v-for="video in videos"
        :key="video.id"
        class="video-item"
      >
        <div class="video-thumbnail">
          <img
            :src="getThumbnailUrl(video)"
            :alt="video.original_filename"
            loading="lazy"
            @error="(e) => (e.target as HTMLImageElement).src = '/placeholder-video.svg'"
          />
          <div class="video-duration">
            {{ formatDuration(video.duration_seconds) }}
          </div>
          <button
            class="delete-btn"
            title="删除视频"
            @click="handleDelete(video)"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="3 6 5 6 21 6" />
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
            </svg>
          </button>
        </div>
        <div class="video-info">
          <span class="video-name" :title="video.original_filename">
            {{ video.original_filename }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.video-list {
  margin-top: 24px;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #9ca3af;
  gap: 12px;
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

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}

.video-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.video-thumbnail {
  position: relative;
  aspect-ratio: 9 / 16;
  background: #f3f4f6;
  border-radius: 8px;
  overflow: hidden;
}

.video-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-duration {
  position: absolute;
  bottom: 8px;
  left: 8px;
  padding: 2px 6px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  font-size: 12px;
  border-radius: 4px;
}

.delete-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: rgba(0, 0, 0, 0.5);
  border: none;
  border-radius: 4px;
  color: white;
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s;
}

.video-thumbnail:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: rgba(239, 68, 68, 0.9);
}

.video-info {
  display: flex;
  flex-direction: column;
}

.video-name {
  font-size: 12px;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
