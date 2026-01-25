<script setup lang="ts">
/**
 * Script editor main view.
 * Story 3.2: 脚本编辑页面
 *
 * Features:
 * - Display AI-generated script paragraphs (AC1)
 * - Edit individual paragraphs with character count (BR-1.1, BR-1.2)
 * - Save modifications (AC2)
 * - Regenerate script (BR-1.3)
 * - Undo support (Ctrl+Z)
 * - Auto-save draft to localStorage (BR-2.1)
 */
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useScriptStore } from '../stores/script';
import { useScriptEditor } from '../composables/useScriptEditor';
import ScriptParagraph from '../components/script/ScriptParagraph.vue';

const route = useRoute();
const router = useRouter();
const taskId = computed(() => Number(route.params.id));

const store = useScriptStore();
const editor = useScriptEditor(taskId.value);

// Toast message
const toastMessage = ref<string | null>(null);
const toastType = ref<'success' | 'error'>('success');

function showToast(message: string, type: 'success' | 'error' = 'success') {
  toastMessage.value = message;
  toastType.value = type;
  setTimeout(() => {
    toastMessage.value = null;
  }, 3000);
}

// Save handler
async function handleSave() {
  // Commit any pending edit
  if (editor.isEditing.value) {
    editor.commitEdit();
  }

  // Validate all paragraphs
  const validation = editor.validateAll();
  if (!validation.isValid) {
    showToast(validation.errors[0], 'error');
    return;
  }

  const success = await store.save();
  if (success) {
    showToast('保存成功');
  } else {
    showToast(store.error || '保存失败', 'error');
  }
}

// Regenerate handler
const regenerateConfirmVisible = ref(false);

async function handleRegenerate() {
  regenerateConfirmVisible.value = false;

  const success = await store.regenerate();
  if (success) {
    showToast('脚本已重新生成');
  } else {
    showToast(store.error || '重新生成失败', 'error');
  }
}

// Navigation
function handleBack() {
  if (store.hasChanges) {
    if (confirm('您有未保存的修改，确定要离开吗？')) {
      router.back();
    }
  } else {
    router.back();
  }
}

function handleNext() {
  // Proceed to next step (TTS settings)
  router.push(`/task/${taskId.value}/voice`);
}
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Header -->
    <header class="bg-white border-b sticky top-0 z-10">
      <div class="max-w-4xl mx-auto px-4 py-4 flex items-center justify-between">
        <button
          @click="handleBack"
          class="flex items-center text-gray-600 hover:text-gray-800"
        >
          <svg class="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
          返回
        </button>

        <h1 class="text-lg font-semibold">编辑脚本</h1>

        <div class="flex items-center space-x-3">
          <button
            @click="handleSave"
            :disabled="!store.hasChanges || store.isSaving"
            class="px-4 py-2 text-sm rounded-lg transition-colors"
            :class="{
              'bg-blue-500 text-white hover:bg-blue-600': store.hasChanges && !store.isSaving,
              'bg-gray-200 text-gray-400 cursor-not-allowed': !store.hasChanges || store.isSaving
            }"
          >
            {{ store.isSaving ? '保存中...' : '保存' }}
          </button>

          <button
            @click="handleNext"
            class="px-4 py-2 text-sm bg-green-500 text-white rounded-lg hover:bg-green-600"
          >
            下一步
          </button>
        </div>
      </div>
    </header>

    <!-- Main content -->
    <main class="max-w-4xl mx-auto px-4 py-6">
      <!-- Loading state -->
      <div v-if="store.isLoading" class="flex flex-col items-center justify-center py-20">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
        <p class="mt-4 text-gray-500">加载脚本中...</p>
      </div>

      <!-- Error state -->
      <div v-else-if="store.error && !store.script" class="text-center py-20">
        <div class="text-red-500 text-lg mb-4">{{ store.error }}</div>
        <button
          @click="store.fetchScript(taskId)"
          class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600"
        >
          重试
        </button>
      </div>

      <!-- Script content -->
      <div v-else-if="store.script" class="space-y-6">
        <!-- Regenerate section -->
        <div class="bg-white rounded-lg border p-4 flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-600">
              不满意当前脚本？可以让AI重新生成
            </p>
            <p class="text-xs text-gray-400 mt-1">
              剩余次数: {{ store.regenerateRemaining }}
            </p>
          </div>
          <button
            @click="regenerateConfirmVisible = true"
            :disabled="!store.canRegenerate || store.isRegenerating"
            class="px-4 py-2 text-sm rounded-lg transition-colors"
            :class="{
              'bg-purple-500 text-white hover:bg-purple-600': store.canRegenerate && !store.isRegenerating,
              'bg-gray-200 text-gray-400 cursor-not-allowed': !store.canRegenerate || store.isRegenerating
            }"
          >
            {{ store.isRegenerating ? '生成中...' : '重新生成' }}
          </button>
        </div>

        <!-- Undo hint -->
        <div v-if="store.undoStack.length > 0" class="text-sm text-gray-500 text-center">
          按 Ctrl+Z 撤销 ({{ store.undoStack.length }})
        </div>

        <!-- Paragraphs -->
        <div class="space-y-4">
          <ScriptParagraph
            v-for="paragraph in store.paragraphs"
            :key="paragraph.id"
            :paragraph="paragraph"
            :currentText="store.getParagraphText(paragraph.id)"
            :isEdited="store.isParagraphEdited(paragraph.id)"
            :isEditing="editor.editingParagraphId.value === paragraph.id"
            :editText="editor.editText.value"
            :charCountInfo="editor.getCharCountInfo(paragraph.id)"
            @startEdit="editor.startEdit(paragraph.id)"
            @updateText="editor.updateEditText"
            @commitEdit="editor.commitEdit"
            @cancelEdit="editor.cancelEdit"
          />
        </div>

        <!-- Version info -->
        <div class="text-center text-xs text-gray-400 pt-4 border-t">
          版本 {{ store.version }} | 上次更新 {{ store.script?.updatedAt }}
        </div>
      </div>
    </main>

    <!-- Toast -->
    <Transition name="toast">
      <div
        v-if="toastMessage"
        class="fixed bottom-6 left-1/2 transform -translate-x-1/2 px-6 py-3 rounded-lg shadow-lg z-50"
        :class="{
          'bg-green-500 text-white': toastType === 'success',
          'bg-red-500 text-white': toastType === 'error'
        }"
      >
        {{ toastMessage }}
      </div>
    </Transition>

    <!-- Regenerate confirm dialog -->
    <Transition name="modal">
      <div
        v-if="regenerateConfirmVisible"
        class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
        @click.self="regenerateConfirmVisible = false"
      >
        <div class="bg-white rounded-lg p-6 max-w-sm mx-4">
          <h3 class="text-lg font-semibold mb-2">确认重新生成？</h3>
          <p class="text-gray-600 text-sm mb-4">
            重新生成将替换当前脚本内容，未保存的修改将丢失。
          </p>
          <div class="flex justify-end space-x-3">
            <button
              @click="regenerateConfirmVisible = false"
              class="px-4 py-2 text-sm text-gray-600 hover:text-gray-800"
            >
              取消
            </button>
            <button
              @click="handleRegenerate"
              class="px-4 py-2 text-sm bg-purple-500 text-white rounded-lg hover:bg-purple-600"
            >
              确认重新生成
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, 20px);
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
