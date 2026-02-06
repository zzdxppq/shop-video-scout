<!--
  Delete Confirmation Modal
  Story 5.5: 历史任务管理

  Displays a confirmation dialog for delete operations.
-->
<script setup lang="ts">
import { watch } from 'vue';

const props = defineProps<{
  /** Whether the modal is visible */
  visible: boolean;
  /** Modal title */
  title?: string;
  /** Confirmation message */
  message: string;
  /** Whether delete operation is in progress */
  loading?: boolean;
  /** Confirm button text */
  confirmText?: string;
  /** Cancel button text */
  cancelText?: string;
}>();

const emit = defineEmits<{
  /** Emitted when user confirms deletion */
  confirm: [];
  /** Emitted when user cancels deletion */
  cancel: [];
}>();

// Handle escape key
watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      const handleEscape = (e: KeyboardEvent): void => {
        if (e.key === 'Escape' && !props.loading) {
          emit('cancel');
        }
      };
      document.addEventListener('keydown', handleEscape);
      return () => document.removeEventListener('keydown', handleEscape);
    }
  }
);

const handleConfirm = (): void => {
  if (!props.loading) {
    emit('confirm');
  }
};

const handleCancel = (): void => {
  if (!props.loading) {
    emit('cancel');
  }
};

const handleBackdropClick = (e: MouseEvent): void => {
  if (e.target === e.currentTarget && !props.loading) {
    emit('cancel');
  }
};
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="visible"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black bg-opacity-50"
        @click="handleBackdropClick"
      >
        <div
          class="bg-white rounded-lg shadow-xl max-w-sm w-full p-6"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="title ? 'modal-title' : undefined"
        >
          <!-- Warning Icon -->
          <div class="flex justify-center mb-4">
            <div class="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center">
              <svg
                class="w-6 h-6 text-red-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                />
              </svg>
            </div>
          </div>

          <!-- Title -->
          <h3
            v-if="title"
            id="modal-title"
            class="text-lg font-medium text-gray-900 text-center mb-2"
          >
            {{ title }}
          </h3>

          <!-- Message -->
          <p class="text-sm text-gray-500 text-center mb-6">
            {{ message }}
          </p>

          <!-- Buttons -->
          <div class="flex space-x-3">
            <button
              type="button"
              class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="loading"
              @click="handleCancel"
            >
              {{ cancelText || '取消' }}
            </button>
            <button
              type="button"
              class="flex-1 px-4 py-2 text-sm font-medium text-white bg-red-500 rounded-md hover:bg-red-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
              :disabled="loading"
              @click="handleConfirm"
            >
              <svg
                v-if="loading"
                class="animate-spin -ml-1 mr-2 h-4 w-4 text-white"
                fill="none"
                viewBox="0 0 24 24"
              >
                <circle
                  class="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  stroke-width="4"
                />
                <path
                  class="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                />
              </svg>
              {{ loading ? '删除中...' : (confirmText || '确认删除') }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-active .bg-white,
.modal-leave-active .bg-white {
  transition: transform 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .bg-white,
.modal-leave-to .bg-white {
  transform: scale(0.95);
}
</style>
