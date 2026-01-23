<script setup lang="ts">
/**
 * Video uploader component with drag-drop support.
 * Handles file selection, validation, and displays upload zone.
 */
import { ref, computed } from 'vue';
import { VIDEO_UPLOAD_CONSTRAINTS } from '../../types/task';

const props = defineProps<{
  currentCount: number;
  maxCount: number;
  disabled?: boolean;
}>();

const emit = defineEmits<{
  (e: 'files-selected', files: File[]): void;
  (e: 'validation-error', errors: string[]): void;
}>();

const isDragging = ref(false);
const fileInputRef = ref<HTMLInputElement | null>(null);

const remainingSlots = computed(() => props.maxCount - props.currentCount);
const canAddMore = computed(() => remainingSlots.value > 0 && !props.disabled);

const acceptedFormats = VIDEO_UPLOAD_CONSTRAINTS.ACCEPTED_EXTENSIONS.join(',');

function handleDragEnter(event: DragEvent): void {
  event.preventDefault();
  if (canAddMore.value) {
    isDragging.value = true;
  }
}

function handleDragOver(event: DragEvent): void {
  event.preventDefault();
}

function handleDragLeave(event: DragEvent): void {
  event.preventDefault();
  // Only set to false if leaving the drop zone entirely
  const relatedTarget = event.relatedTarget as Node | null;
  if (!event.currentTarget || !(event.currentTarget as Node).contains(relatedTarget)) {
    isDragging.value = false;
  }
}

function handleDrop(event: DragEvent): void {
  event.preventDefault();
  isDragging.value = false;

  if (!canAddMore.value) return;

  const files = event.dataTransfer?.files;
  if (files && files.length > 0) {
    processFiles(files);
  }
}

function handleFileInput(event: Event): void {
  const input = event.target as HTMLInputElement;
  if (input.files && input.files.length > 0) {
    processFiles(input.files);
    // Reset input to allow selecting same file again
    input.value = '';
  }
}

function processFiles(fileList: FileList): void {
  const files = Array.from(fileList);
  const validFiles: File[] = [];
  const errors: string[] = [];

  // Check total count first
  if (files.length > remainingSlots.value) {
    errors.push(`最多还能上传 ${remainingSlots.value} 个视频`);
  }

  // Validate each file
  const filesToProcess = files.slice(0, remainingSlots.value);
  const acceptedTypes: readonly string[] = VIDEO_UPLOAD_CONSTRAINTS.ACCEPTED_TYPES;
  for (const file of filesToProcess) {
    // Check type
    const isValidType = acceptedTypes.includes(file.type) ||
      VIDEO_UPLOAD_CONSTRAINTS.ACCEPTED_EXTENSIONS.some(ext =>
        file.name.toLowerCase().endsWith(ext)
      );

    if (!isValidType) {
      errors.push(`不支持的文件格式: ${file.name}`);
      continue;
    }

    // Check size
    if (file.size > VIDEO_UPLOAD_CONSTRAINTS.MAX_FILE_SIZE) {
      const sizeMB = (file.size / (1024 * 1024)).toFixed(1);
      errors.push(`文件过大: ${file.name} (${sizeMB}MB)，最大 100MB`);
      continue;
    }

    validFiles.push(file);
  }

  if (errors.length > 0) {
    emit('validation-error', errors);
  }

  if (validFiles.length > 0) {
    emit('files-selected', validFiles);
  }
}

function triggerFileInput(): void {
  if (canAddMore.value) {
    fileInputRef.value?.click();
  }
}
</script>

<template>
  <div
    class="video-uploader"
    :class="{
      'is-dragging': isDragging,
      'is-disabled': !canAddMore
    }"
    @dragenter="handleDragEnter"
    @dragover="handleDragOver"
    @dragleave="handleDragLeave"
    @drop="handleDrop"
    @click="triggerFileInput"
  >
    <input
      ref="fileInputRef"
      type="file"
      :accept="acceptedFormats"
      multiple
      class="file-input"
      @change="handleFileInput"
    />

    <div class="upload-content">
      <div class="upload-icon">
        <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
          <polyline points="17 8 12 3 7 8" />
          <line x1="12" y1="3" x2="12" y2="15" />
        </svg>
      </div>

      <div v-if="isDragging" class="upload-text">
        <span class="upload-title">释放上传</span>
      </div>
      <div v-else-if="canAddMore" class="upload-text">
        <span class="upload-title">拖拽视频文件到此处</span>
        <span class="upload-subtitle">或 <span class="upload-link">点击选择文件</span></span>
        <span class="upload-hint">支持 MP4/MOV，单个≤100MB</span>
      </div>
      <div v-else class="upload-text">
        <span class="upload-title">已达上传上限</span>
        <span class="upload-subtitle">最多上传 {{ maxCount }} 个视频</span>
      </div>
    </div>

    <div class="upload-count">
      已上传: {{ currentCount }}/{{ maxCount }}
    </div>
  </div>
</template>

<style scoped>
.video-uploader {
  position: relative;
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  padding: 40px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background-color: #fafafa;
}

.video-uploader:hover:not(.is-disabled) {
  border-color: #3b82f6;
  background-color: #eff6ff;
}

.video-uploader.is-dragging {
  border-color: #3b82f6;
  background-color: #dbeafe;
  border-style: solid;
}

.video-uploader.is-disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.file-input {
  display: none;
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.upload-icon {
  color: #9ca3af;
  transition: color 0.2s;
}

.video-uploader:hover:not(.is-disabled) .upload-icon,
.video-uploader.is-dragging .upload-icon {
  color: #3b82f6;
}

.upload-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.upload-title {
  font-size: 16px;
  font-weight: 500;
  color: #374151;
}

.upload-subtitle {
  font-size: 14px;
  color: #6b7280;
}

.upload-link {
  color: #3b82f6;
  font-weight: 500;
}

.upload-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

.upload-count {
  position: absolute;
  bottom: 12px;
  right: 16px;
  font-size: 12px;
  color: #6b7280;
  background: white;
  padding: 4px 8px;
  border-radius: 4px;
}
</style>
