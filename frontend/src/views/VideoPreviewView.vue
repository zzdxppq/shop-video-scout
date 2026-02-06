<script setup lang="ts">
/**
 * Video preview page.
 * Story 5.1: 视频预览播放器
 * Story 5.2: 下载导出功能
 * Story 5.4: 发布辅助UI组件
 *
 * Features:
 * - Video player with controls
 * - Shot list display
 * - Download video and assets pack (5.2)
 * - Navigation to regenerate
 * - Task status validation
 */
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getTaskOutput } from '../api/output';
import { getTask } from '../api/task';
import VideoPlayer from '../components/preview/VideoPlayer.vue';
import ShotList from '../components/preview/ShotList.vue';
import DownloadButtons from '../components/preview/DownloadButtons.vue';
import PublishAssistPanel from '../components/publish/PublishAssistPanel.vue';
import type { TaskOutput, VideoShot } from '../types/output';

const route = useRoute();
const router = useRouter();

const taskId = computed(() => Number(route.params.id));

// State
const isLoading = ref(true);
const error = ref<string | null>(null);
const taskOutput = ref<TaskOutput | null>(null);
const shots = ref<VideoShot[]>([]);
const shopName = ref('');

// Computed: Check if there are recommended shots for assets pack
const hasRecommendedShots = computed(() => shots.value.some(shot => shot.is_recommended));

/**
 * Fetch task and output data.
 */
async function fetchData() {
  isLoading.value = true;
  error.value = null;

  try {
    // Fetch task details first to check status
    const taskResponse = await getTask(taskId.value);

    if (taskResponse.code !== 0 && taskResponse.code !== 200) {
      throw new Error(taskResponse.message || '获取任务信息失败');
    }

    const task = taskResponse.data;

    // Check task status
    if (task.status !== 'done') {
      // Redirect to progress page if not done
      router.replace(`/task/${taskId.value}/progress`);
      return;
    }

    shopName.value = task.shop_name || '';

    // Map videos to shots format
    if (task.videos && Array.isArray(task.videos)) {
      shots.value = task.videos.map((v: any) => ({
        id: v.id,
        thumbnail_url: v.thumbnail_url,
        category: v.category || '未分类',
        duration_seconds: v.duration_seconds || 0,
        is_recommended: v.is_recommended || false,
        sequence_in_output: v.sequence_in_output || 0
      }));
    }

    // Fetch output URL
    const outputResponse = await getTaskOutput(taskId.value);

    if (outputResponse.code !== 0 && outputResponse.code !== 200) {
      throw new Error(outputResponse.message || '获取视频信息失败');
    }

    taskOutput.value = outputResponse.data;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载失败';
  } finally {
    isLoading.value = false;
  }
}

/**
 * Handle retry on error.
 */
function handleRetry() {
  fetchData();
}

/**
 * Navigate back to voice settings for regeneration.
 */
function handleRegenerate() {
  router.push(`/task/${taskId.value}/voice`);
}

onMounted(() => {
  fetchData();
});
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Header -->
    <header class="bg-white border-b sticky top-0 z-10">
      <div class="max-w-4xl mx-auto px-4 py-4">
        <h1 class="text-lg font-semibold text-center">视频预览</h1>
        <p v-if="shopName" class="text-sm text-gray-500 text-center mt-1">{{ shopName }}</p>
      </div>
    </header>

    <!-- Main content -->
    <main class="max-w-4xl mx-auto px-4 py-6">
      <!-- Loading state -->
      <div v-if="isLoading" class="flex flex-col items-center justify-center py-20">
        <div class="animate-spin rounded-full h-12 w-12 border-4 border-blue-500 border-t-transparent" />
        <p class="mt-4 text-gray-500">加载中...</p>
      </div>

      <!-- Error state -->
      <div v-else-if="error" class="text-center py-20">
        <div class="w-20 h-20 mx-auto mb-6 rounded-full bg-red-100 flex items-center justify-center">
          <svg class="w-10 h-10 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </div>
        <h2 class="text-xl font-semibold text-gray-800 mb-2">加载失败</h2>
        <p class="text-gray-500 mb-6">{{ error }}</p>
        <button
          class="px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
          @click="handleRetry"
        >
          重试
        </button>
      </div>

      <!-- Content -->
      <div v-else-if="taskOutput" class="space-y-6">
        <!-- Video Player -->
        <section class="flex justify-center">
          <VideoPlayer
            :src="taskOutput.output_url"
          />
        </section>

        <!-- Video info -->
        <section class="text-center text-sm text-gray-500">
          <span>{{ Math.round((taskOutput.file_size_bytes || 0) / 1024 / 1024 * 10) / 10 }} MB</span>
          <span class="mx-2">·</span>
          <span>{{ taskOutput.duration_seconds || 0 }}秒</span>
        </section>

        <!-- Shot List -->
        <section v-if="shots.length > 0">
          <ShotList :shots="shots" />
        </section>

        <!-- Download buttons (Story 5.2) -->
        <section class="pt-6">
          <DownloadButtons
            :task-id="taskId"
            :has-recommended-shots="hasRecommendedShots"
          />
        </section>

        <!-- Publish Assist Panel (Story 5.4) -->
        <section class="pt-6">
          <PublishAssistPanel :task-id="taskId" />
        </section>

        <!-- Regenerate button -->
        <section class="pt-3">
          <button
            class="w-full py-3 bg-white border border-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-50 transition-colors"
            @click="handleRegenerate"
          >
            重新生成视频
          </button>
        </section>
      </div>
    </main>
  </div>
</template>
