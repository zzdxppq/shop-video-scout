<script setup lang="ts">
/**
 * Step indicator component showing progress through task creation flow.
 */

interface Step {
  label: string;
  description?: string;
}

interface Props {
  steps: Step[];
  currentStep: number;
}

defineProps<Props>();
</script>

<template>
  <div class="step-indicator">
    <div
      v-for="(step, index) in steps"
      :key="index"
      class="step-item"
      :class="{
        'step-active': index === currentStep,
        'step-completed': index < currentStep,
        'step-pending': index > currentStep
      }"
    >
      <div class="step-number">
        <span v-if="index < currentStep" class="step-check">✓</span>
        <span v-else>{{ index + 1 }}</span>
      </div>
      <div class="step-content">
        <div class="step-label">{{ step.label }}</div>
        <div v-if="step.description" class="step-description">{{ step.description }}</div>
      </div>
      <div v-if="index < steps.length - 1" class="step-connector" />
    </div>
  </div>
</template>

<style scoped>
.step-indicator {
  display: flex;
  align-items: flex-start;
  padding: 16px 0;
}

.step-item {
  display: flex;
  align-items: center;
  flex: 1;
  position: relative;
}

.step-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.step-pending .step-number {
  background-color: #e5e7eb;
  color: #9ca3af;
}

.step-active .step-number {
  background-color: #3b82f6;
  color: white;
}

.step-completed .step-number {
  background-color: #10b981;
  color: white;
}

.step-check {
  font-size: 16px;
}

.step-content {
  margin-left: 12px;
  flex-shrink: 0;
}

.step-label {
  font-weight: 500;
  font-size: 14px;
  color: #374151;
}

.step-pending .step-label {
  color: #9ca3af;
}

.step-description {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.step-connector {
  flex: 1;
  height: 2px;
  background-color: #e5e7eb;
  margin: 0 16px;
}

.step-completed + .step-item .step-connector,
.step-completed .step-connector {
  background-color: #10b981;
}
</style>
