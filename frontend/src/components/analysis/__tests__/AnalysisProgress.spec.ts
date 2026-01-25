/**
 * AnalysisProgress component tests.
 * Story 2.4: 分析进度展示页面 - AC1
 *
 * Test coverage:
 * - 2.4-UNIT-001: Progress bar renders correct percentage
 * - 2.4-UNIT-002: Video count display
 * - 2.4-UNIT-003: Estimated time remaining
 * - 2.4-UNIT-004: Stage list with status markers
 * - 2.4-UNIT-005: Progress bar animation (CSS)
 * - 2.4-UNIT-006: Completion animation
 * - 2.4-BLIND-BOUNDARY-001: Progress at 0%
 * - 2.4-BLIND-BOUNDARY-002: Progress at 100%
 * - 2.4-BLIND-ERROR-001: Analysis failure display
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import AnalysisProgress from '../AnalysisProgress.vue';
import type { AnalysisProgress as ProgressData, AnalysisStage } from '../../../types/analysis';

/**
 * Helper to create mock progress data.
 */
function createMockProgress(overrides: Partial<ProgressData> = {}): ProgressData {
  const defaultStages: AnalysisStage[] = [
    { id: 'upload', label: '上传完成', status: 'completed' },
    { id: 'content_recognition', label: '镜头内容识别', status: 'completed' },
    { id: 'quality_assessment', label: '画面质量评估', status: 'in_progress' },
    { id: 'shot_marking', label: '推荐镜头标记', status: 'pending' },
    { id: 'script_generation', label: '脚本生成', status: 'pending' }
  ];

  return {
    percent: 65,
    currentVideo: 13,
    totalVideos: 20,
    stage: 'quality_assessment',
    stages: defaultStages,
    estimatedTimeRemaining: 60,
    status: 'analyzing',
    ...overrides
  };
}

describe('AnalysisProgress', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // 2.4-UNIT-001: Progress bar renders correct percentage (0-100)
  describe('progress bar rendering', () => {
    it('should render progress bar with correct percentage', () => {
      const progress = createMockProgress({ percent: 65 });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      const progressBar = wrapper.find('.progress-bar-fill');
      expect(progressBar.exists()).toBe(true);
      expect(progressBar.attributes('style')).toContain('width: 65%');
    });

    it('should display percentage text', () => {
      const progress = createMockProgress({ percent: 75 });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      expect(wrapper.text()).toContain('75%');
    });

    // 2.4-BLIND-BOUNDARY-001: Progress at 0%
    it('should handle 0% progress (boundary)', () => {
      const progress = createMockProgress({
        percent: 0,
        stages: [
          { id: 'upload', label: '上传完成', status: 'completed' },
          { id: 'content_recognition', label: '镜头内容识别', status: 'pending' },
          { id: 'quality_assessment', label: '画面质量评估', status: 'pending' },
          { id: 'shot_marking', label: '推荐镜头标记', status: 'pending' },
          { id: 'script_generation', label: '脚本生成', status: 'pending' }
        ]
      });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      const progressBar = wrapper.find('.progress-bar-fill');
      expect(progressBar.attributes('style')).toContain('width: 0%');
    });

    // 2.4-BLIND-BOUNDARY-002: Progress at 100%
    it('should handle 100% progress (boundary)', () => {
      // Test with analyzing status to show progress bar at 100%
      const progress = createMockProgress({
        percent: 100,
        status: 'analyzing', // Keep analyzing to see progress bar at 100%
        stages: [
          { id: 'upload', label: '上传完成', status: 'completed' },
          { id: 'content_recognition', label: '镜头内容识别', status: 'completed' },
          { id: 'quality_assessment', label: '画面质量评估', status: 'completed' },
          { id: 'shot_marking', label: '推荐镜头标记', status: 'completed' },
          { id: 'script_generation', label: '脚本生成', status: 'in_progress' }
        ]
      });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      const progressBar = wrapper.find('.progress-bar-fill');
      expect(progressBar.attributes('style')).toContain('width: 100%');
    });

    it('should show completion state when status is completed at 100%', () => {
      const progress = createMockProgress({
        percent: 100,
        status: 'completed',
        stages: [
          { id: 'upload', label: '上传完成', status: 'completed' },
          { id: 'content_recognition', label: '镜头内容识别', status: 'completed' },
          { id: 'quality_assessment', label: '画面质量评估', status: 'completed' },
          { id: 'shot_marking', label: '推荐镜头标记', status: 'completed' },
          { id: 'script_generation', label: '脚本生成', status: 'completed' }
        ]
      });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      // Should show completion state, not progress bar
      expect(wrapper.find('.completion-state').exists()).toBe(true);
      expect(wrapper.find('.progress-bar-fill').exists()).toBe(false);
    });
  });

  // 2.4-UNIT-002: Video count display "已分析 X/Y 个视频"
  describe('video count display', () => {
    it('should display video progress text', () => {
      const progress = createMockProgress({
        currentVideo: 13,
        totalVideos: 20
      });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      expect(wrapper.text()).toContain('已分析 13/20 个视频');
    });

    it('should handle zero videos', () => {
      const progress = createMockProgress({
        currentVideo: 0,
        totalVideos: 5
      });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      expect(wrapper.text()).toContain('已分析 0/5 个视频');
    });
  });

  // 2.4-UNIT-003: Estimated time remaining display
  describe('estimated time remaining', () => {
    it('should display estimated time in minutes and seconds', () => {
      const progress = createMockProgress({ estimatedTimeRemaining: 125 });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      expect(wrapper.text()).toContain('预计剩余');
      expect(wrapper.text()).toMatch(/2.*分.*5.*秒|2:05/);
    });

    it('should display seconds only when under 1 minute', () => {
      const progress = createMockProgress({ estimatedTimeRemaining: 45 });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      expect(wrapper.text()).toContain('45');
    });

    it('should handle zero time remaining', () => {
      const progress = createMockProgress({ estimatedTimeRemaining: 0 });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      // Should not throw or show negative time
      expect(wrapper.find('.estimated-time').exists()).toBe(true);
    });
  });

  // 2.4-UNIT-004: Stage list with status markers (✓/●/○)
  describe('stage status markers', () => {
    it('should render all stages', () => {
      const progress = createMockProgress();
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      const stages = wrapper.findAll('.stage-item');
      expect(stages).toHaveLength(5);
    });

    it('should show ✓ for completed stages', () => {
      const progress = createMockProgress({
        stages: [
          { id: 'upload', label: '上传完成', status: 'completed' }
        ]
      });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      const marker = wrapper.find('.stage-marker-completed');
      expect(marker.exists()).toBe(true);
      expect(marker.text()).toContain('✓');
    });

    it('should show ● for in_progress stages', () => {
      const progress = createMockProgress({
        stages: [
          { id: 'quality_assessment', label: '画面质量评估', status: 'in_progress' }
        ]
      });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      const marker = wrapper.find('.stage-marker-in_progress');
      expect(marker.exists()).toBe(true);
      expect(marker.text()).toContain('●');
    });

    it('should show ○ for pending stages', () => {
      const progress = createMockProgress({
        stages: [
          { id: 'script_generation', label: '脚本生成', status: 'pending' }
        ]
      });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      const marker = wrapper.find('.stage-marker-pending');
      expect(marker.exists()).toBe(true);
      expect(marker.text()).toContain('○');
    });

    it('should display stage labels correctly', () => {
      const progress = createMockProgress();
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      expect(wrapper.text()).toContain('上传完成');
      expect(wrapper.text()).toContain('镜头内容识别');
      expect(wrapper.text()).toContain('画面质量评估');
      expect(wrapper.text()).toContain('推荐镜头标记');
      expect(wrapper.text()).toContain('脚本生成');
    });
  });

  // 2.4-UNIT-005: Progress bar animation (CSS transition)
  describe('progress bar animation', () => {
    it('should have CSS transition for smooth animation', () => {
      const progress = createMockProgress();
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      const progressBar = wrapper.find('.progress-bar-fill');
      // The component should have transition style for smooth animation
      // BR-1.3: linear easing, 100ms
      const style = progressBar.attributes('style') || '';
      expect(
        style.includes('transition') ||
        wrapper.find('.progress-bar-fill').classes().some(c => c.includes('transition'))
      ).toBe(true);
    });
  });

  // 2.4-UNIT-006: Completion animation
  describe('completion state', () => {
    it('should show completion animation when status is completed', () => {
      const progress = createMockProgress({
        percent: 100,
        status: 'completed'
      });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      expect(wrapper.find('.completion-state').exists()).toBe(true);
      expect(wrapper.text()).toContain('✓');
      expect(wrapper.text()).toContain('分析完成');
    });

    it('should not show completion animation when analyzing', () => {
      const progress = createMockProgress({ status: 'analyzing' });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      expect(wrapper.find('.completion-state').exists()).toBe(false);
    });
  });

  // 2.4-BLIND-ERROR-001: Analysis failure display
  describe('error state', () => {
    it('should show error state when status is failed', () => {
      const progress = createMockProgress({
        status: 'failed',
        error: '分析失败，请重试'
      });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      expect(wrapper.find('.error-state').exists()).toBe(true);
      expect(wrapper.text()).toContain('分析失败');
    });

    it('should show retry button on failure', () => {
      const progress = createMockProgress({
        status: 'failed',
        error: '分析失败'
      });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      expect(wrapper.find('.retry-btn').exists()).toBe(true);
    });

    it('should emit retry event when retry button clicked', async () => {
      const progress = createMockProgress({
        status: 'failed',
        error: '分析失败'
      });
      const wrapper = mount(AnalysisProgress, {
        props: { progress }
      });

      await wrapper.find('.retry-btn').trigger('click');
      expect(wrapper.emitted('retry')).toBeTruthy();
    });
  });

  // Long wait warnings
  describe('long wait warnings', () => {
    it('should show warning when showLongWaitWarning prop is true', () => {
      const progress = createMockProgress();
      const wrapper = mount(AnalysisProgress, {
        props: { progress, showLongWaitWarning: true }
      });

      expect(wrapper.text()).toContain('分析时间较长');
    });

    it('should show retry option when showRetryOption prop is true', () => {
      const progress = createMockProgress();
      const wrapper = mount(AnalysisProgress, {
        props: { progress, showRetryOption: true }
      });

      expect(wrapper.find('.retry-btn').exists()).toBe(true);
    });
  });
});
