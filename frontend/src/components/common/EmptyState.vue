<!--
  Empty State Component
  Story 5.5: 历史任务管理

  Displays an empty state with icon, title, description, and optional action button.
-->
<script setup lang="ts">
defineProps<{
  /** Icon to display (emoji or icon component slot) */
  icon?: string;
  /** Main title */
  title: string;
  /** Description text */
  description?: string;
  /** Action button text */
  actionText?: string;
}>();

const emit = defineEmits<{
  /** Emitted when action button is clicked */
  action: [];
}>();

const handleAction = (): void => {
  emit('action');
};
</script>

<template>
  <div class="empty-state flex flex-col items-center justify-center py-12 px-4 text-center">
    <!-- Icon slot or emoji -->
    <div class="text-5xl mb-4">
      <slot name="icon">
        <span v-if="icon">{{ icon }}</span>
        <span v-else class="text-gray-300">📋</span>
      </slot>
    </div>

    <!-- Title -->
    <h3 class="text-lg font-medium text-gray-900 mb-2">
      {{ title }}
    </h3>

    <!-- Description -->
    <p v-if="description" class="text-sm text-gray-500 max-w-sm mb-6">
      {{ description }}
    </p>

    <!-- Action button -->
    <button
      v-if="actionText"
      type="button"
      class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-500 hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
      @click="handleAction"
    >
      {{ actionText }}
    </button>

    <!-- Additional slot for custom content -->
    <slot name="extra"></slot>
  </div>
</template>

<style scoped>
.empty-state {
  min-height: 200px;
}
</style>
