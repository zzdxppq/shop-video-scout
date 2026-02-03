<script setup lang="ts">
/**
 * Subtitle settings container component.
 * Story 4.5: 字幕设置页面
 *
 * Composes SubtitleToggle and StyleSelector with shared state management.
 * BR-1.3: 关闭时字幕样式选择区域置灰禁用
 */
import { watch } from 'vue';
import SubtitleToggle from './SubtitleToggle.vue';
import StyleSelector from './StyleSelector.vue';
import { useSubtitleSettings } from '../../composables/useSubtitleSettings';
import type { SubtitleStyleId } from '../../types/subtitle';

const props = defineProps<{
  /** Task ID for API calls */
  taskId: number;
  /** Initial subtitle enabled state */
  initialEnabled?: boolean;
  /** Initial subtitle style */
  initialStyle?: SubtitleStyleId;
}>();

const emit = defineEmits<{
  /** Emitted when settings are saved with error */
  (e: 'error', message: string): void;
}>();

const {
  subtitleEnabled,
  subtitleStyle,
  loading,
  error,
  initSettings,
  toggleSubtitle,
  setStyle,
  clearError
} = useSubtitleSettings();

// Initialize settings from props
if (props.initialEnabled !== undefined && props.initialStyle !== undefined) {
  initSettings(props.initialEnabled, props.initialStyle);
}

// Watch for errors and emit to parent
watch(error, (newError) => {
  if (newError) {
    emit('error', newError);
    // Auto-clear error after emitting
    setTimeout(clearError, 100);
  }
});

async function handleToggle(enabled: boolean) {
  await toggleSubtitle(props.taskId);
}

async function handleStyleChange(style: SubtitleStyleId) {
  await setStyle(props.taskId, style);
}
</script>

<template>
  <div class="bg-white rounded-lg border border-gray-200 p-4 space-y-4">
    <h2 class="text-base font-semibold text-gray-800">字幕设置</h2>

    <!-- Subtitle toggle -->
    <SubtitleToggle
      :enabled="subtitleEnabled"
      :loading="loading"
      @update:enabled="handleToggle"
    />

    <!-- Divider -->
    <div class="border-t border-gray-100" />

    <!-- Style selector -->
    <StyleSelector
      :selected-style="subtitleStyle"
      :disabled="!subtitleEnabled"
      @update:selected-style="handleStyleChange"
    />
  </div>
</template>
