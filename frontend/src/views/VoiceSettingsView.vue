<script setup lang="ts">
/**
 * Voice settings page.
 * Story 4.4: 配音设置页面
 * Story 4.5: 字幕设置页面 (Subtitle Settings Integration)
 *
 * Features:
 * - Voice selection with preview (AC1)
 * - Start compose with confirmation (AC2)
 * - Subtitle toggle and style selection (4.5 AC1, AC2)
 */
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useVoiceSettingsStore } from '../stores/voiceSettings';
import { getVoiceSamples, setVoiceType } from '../api/voice';
import { startCompose } from '../api/compose';
import VoiceList from '../components/voice/VoiceList.vue';
import ComposeConfirmModal from '../components/voice/ComposeConfirmModal.vue';
import SubtitleSettings from '../components/subtitle/SubtitleSettings.vue';
import type { VoiceType } from '../types/voice';

const route = useRoute();
const router = useRouter();
const taskId = computed(() => Number(route.params.id));

const store = useVoiceSettingsStore();

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

// Confirm modal
const showConfirmModal = ref(false);
const paragraphCount = ref(0);
const estimatedDuration = ref('');

/**
 * Load voice samples on mount.
 */
async function loadVoiceSamples() {
  store.setLoading(true);
  store.setError(null);

  try {
    const response = await getVoiceSamples();
    if (response.code === 0 || response.code === 200) {
      store.setSamples(response.data.samples, response.data.cloned_voices);
    } else {
      store.setError(response.message || '加载音色失败');
    }
  } catch (err) {
    store.setError(err instanceof Error ? err.message : '加载音色失败');
  } finally {
    store.setLoading(false);
  }
}

/**
 * Handle voice selection.
 * BR-1.4: 选中音色高亮显示
 */
function handleVoiceSelect(voiceType: VoiceType, voiceId?: string) {
  store.selectVoice(voiceType, voiceId);
}

/**
 * Open compose confirmation modal.
 * BR-2.1: 确认弹窗显示已选音色名称、脚本段落数、预计生成时长
 */
function handleComposeClick() {
  // TODO: Get actual paragraph count from task/script
  paragraphCount.value = 5;
  // BR-2.2: 预计时长计算：脚本总字数 × 0.3秒/字
  const estimatedSeconds = paragraphCount.value * 50 * 0.3; // Assume ~50 chars per paragraph
  const minutes = Math.ceil(estimatedSeconds / 60);
  estimatedDuration.value = `约${minutes}分钟`;

  showConfirmModal.value = true;
}

/**
 * Handle compose confirmation.
 * BR-2.3: 确认后调用 PUT /voice-type 保存音色，再调用 POST /compose
 */
async function handleConfirmCompose() {
  showConfirmModal.value = false;
  store.setSaving(true);

  try {
    // Step 1: Save voice type
    const voiceResponse = await setVoiceType(
      taskId.value,
      store.selectedVoiceType,
      store.selectedVoiceId || undefined
    );

    if (voiceResponse.code !== 0 && voiceResponse.code !== 200) {
      throw new Error(voiceResponse.message || '保存音色失败');
    }

    // Step 2: Start compose
    const composeResponse = await startCompose(taskId.value);

    if (composeResponse.code === 409) {
      // Already composing - redirect to progress
      router.push(`/task/${taskId.value}/progress`);
      return;
    }

    if (composeResponse.code !== 0 && composeResponse.code !== 200 && composeResponse.code !== 202) {
      throw new Error(composeResponse.message || '启动合成失败');
    }

    // BR-2.4: 合成开始后跳转到进度页面
    router.push(`/task/${taskId.value}/progress`);
  } catch (err) {
    showToast(err instanceof Error ? err.message : '操作失败', 'error');
  } finally {
    store.setSaving(false);
  }
}

/**
 * Handle cancel confirmation.
 */
function handleCancelConfirm() {
  showConfirmModal.value = false;
}

/**
 * Navigate back to script editor.
 */
function handleBack() {
  router.push(`/task/${taskId.value}/script`);
}

/**
 * Handle subtitle settings error.
 * Story 4.5: Display toast on subtitle save error
 */
function handleSubtitleError(message: string) {
  showToast(message, 'error');
}

onMounted(() => {
  loadVoiceSamples();
});
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

        <h1 class="text-lg font-semibold">选择配音</h1>

        <button
          @click="handleComposeClick"
          :disabled="store.isLoading || store.isSaving || !store.selectedVoice"
          class="px-4 py-2 text-sm rounded-lg transition-colors"
          :class="{
            'bg-green-500 text-white hover:bg-green-600': !store.isLoading && !store.isSaving && store.selectedVoice,
            'bg-gray-200 text-gray-400 cursor-not-allowed': store.isLoading || store.isSaving || !store.selectedVoice
          }"
        >
          {{ store.isSaving ? '处理中...' : '开始合成' }}
        </button>
      </div>
    </header>

    <!-- Main content -->
    <main class="max-w-4xl mx-auto px-4 py-6">
      <!-- Loading state -->
      <div v-if="store.isLoading" class="flex flex-col items-center justify-center py-20">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
        <p class="mt-4 text-gray-500">加载音色中...</p>
      </div>

      <!-- Error state -->
      <div v-else-if="store.error" class="text-center py-20">
        <div class="text-red-500 text-lg mb-4">{{ store.error }}</div>
        <button
          @click="loadVoiceSamples"
          class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600"
        >
          重试
        </button>
      </div>

      <!-- Voice list -->
      <div v-else class="space-y-6">
        <!-- Instructions -->
        <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <p class="text-blue-800 text-sm">
            选择您喜欢的配音风格，点击播放按钮可试听效果
          </p>
        </div>

        <!-- Voice selection grid -->
        <VoiceList
          :samples="store.allVoices"
          :selectedVoiceType="store.selectedVoiceType"
          :selectedVoiceId="store.selectedVoiceId"
          @select="handleVoiceSelect"
        />

        <!-- Cloned voice hint -->
        <div v-if="!store.hasClonedVoices" class="text-center text-gray-500 text-sm">
          <p>想使用自己的声音？可在个人中心上传声音样本</p>
        </div>

        <!-- Subtitle Settings - Story 4.5 -->
        <SubtitleSettings
          :task-id="taskId"
          :initial-enabled="true"
          :initial-style="'simple_white'"
          @error="handleSubtitleError"
        />
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

    <!-- Compose confirm modal -->
    <ComposeConfirmModal
      :visible="showConfirmModal"
      :voiceName="store.selectedVoice?.name || ''"
      :paragraphCount="paragraphCount"
      :estimatedDuration="estimatedDuration"
      @confirm="handleConfirmCompose"
      @cancel="handleCancelConfirm"
    />
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
</style>
