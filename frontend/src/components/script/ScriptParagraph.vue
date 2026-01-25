<script setup lang="ts">
/**
 * Script paragraph editing component.
 * Story 3.2: 脚本编辑页面 - T1
 *
 * Features:
 * - Click to edit mode
 * - Real-time character count (BR-1.1)
 * - Suggested length hint (BR-1.2)
 * - Cancel and save buttons
 */
import { computed, ref, watch, nextTick } from 'vue';
import type { ScriptParagraph, CharCountInfo } from '../../types/script';
import { SCRIPT_VALIDATION } from '../../types/script';

const props = defineProps<{
  paragraph: ScriptParagraph;
  currentText: string;
  isEdited: boolean;
  isEditing: boolean;
  editText: string;
  charCountInfo: CharCountInfo;
}>();

const emit = defineEmits<{
  startEdit: [];
  updateText: [text: string];
  commitEdit: [];
  cancelEdit: [];
}>();

const textareaRef = ref<HTMLTextAreaElement | null>(null);

// Validation
const validation = computed(() => {
  const text = props.isEditing ? props.editText : props.currentText;
  if (!text || text.trim().length === 0) {
    return { isValid: false, error: '脚本内容不能为空' };
  }
  if (text.length > SCRIPT_VALIDATION.MAX_PARAGRAPH_LENGTH) {
    return { isValid: false, error: `不能超过${SCRIPT_VALIDATION.MAX_PARAGRAPH_LENGTH}字` };
  }
  return { isValid: true, error: null };
});

// Character count display
const charCountDisplay = computed(() => {
  const info = props.charCountInfo;
  return `${info.current}/${info.suggested}字`;
});

const charCountClass = computed(() => {
  const info = props.charCountInfo;
  if (info.isOverLimit) return 'text-red-500';
  if (info.current > info.suggested * 1.2) return 'text-yellow-500';
  return 'text-gray-400';
});

// Shot label display
const shotLabel = computed(() => `【${props.paragraph.section}】镜头#${props.paragraph.shotId}`);

// Focus textarea when entering edit mode
watch(() => props.isEditing, async (editing) => {
  if (editing) {
    await nextTick();
    textareaRef.value?.focus();
    // Move cursor to end
    if (textareaRef.value) {
      textareaRef.value.selectionStart = textareaRef.value.selectionEnd = props.editText.length;
    }
  }
});

function handleClick() {
  if (!props.isEditing) {
    emit('startEdit');
  }
}

function handleInput(event: Event) {
  const target = event.target as HTMLTextAreaElement;
  emit('updateText', target.value);
}

function handleSave() {
  if (validation.value.isValid) {
    emit('commitEdit');
  }
}

function handleCancel() {
  emit('cancelEdit');
}

// Handle Enter key to save, Shift+Enter for newline
function handleKeyDown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    handleSave();
  }
}
</script>

<template>
  <div
    class="script-paragraph rounded-lg border transition-all"
    :class="{
      'border-blue-500 bg-blue-50': isEditing,
      'border-yellow-300 bg-yellow-50': isEdited && !isEditing,
      'border-gray-200 bg-white hover:border-gray-300 cursor-pointer': !isEditing && !isEdited
    }"
  >
    <!-- Header with shot label -->
    <div class="flex items-center justify-between px-4 py-2 border-b border-gray-100">
      <span class="text-sm font-medium text-gray-600">{{ shotLabel }}</span>
      <span
        v-if="!isEditing"
        class="text-xs"
        :class="charCountClass"
      >
        {{ charCountDisplay }}
      </span>
    </div>

    <!-- Content area -->
    <div class="p-4" @click="handleClick">
      <!-- View mode -->
      <div v-if="!isEditing" class="min-h-[60px]">
        <p class="text-gray-800 whitespace-pre-wrap leading-relaxed">
          {{ currentText }}
        </p>
        <p v-if="isEdited" class="text-xs text-yellow-600 mt-2">
          已修改（点击编辑）
        </p>
      </div>

      <!-- Edit mode -->
      <div v-else class="space-y-3" @click.stop>
        <textarea
          ref="textareaRef"
          :value="editText"
          @input="handleInput"
          @keydown="handleKeyDown"
          class="w-full min-h-[100px] p-3 border rounded-lg resize-none focus:outline-none focus:ring-2"
          :class="{
            'border-red-500 focus:ring-red-200': !validation.isValid,
            'border-gray-300 focus:ring-blue-200': validation.isValid
          }"
          placeholder="请输入脚本内容..."
        />

        <!-- Character count and validation -->
        <div class="flex items-center justify-between text-sm">
          <div class="flex items-center space-x-4">
            <span :class="charCountClass">{{ charCountDisplay }}</span>
            <span v-if="!validation.isValid" class="text-red-500">
              {{ validation.error }}
            </span>
          </div>

          <!-- Action buttons -->
          <div class="flex space-x-2">
            <button
              type="button"
              @click="handleCancel"
              class="px-3 py-1.5 text-sm text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded"
            >
              取消
            </button>
            <button
              type="button"
              @click="handleSave"
              :disabled="!validation.isValid"
              class="px-3 py-1.5 text-sm text-white rounded transition-colors"
              :class="{
                'bg-blue-500 hover:bg-blue-600': validation.isValid,
                'bg-gray-300 cursor-not-allowed': !validation.isValid
              }"
            >
              确定
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.script-paragraph {
  @apply mb-4;
}

textarea {
  font-family: inherit;
  line-height: 1.6;
}
</style>
