<script setup lang="ts">
/**
 * Style card component.
 * Story 4.5: 字幕设置页面 - AC2
 *
 * Displays a single subtitle style option with preview image.
 * BR-2.4: 样式卡片显示：预览图 + 样式名称
 * BR-2.5: 选中卡片显示蓝色边框高亮
 */
import { ref, computed } from 'vue';
import type { SubtitleStyle } from '../../types/subtitle';

const props = withDefaults(defineProps<{
  /** Style data */
  style: SubtitleStyle;
  /** Whether this style is selected */
  selected?: boolean;
  /** Whether the card is disabled */
  disabled?: boolean;
}>(), {
  selected: false,
  disabled: false
});

const emit = defineEmits<{
  /** Emitted when user selects this style */
  (e: 'select', styleId: string): void;
}>();

const imageError = ref(false);

const isInteractive = computed(() => !props.disabled);

function handleClick() {
  if (isInteractive.value) {
    emit('select', props.style.id);
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    handleClick();
  }
}

function handleImageError() {
  imageError.value = true;
}
</script>

<template>
  <div
    role="button"
    :tabindex="disabled ? -1 : 0"
    :aria-selected="selected"
    :aria-disabled="disabled"
    class="relative rounded-lg border-2 overflow-hidden transition-all duration-200"
    :class="[
      selected ? 'border-blue-500 ring-2 ring-blue-200' : 'border-gray-200',
      disabled ? 'grayscale opacity-60 cursor-not-allowed' : 'cursor-pointer hover:scale-[1.02] hover:shadow-md',
    ]"
    @click="handleClick"
    @keydown="handleKeydown"
  >
    <!-- Preview image -->
    <div class="aspect-video bg-gray-100 relative">
      <img
        v-if="!imageError"
        :src="style.previewUrl"
        :alt="`${style.name} 预览`"
        class="w-full h-full object-cover"
        @error="handleImageError"
      />

      <!-- Fallback placeholder -->
      <div
        v-else
        class="absolute inset-0 flex items-center justify-center bg-gradient-to-br from-gray-200 to-gray-300"
      >
        <span class="text-gray-500 text-sm">{{ style.name }}</span>
      </div>
    </div>

    <!-- Style name -->
    <div class="px-3 py-2 bg-white">
      <p
        class="text-sm font-medium text-center truncate"
        :class="selected ? 'text-blue-600' : 'text-gray-700'"
      >
        {{ style.name }}
      </p>
    </div>

    <!-- Selected checkmark -->
    <div
      v-if="selected"
      class="absolute top-2 right-2 w-5 h-5 bg-blue-500 rounded-full flex items-center justify-center"
    >
      <svg class="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
        <path
          fill-rule="evenodd"
          d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
          clip-rule="evenodd"
        />
      </svg>
    </div>
  </div>
</template>
