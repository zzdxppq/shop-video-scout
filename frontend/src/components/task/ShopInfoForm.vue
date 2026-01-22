<script setup lang="ts">
/**
 * Shop information form component.
 * Handles shop name, type, promotion text, and video style inputs with validation.
 */
import { reactive, computed } from 'vue';
import type { ShopType, VideoStyle } from '../../types/task';
import { SHOP_TYPE_LABELS, VIDEO_STYLE_LABELS } from '../../types/task';

interface FormData {
  shopName: string;
  shopType: ShopType | '';
  promotionText: string;
  videoStyle: VideoStyle | '';
}

interface FormErrors {
  shopName: string;
  shopType: string;
  videoStyle: string;
  promotionText: string;
}

const emit = defineEmits<{
  (e: 'submit', data: { shop_name: string; shop_type: ShopType; promotion_text: string; video_style: VideoStyle }): void;
}>();

const formData = reactive<FormData>({
  shopName: '',
  shopType: '',
  promotionText: '',
  videoStyle: ''
});

const errors = reactive<FormErrors>({
  shopName: '',
  shopType: '',
  videoStyle: '',
  promotionText: ''
});

const touched = reactive({
  shopName: false,
  shopType: false,
  videoStyle: false,
  promotionText: false
});

// Constants
const MAX_SHOP_NAME_LENGTH = 200;
const MAX_PROMOTION_TEXT_LENGTH = 500;

const shopTypes: { value: ShopType; label: string }[] = [
  { value: 'food', label: SHOP_TYPE_LABELS.food },
  { value: 'beauty', label: SHOP_TYPE_LABELS.beauty },
  { value: 'entertainment', label: SHOP_TYPE_LABELS.entertainment },
  { value: 'other', label: SHOP_TYPE_LABELS.other }
];

const videoStyles: { value: VideoStyle; label: string }[] = [
  { value: 'recommend', label: VIDEO_STYLE_LABELS.recommend },
  { value: 'review', label: VIDEO_STYLE_LABELS.review },
  { value: 'vlog', label: VIDEO_STYLE_LABELS.vlog }
];

// Validation functions
function validateShopName(): boolean {
  if (!formData.shopName.trim()) {
    errors.shopName = '请输入店铺名称';
    return false;
  }
  if (formData.shopName.length > MAX_SHOP_NAME_LENGTH) {
    errors.shopName = `店铺名称不能超过${MAX_SHOP_NAME_LENGTH}字`;
    return false;
  }
  errors.shopName = '';
  return true;
}

function validateShopType(): boolean {
  if (!formData.shopType) {
    errors.shopType = '请选择店铺类型';
    return false;
  }
  errors.shopType = '';
  return true;
}

function validateVideoStyle(): boolean {
  if (!formData.videoStyle) {
    errors.videoStyle = '请选择视频风格';
    return false;
  }
  errors.videoStyle = '';
  return true;
}

function validatePromotionText(): boolean {
  if (formData.promotionText.length > MAX_PROMOTION_TEXT_LENGTH) {
    errors.promotionText = `优惠描述不能超过${MAX_PROMOTION_TEXT_LENGTH}字`;
    return false;
  }
  errors.promotionText = '';
  return true;
}

// Real-time validation handlers
function handleShopNameInput() {
  touched.shopName = true;
  validateShopName();
}

function handleShopTypeChange() {
  touched.shopType = true;
  validateShopType();
}

function handleVideoStyleChange() {
  touched.videoStyle = true;
  validateVideoStyle();
}

function handlePromotionTextInput() {
  touched.promotionText = true;
  validatePromotionText();
}

// Form validity check
const isFormValid = computed(() => {
  return formData.shopName.trim() !== '' &&
         formData.shopName.length <= MAX_SHOP_NAME_LENGTH &&
         formData.shopType !== '' &&
         formData.videoStyle !== '' &&
         formData.promotionText.length <= MAX_PROMOTION_TEXT_LENGTH;
});

// Character count helpers
const shopNameCount = computed(() => formData.shopName.length);
const promotionTextCount = computed(() => formData.promotionText.length);

// Submit handler
function handleSubmit() {
  touched.shopName = true;
  touched.shopType = true;
  touched.videoStyle = true;
  touched.promotionText = true;

  const isValid = validateShopName() && validateShopType() && validateVideoStyle() && validatePromotionText();

  if (isValid) {
    emit('submit', {
      shop_name: formData.shopName.trim(),
      shop_type: formData.shopType as ShopType,
      promotion_text: formData.promotionText.trim(),
      video_style: formData.videoStyle as VideoStyle
    });
  }
}
</script>

<template>
  <form class="shop-info-form" @submit.prevent="handleSubmit">
    <!-- Shop Name -->
    <div class="form-group" :class="{ 'has-error': touched.shopName && errors.shopName }">
      <label class="form-label">
        店铺名称
        <span class="required">*</span>
      </label>
      <div class="input-wrapper">
        <input
          v-model="formData.shopName"
          type="text"
          class="form-input"
          placeholder="请输入店铺名称"
          :maxlength="MAX_SHOP_NAME_LENGTH"
          @input="handleShopNameInput"
          @blur="handleShopNameInput"
        />
        <span class="char-count" :class="{ 'count-warning': shopNameCount > MAX_SHOP_NAME_LENGTH * 0.9 }">
          {{ shopNameCount }}/{{ MAX_SHOP_NAME_LENGTH }}
        </span>
      </div>
      <span v-if="touched.shopName && errors.shopName" class="error-message">{{ errors.shopName }}</span>
    </div>

    <!-- Shop Type -->
    <div class="form-group" :class="{ 'has-error': touched.shopType && errors.shopType }">
      <label class="form-label">
        店铺类型
        <span class="required">*</span>
      </label>
      <div class="radio-group">
        <label
          v-for="type in shopTypes"
          :key="type.value"
          class="radio-item"
          :class="{ 'radio-selected': formData.shopType === type.value }"
        >
          <input
            v-model="formData.shopType"
            type="radio"
            :value="type.value"
            class="radio-input"
            @change="handleShopTypeChange"
          />
          <span class="radio-label">{{ type.label }}</span>
        </label>
      </div>
      <span v-if="touched.shopType && errors.shopType" class="error-message">{{ errors.shopType }}</span>
    </div>

    <!-- Promotion Text -->
    <div class="form-group" :class="{ 'has-error': touched.promotionText && errors.promotionText }">
      <label class="form-label">
        商品/优惠描述
        <span class="optional">(选填)</span>
      </label>
      <div class="input-wrapper">
        <textarea
          v-model="formData.promotionText"
          class="form-textarea"
          placeholder="例如：人均89，招牌毛肚七上八下"
          :maxlength="MAX_PROMOTION_TEXT_LENGTH"
          rows="3"
          @input="handlePromotionTextInput"
          @blur="handlePromotionTextInput"
        />
        <span class="char-count textarea-count" :class="{ 'count-warning': promotionTextCount > MAX_PROMOTION_TEXT_LENGTH * 0.9 }">
          {{ promotionTextCount }}/{{ MAX_PROMOTION_TEXT_LENGTH }}
        </span>
      </div>
      <span v-if="touched.promotionText && errors.promotionText" class="error-message">{{ errors.promotionText }}</span>
    </div>

    <!-- Video Style -->
    <div class="form-group" :class="{ 'has-error': touched.videoStyle && errors.videoStyle }">
      <label class="form-label">
        视频风格
        <span class="required">*</span>
      </label>
      <div class="radio-group">
        <label
          v-for="style in videoStyles"
          :key="style.value"
          class="radio-item"
          :class="{ 'radio-selected': formData.videoStyle === style.value }"
        >
          <input
            v-model="formData.videoStyle"
            type="radio"
            :value="style.value"
            class="radio-input"
            @change="handleVideoStyleChange"
          />
          <span class="radio-label">{{ style.label }}</span>
        </label>
      </div>
      <span v-if="touched.videoStyle && errors.videoStyle" class="error-message">{{ errors.videoStyle }}</span>
    </div>

    <!-- Submit Button -->
    <div class="form-actions">
      <button
        type="submit"
        class="submit-button"
        :disabled="!isFormValid"
      >
        下一步
      </button>
    </div>
  </form>
</template>

<style scoped>
.shop-info-form {
  max-width: 600px;
  margin: 0 auto;
}

.form-group {
  margin-bottom: 24px;
}

.form-group.has-error .form-input,
.form-group.has-error .form-textarea {
  border-color: #ef4444;
}

.form-label {
  display: block;
  font-weight: 500;
  font-size: 14px;
  color: #374151;
  margin-bottom: 8px;
}

.required {
  color: #ef4444;
  margin-left: 4px;
}

.optional {
  color: #9ca3af;
  font-weight: 400;
  margin-left: 4px;
}

.input-wrapper {
  position: relative;
}

.form-input {
  width: 100%;
  padding: 12px 60px 12px 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-textarea {
  width: 100%;
  padding: 12px 16px 32px 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  min-height: 80px;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.form-textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.char-count {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  color: #9ca3af;
}

.char-count.textarea-count {
  top: auto;
  bottom: 8px;
  transform: none;
}

.char-count.count-warning {
  color: #f59e0b;
}

.error-message {
  display: block;
  color: #ef4444;
  font-size: 12px;
  margin-top: 4px;
}

.radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.radio-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.radio-item:hover {
  border-color: #3b82f6;
}

.radio-item.radio-selected {
  border-color: #3b82f6;
  background-color: #eff6ff;
}

.radio-input {
  display: none;
}

.radio-label {
  font-size: 14px;
  color: #374151;
}

.radio-selected .radio-label {
  color: #3b82f6;
  font-weight: 500;
}

.form-actions {
  margin-top: 32px;
}

.submit-button {
  width: 100%;
  padding: 14px 24px;
  background-color: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.submit-button:hover:not(:disabled) {
  background-color: #2563eb;
}

.submit-button:disabled {
  background-color: #9ca3af;
  cursor: not-allowed;
}
</style>
