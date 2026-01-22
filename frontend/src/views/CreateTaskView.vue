<script setup lang="ts">
/**
 * Create Task page - Step 1: Fill in shop information.
 * After successful task creation, redirects to video upload page.
 */
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import StepIndicator from '../components/task/StepIndicator.vue';
import ShopInfoForm from '../components/task/ShopInfoForm.vue';
import { createTask } from '../api/task';
import type { ShopType, VideoStyle } from '../types/task';

const router = useRouter();

const steps = [
  { label: '填写信息', description: '店铺基本信息' },
  { label: '上传视频', description: '素材视频' },
  { label: 'AI分析', description: '智能分析' },
  { label: '编辑脚本', description: '口播文案' },
  { label: '预览导出', description: '生成视频' }
];

const currentStep = ref(0);
const isSubmitting = ref(false);
const errorMessage = ref('');

interface FormSubmitData {
  shop_name: string;
  shop_type: ShopType;
  promotion_text: string;
  video_style: VideoStyle;
}

async function handleFormSubmit(data: FormSubmitData) {
  if (isSubmitting.value) return;

  isSubmitting.value = true;
  errorMessage.value = '';

  try {
    const response = await createTask(data);
    if (response.code === 200 && response.data) {
      // Navigate to upload page with task ID
      router.push(`/task/${response.data.id}/upload`);
    } else {
      errorMessage.value = response.message || '创建任务失败，请重试';
    }
  } catch (error) {
    if (error instanceof Error) {
      // Handle rate limit error (429)
      if (error.message.includes('太多进行中的任务')) {
        errorMessage.value = error.message;
      } else {
        errorMessage.value = error.message || '创建任务失败，请重试';
      }
    } else {
      errorMessage.value = '网络错误，请检查网络连接';
    }
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <div class="create-task-page">
    <div class="page-header">
      <h1 class="page-title">创建任务</h1>
      <p class="page-subtitle">填写店铺信息，开始视频创作</p>
    </div>

    <StepIndicator :steps="steps" :current-step="currentStep" />

    <div class="page-content">
      <div class="form-card">
        <div class="card-header">
          <h2 class="card-title">店铺信息</h2>
          <p class="card-description">请填写店铺的基本信息，AI将根据这些信息生成视频脚本</p>
        </div>

        <div v-if="errorMessage" class="error-banner">
          <span class="error-icon">⚠️</span>
          <span>{{ errorMessage }}</span>
        </div>

        <ShopInfoForm @submit="handleFormSubmit" />

        <div v-if="isSubmitting" class="loading-overlay">
          <div class="loading-spinner"></div>
          <span>正在创建任务...</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.create-task-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
}

.page-content {
  margin-top: 32px;
}

.form-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 32px;
  position: relative;
}

.card-header {
  margin-bottom: 24px;
}

.card-title {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 8px 0;
}

.card-description {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background-color: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 14px;
  margin-bottom: 24px;
}

.error-icon {
  flex-shrink: 0;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  border-radius: 12px;
  z-index: 10;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e5e7eb;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
