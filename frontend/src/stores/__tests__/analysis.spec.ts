/**
 * Analysis store tests.
 * Story 2.4: 分析进度展示页面
 *
 * Test coverage:
 * - 2.4-UNIT-Store-001: Initial state
 * - 2.4-UNIT-Store-002: setProgress updates state
 * - 2.4-UNIT-Store-003: setFrames updates state
 * - 2.4-UNIT-Store-004: Tab filtering logic
 * - 2.4-UNIT-Store-005: Tabs computed with counts
 */
import { describe, it, expect, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useAnalysisStore } from '../analysis';
import type { AnalysisProgress, VideoFrame } from '../../types/analysis';

/**
 * Helper to create mock progress.
 */
function createMockProgress(overrides: Partial<AnalysisProgress> = {}): AnalysisProgress {
  return {
    percent: 50,
    currentVideo: 5,
    totalVideos: 10,
    stage: 'content_recognition',
    stages: [
      { id: 'upload', label: '上传完成', status: 'completed' },
      { id: 'content_recognition', label: '镜头内容识别', status: 'in_progress' }
    ],
    estimatedTimeRemaining: 60,
    status: 'analyzing',
    ...overrides
  };
}

/**
 * Helper to create mock frame.
 */
function createMockFrame(overrides: Partial<VideoFrame> = {}): VideoFrame {
  return {
    id: Math.floor(Math.random() * 1000),
    videoId: 1,
    thumbnailUrl: 'https://cdn.example.com/thumb.jpg',
    category: 'food',
    tags: ['美食'],
    qualityScore: 80,
    isRecommended: false,
    ...overrides
  };
}

describe('useAnalysisStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  describe('initial state', () => {
    it('should have correct initial values', () => {
      const store = useAnalysisStore();

      expect(store.progress).toBeNull();
      expect(store.frames).toEqual([]);
      expect(store.isLoading).toBe(false);
      expect(store.error).toBeNull();
      expect(store.activeTab).toBe('all');
      expect(store.pollErrorCount).toBe(0);
    });

    it('should have correct initial computed values', () => {
      const store = useAnalysisStore();

      expect(store.status).toBeNull();
      expect(store.isAnalyzing).toBe(false);
      expect(store.isCompleted).toBe(false);
      expect(store.isFailed).toBe(false);
      expect(store.percent).toBe(0);
      expect(store.currentVideo).toBe(0);
      expect(store.totalVideos).toBe(0);
    });
  });

  describe('setProgress', () => {
    it('should update progress state', () => {
      const store = useAnalysisStore();
      const progress = createMockProgress({ percent: 75 });

      store.setProgress(progress);

      expect(store.progress).toEqual(progress);
      expect(store.percent).toBe(75);
    });

    it('should update computed status values', () => {
      const store = useAnalysisStore();

      store.setProgress(createMockProgress({ status: 'analyzing' }));
      expect(store.isAnalyzing).toBe(true);
      expect(store.isCompleted).toBe(false);

      store.setProgress(createMockProgress({ status: 'completed' }));
      expect(store.isAnalyzing).toBe(false);
      expect(store.isCompleted).toBe(true);
    });

    it('should set error from progress data', () => {
      const store = useAnalysisStore();

      store.setProgress(createMockProgress({
        status: 'failed',
        error: '分析失败'
      }));

      expect(store.error).toBe('分析失败');
    });
  });

  describe('setFrames', () => {
    it('should update frames state', () => {
      const store = useAnalysisStore();
      const frames = [createMockFrame(), createMockFrame()];

      store.setFrames(frames);

      expect(store.frames).toEqual(frames);
    });
  });

  describe('filteredFrames', () => {
    it('should return all frames when activeTab is "all"', () => {
      const store = useAnalysisStore();
      const frames = [
        createMockFrame({ category: 'food' }),
        createMockFrame({ category: 'person' }),
        createMockFrame({ category: 'environment' })
      ];
      store.setFrames(frames);
      store.setActiveTab('all');

      expect(store.filteredFrames).toHaveLength(3);
    });

    it('should filter frames by category', () => {
      const store = useAnalysisStore();
      const frames = [
        createMockFrame({ category: 'food' }),
        createMockFrame({ category: 'food' }),
        createMockFrame({ category: 'person' }),
        createMockFrame({ category: 'environment' })
      ];
      store.setFrames(frames);

      store.setActiveTab('food');
      expect(store.filteredFrames).toHaveLength(2);

      store.setActiveTab('person');
      expect(store.filteredFrames).toHaveLength(1);

      store.setActiveTab('other');
      expect(store.filteredFrames).toHaveLength(0);
    });
  });

  describe('tabs computed', () => {
    it('should generate tabs with correct counts', () => {
      const store = useAnalysisStore();
      const frames = [
        createMockFrame({ category: 'food' }),
        createMockFrame({ category: 'food' }),
        createMockFrame({ category: 'person' }),
        createMockFrame({ category: 'environment' })
      ];
      store.setFrames(frames);

      expect(store.tabs).toHaveLength(5);
      expect(store.tabs[0]).toEqual({ key: 'all', label: '全部', count: 4 });
      expect(store.tabs[1]).toEqual({ key: 'food', label: '食物', count: 2 });
      expect(store.tabs[2]).toEqual({ key: 'person', label: '人物', count: 1 });
      expect(store.tabs[3]).toEqual({ key: 'environment', label: '环境', count: 1 });
      expect(store.tabs[4]).toEqual({ key: 'other', label: '其他', count: 0 });
    });
  });

  describe('error handling', () => {
    it('should track poll error count', () => {
      const store = useAnalysisStore();

      expect(store.pollErrorCount).toBe(0);
      store.incrementPollErrorCount();
      expect(store.pollErrorCount).toBe(1);
      store.incrementPollErrorCount();
      expect(store.pollErrorCount).toBe(2);
    });

    it('should reset poll error count', () => {
      const store = useAnalysisStore();
      store.incrementPollErrorCount();
      store.incrementPollErrorCount();

      store.resetPollErrorCount();

      expect(store.pollErrorCount).toBe(0);
    });
  });

  describe('reset', () => {
    it('should reset all state to initial values', () => {
      const store = useAnalysisStore();

      // Set some state
      store.setProgress(createMockProgress());
      store.setFrames([createMockFrame()]);
      store.setLoading(true);
      store.setError('error');
      store.setActiveTab('food');
      store.incrementPollErrorCount();

      // Reset
      store.reset();

      // Verify reset
      expect(store.progress).toBeNull();
      expect(store.frames).toEqual([]);
      expect(store.isLoading).toBe(false);
      expect(store.error).toBeNull();
      expect(store.activeTab).toBe('all');
      expect(store.pollErrorCount).toBe(0);
    });
  });
});
