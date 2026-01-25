/**
 * Analysis store for managing analysis state.
 * Story 2.4: 分析进度展示页面
 */
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type {
  AnalysisProgress,
  VideoFrame,
  AnalysisStatus,
  TabCategory,
  FrameTab
} from '../types/analysis';
import { CATEGORY_LABELS, DEFAULT_ANALYSIS_STAGES } from '../types/analysis';

export const useAnalysisStore = defineStore('analysis', () => {
  // State
  const progress = ref<AnalysisProgress | null>(null);
  const frames = ref<VideoFrame[]>([]);
  const isLoading = ref(false);
  const error = ref<string | null>(null);
  const activeTab = ref<TabCategory>('all');
  const pollErrorCount = ref(0);

  // Getters
  const status = computed<AnalysisStatus | null>(() => progress.value?.status ?? null);

  const isAnalyzing = computed(() => status.value === 'analyzing');

  const isCompleted = computed(() => status.value === 'completed');

  const isFailed = computed(() => status.value === 'failed');

  const percent = computed(() => progress.value?.percent ?? 0);

  const currentVideo = computed(() => progress.value?.currentVideo ?? 0);

  const totalVideos = computed(() => progress.value?.totalVideos ?? 0);

  const stages = computed(() => progress.value?.stages ?? DEFAULT_ANALYSIS_STAGES);

  const estimatedTimeRemaining = computed(() => progress.value?.estimatedTimeRemaining ?? 0);

  /**
   * Filter frames by active tab category.
   */
  const filteredFrames = computed(() => {
    if (activeTab.value === 'all') {
      return frames.value;
    }
    return frames.value.filter(f => f.category === activeTab.value);
  });

  /**
   * Generate tabs with counts (BR-2.4).
   */
  const tabs = computed<FrameTab[]>(() => {
    const categories: TabCategory[] = ['all', 'food', 'person', 'environment', 'other'];
    return categories.map(key => ({
      key,
      label: CATEGORY_LABELS[key],
      count: key === 'all'
        ? frames.value.length
        : frames.value.filter(f => f.category === key).length
    }));
  });

  // Actions
  function setProgress(data: AnalysisProgress) {
    progress.value = data;
    error.value = data.error ?? null;
  }

  function setFrames(data: VideoFrame[]) {
    frames.value = data;
  }

  function setLoading(loading: boolean) {
    isLoading.value = loading;
  }

  function setError(err: string | null) {
    error.value = err;
  }

  function setActiveTab(tab: TabCategory) {
    activeTab.value = tab;
  }

  function incrementPollErrorCount() {
    pollErrorCount.value++;
  }

  function resetPollErrorCount() {
    pollErrorCount.value = 0;
  }

  function reset() {
    progress.value = null;
    frames.value = [];
    isLoading.value = false;
    error.value = null;
    activeTab.value = 'all';
    pollErrorCount.value = 0;
  }

  return {
    // State
    progress,
    frames,
    isLoading,
    error,
    activeTab,
    pollErrorCount,

    // Getters
    status,
    isAnalyzing,
    isCompleted,
    isFailed,
    percent,
    currentVideo,
    totalVideos,
    stages,
    estimatedTimeRemaining,
    filteredFrames,
    tabs,

    // Actions
    setProgress,
    setFrames,
    setLoading,
    setError,
    setActiveTab,
    incrementPollErrorCount,
    resetPollErrorCount,
    reset
  };
});
