<script setup lang="ts">
/**
 * Frame Gallery component.
 * Story 2.4: 分析进度展示页面 - AC2
 *
 * Displays:
 * - Tab categories with counts (BR-2.4)
 * - Frame grid with thumbnails, category, quality score
 * - Recommended frame markers (BR-2.2)
 * - Empty state guidance
 */
import { ref } from 'vue';
import type { VideoFrame, FrameTab, TabCategory } from '../../types/analysis';
import { CATEGORY_LABELS } from '../../types/analysis';

const props = defineProps<{
  frames: VideoFrame[];
  tabs: FrameTab[];
  activeTab: TabCategory;
}>();

const emit = defineEmits<{
  (e: 'tab-change', tab: TabCategory): void;
  (e: 'preview', frame: VideoFrame): void;
}>();

const placeholderUrl = '/placeholder-frame.svg';

/**
 * Handle image load error.
 */
function handleImageError(event: Event): void {
  const img = event.target as HTMLImageElement;
  img.src = placeholderUrl;
}

/**
 * Handle tab click.
 */
function handleTabClick(tab: TabCategory): void {
  emit('tab-change', tab);
}

/**
 * Handle frame click for preview.
 */
function handleFrameClick(frame: VideoFrame): void {
  emit('preview', frame);
}

/**
 * Get category label in Chinese.
 */
function getCategoryLabel(category: string): string {
  return CATEGORY_LABELS[category as TabCategory] || category;
}

/**
 * Format tags for tooltip.
 */
function formatTags(tags: string[]): string {
  return tags.join(', ');
}
</script>

<template>
  <div class="frame-gallery">
    <!-- Empty State -->
    <div v-if="frames.length === 0" class="empty-state">
      <div class="empty-icon">📷</div>
      <h3 class="empty-title">暂无分析结果</h3>
      <p class="empty-message">请上传视频后重新开始分析</p>
    </div>

    <!-- Gallery Content -->
    <div v-else class="gallery-content">
      <!-- Tabs -->
      <div class="tabs-container">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-item"
          :class="{ active: activeTab === tab.key }"
          @click="handleTabClick(tab.key)"
        >
          {{ tab.label }}({{ tab.count }})
        </button>
      </div>

      <!-- Frame Grid -->
      <div class="frame-grid">
        <div
          v-for="frame in frames"
          :key="frame.id"
          class="frame-card"
          :title="formatTags(frame.tags)"
          :data-tags="frame.tags.join(',')"
          @click="handleFrameClick(frame)"
        >
          <div class="frame-thumbnail">
            <img
              :src="frame.thumbnailUrl || placeholderUrl"
              :alt="`Frame ${frame.id}`"
              loading="lazy"
              @error="handleImageError"
            />
            <!-- Quality Score (BR-2.3: bottom-right) -->
            <span class="quality-score">{{ frame.qualityScore }}</span>
            <!-- Recommended Marker (BR-2.2) -->
            <span v-if="frame.isRecommended" class="recommended-marker">⭐</span>
          </div>
          <div class="frame-info">
            <span class="frame-category">{{ getCategoryLabel(frame.category) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.frame-gallery {
  width: 100%;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 64px 24px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 8px 0;
}

.empty-message {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

/* Tabs */
.tabs-container {
  display: flex;
  gap: 8px;
  padding: 16px 0;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 24px;
  overflow-x: auto;
}

.tab-item {
  padding: 8px 16px;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  font-size: 14px;
  color: #4b5563;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.tab-item:hover {
  background: #e5e7eb;
}

.tab-item.active {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
}

/* Frame Grid */
.frame-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
}

.frame-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border-radius: 8px;
  overflow: hidden;
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.frame-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.frame-thumbnail {
  position: relative;
  aspect-ratio: 9 / 16;
  background: #f3f4f6;
  overflow: hidden;
}

.frame-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Quality Score (BR-2.3: bottom-right) */
.quality-score {
  position: absolute;
  bottom: 8px;
  right: 8px;
  padding: 4px 8px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  font-size: 12px;
  font-weight: 600;
  border-radius: 4px;
}

/* Recommended Marker (BR-2.2) */
.recommended-marker {
  position: absolute;
  top: 8px;
  left: 8px;
  font-size: 20px;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.3));
}

.frame-info {
  padding: 12px;
}

.frame-category {
  font-size: 12px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 4px 8px;
  border-radius: 4px;
}

/* Responsive */
@media (min-width: 768px) {
  .frame-grid {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  }
}

@media (min-width: 1024px) {
  .frame-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }
}
</style>
