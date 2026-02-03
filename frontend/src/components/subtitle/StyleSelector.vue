<script setup lang="ts">
/**
 * Style selector component.
 * Story 4.5: 字幕设置页面 - AC2
 *
 * Displays a grid of subtitle style cards for selection.
 * BR-2.1: 5种预设样式模板卡片
 * BR-2.2: 默认选中"简约白字"样式
 */
import StyleCard from './StyleCard.vue';
import { SUBTITLE_STYLES, DEFAULT_SUBTITLE_STYLE } from '../../types/subtitle';
import type { SubtitleStyleId } from '../../types/subtitle';

const props = withDefaults(defineProps<{
  /** Currently selected style */
  selectedStyle?: SubtitleStyleId;
  /** Whether the selector is disabled */
  disabled?: boolean;
}>(), {
  selectedStyle: DEFAULT_SUBTITLE_STYLE,
  disabled: false
});

const emit = defineEmits<{
  /** Emitted when user selects a style */
  (e: 'update:selectedStyle', styleId: SubtitleStyleId): void;
}>();

function handleStyleSelect(styleId: string) {
  if (!props.disabled) {
    emit('update:selectedStyle', styleId as SubtitleStyleId);
  }
}
</script>

<template>
  <div class="space-y-3">
    <h3 class="text-sm font-medium text-gray-700">字幕样式</h3>

    <div
      class="grid grid-cols-2 sm:grid-cols-3 gap-3"
      role="listbox"
      :aria-disabled="disabled"
    >
      <StyleCard
        v-for="style in SUBTITLE_STYLES"
        :key="style.id"
        :style="style"
        :selected="style.id === selectedStyle"
        :disabled="disabled"
        @select="handleStyleSelect"
      />
    </div>
  </div>
</template>
