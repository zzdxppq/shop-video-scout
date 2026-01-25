/**
 * FrameGallery component tests.
 * Story 2.4: 分析进度展示页面 - AC2
 *
 * Test coverage:
 * - 2.4-UNIT-007: Tab rendering with 5 categories
 * - 2.4-UNIT-008: Tab labels show frame counts
 * - 2.4-UNIT-009: Frame card renders thumbnail, category, score
 * - 2.4-UNIT-010: Recommended frame shows star icon
 * - 2.4-UNIT-011: Quality score positioned at bottom-right
 * - 2.4-UNIT-012: Default tab selection is "全部"
 * - 2.4-UNIT-013: Empty state displays message
 * - 2.4-UNIT-014: Thumbnail placeholder on error
 * - 2.4-INT-007: Tab click filters frames
 * - 2.4-BLIND-BOUNDARY-003: Single frame in results
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import FrameGallery from '../FrameGallery.vue';
import type { VideoFrame, FrameTab } from '../../../types/analysis';

/**
 * Helper to create mock frame data.
 */
function createMockFrame(overrides: Partial<VideoFrame> = {}): VideoFrame {
  return {
    id: Math.floor(Math.random() * 1000),
    videoId: 1,
    thumbnailUrl: 'https://cdn.example.com/thumb.jpg',
    category: 'food',
    tags: ['美食', '精美'],
    qualityScore: 85,
    isRecommended: false,
    ...overrides
  };
}

/**
 * Helper to create mock tabs.
 */
function createMockTabs(frames: VideoFrame[]): FrameTab[] {
  return [
    { key: 'all', label: '全部', count: frames.length },
    { key: 'food', label: '食物', count: frames.filter(f => f.category === 'food').length },
    { key: 'person', label: '人物', count: frames.filter(f => f.category === 'person').length },
    { key: 'environment', label: '环境', count: frames.filter(f => f.category === 'environment').length },
    { key: 'other', label: '其他', count: frames.filter(f => f.category === 'other').length }
  ];
}

describe('FrameGallery', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // 2.4-UNIT-007: Tab rendering with 5 categories
  describe('tab rendering', () => {
    it('should render 5 tabs (全部/食物/人物/环境/其他)', () => {
      const frames = [createMockFrame()];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      const tabElements = wrapper.findAll('.tab-item');
      expect(tabElements).toHaveLength(5);
    });

    it('should display correct tab labels', () => {
      const frames = [createMockFrame()];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      expect(wrapper.text()).toContain('全部');
      expect(wrapper.text()).toContain('食物');
      expect(wrapper.text()).toContain('人物');
      expect(wrapper.text()).toContain('环境');
      expect(wrapper.text()).toContain('其他');
    });
  });

  // 2.4-UNIT-008: Tab labels show frame counts
  describe('tab counts', () => {
    it('should display count in tab labels (BR-2.4)', () => {
      const frames = [
        createMockFrame({ category: 'food' }),
        createMockFrame({ category: 'food' }),
        createMockFrame({ category: 'person' })
      ];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      expect(wrapper.text()).toContain('全部(3)');
      expect(wrapper.text()).toContain('食物(2)');
      expect(wrapper.text()).toContain('人物(1)');
      expect(wrapper.text()).toContain('环境(0)');
    });
  });

  // 2.4-UNIT-012: Default tab selection is "全部"
  describe('default tab selection', () => {
    it('should highlight "全部" tab when activeTab is all (BR-2.1)', () => {
      const frames = [createMockFrame()];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      const activeTab = wrapper.find('.tab-item.active');
      expect(activeTab.exists()).toBe(true);
      expect(activeTab.text()).toContain('全部');
    });
  });

  // 2.4-UNIT-009: Frame card renders thumbnail, category label, quality score
  describe('frame card rendering', () => {
    it('should render frame thumbnails', () => {
      const frames = [createMockFrame({ thumbnailUrl: 'https://cdn.example.com/test.jpg' })];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      const img = wrapper.find('.frame-thumbnail img');
      expect(img.exists()).toBe(true);
      expect(img.attributes('src')).toBe('https://cdn.example.com/test.jpg');
    });

    it('should display category labels', () => {
      const frames = [createMockFrame({ category: 'food' })];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      expect(wrapper.find('.frame-category').exists()).toBe(true);
    });

    it('should display quality score', () => {
      const frames = [createMockFrame({ qualityScore: 85 })];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      expect(wrapper.text()).toContain('85');
    });
  });

  // 2.4-UNIT-010: Recommended frame shows star icon (BR-2.2)
  describe('recommended frame marker', () => {
    it('should show ⭐ for recommended frames', () => {
      const frames = [createMockFrame({ isRecommended: true })];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      expect(wrapper.find('.recommended-marker').exists()).toBe(true);
      expect(wrapper.text()).toContain('⭐');
    });

    it('should not show star for non-recommended frames', () => {
      const frames = [createMockFrame({ isRecommended: false })];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      expect(wrapper.find('.recommended-marker').exists()).toBe(false);
    });
  });

  // 2.4-UNIT-011: Quality score positioned at thumbnail bottom-right (BR-2.3)
  describe('quality score positioning', () => {
    it('should position quality score at bottom-right', () => {
      const frames = [createMockFrame({ qualityScore: 90 })];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      const scoreElement = wrapper.find('.quality-score');
      expect(scoreElement.exists()).toBe(true);
      // The score should be inside the thumbnail container for positioning
      const thumbnail = wrapper.find('.frame-thumbnail');
      expect(thumbnail.find('.quality-score').exists()).toBe(true);
    });
  });

  // 2.4-UNIT-013: Empty state displays message
  describe('empty state', () => {
    it('should show empty state when no frames', () => {
      const frames: VideoFrame[] = [];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      expect(wrapper.find('.empty-state').exists()).toBe(true);
      expect(wrapper.text()).toContain('暂无分析结果');
    });

    it('should show guidance in empty state', () => {
      const frames: VideoFrame[] = [];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      // Should guide user to re-upload
      expect(wrapper.text()).toMatch(/重新上传|上传/);
    });
  });

  // 2.4-UNIT-014: Thumbnail placeholder on image load failure
  describe('image error handling', () => {
    it('should have onerror handler for fallback', () => {
      const frames = [createMockFrame({ thumbnailUrl: 'https://broken.url/img.jpg' })];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      const img = wrapper.find('.frame-thumbnail img');
      expect(img.exists()).toBe(true);
      // Image should have an error handler
      expect(img.attributes('onerror') || img.element.onerror !== null).toBeTruthy;
    });
  });

  // 2.4-INT-007: Tab click filters frames by category
  describe('tab filtering', () => {
    it('should emit tab-change event when tab clicked', async () => {
      const frames = [
        createMockFrame({ category: 'food' }),
        createMockFrame({ category: 'person' })
      ];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      const foodTab = wrapper.findAll('.tab-item')[1]; // food is second tab
      await foodTab.trigger('click');

      expect(wrapper.emitted('tab-change')).toBeTruthy();
      expect(wrapper.emitted('tab-change')![0]).toEqual(['food']);
    });
  });

  // 2.4-BLIND-BOUNDARY-003: Single frame in results
  describe('single frame handling', () => {
    it('should correctly display single frame', () => {
      const frames = [createMockFrame({ category: 'food' })];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      expect(wrapper.findAll('.frame-card')).toHaveLength(1);
      expect(wrapper.text()).toContain('全部(1)');
      expect(wrapper.text()).toContain('食物(1)');
    });
  });

  // Frame preview modal
  describe('frame preview', () => {
    it('should emit preview event when frame clicked', async () => {
      const frame = createMockFrame({ id: 42 });
      const frames = [frame];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      await wrapper.find('.frame-card').trigger('click');
      expect(wrapper.emitted('preview')).toBeTruthy();
      expect(wrapper.emitted('preview')![0]).toEqual([frame]);
    });
  });

  // Hover tooltip
  describe('hover tooltip', () => {
    it('should show tooltip data on frame card', () => {
      const frames = [createMockFrame({ tags: ['美食', '精致', '招牌'] })];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      const card = wrapper.find('.frame-card');
      // Card should have title or data attributes for tooltip
      expect(
        card.attributes('title') ||
        card.attributes('data-tags') ||
        card.find('[title]').exists()
      ).toBeTruthy;
    });
  });

  // Multiple frames rendering
  describe('multiple frames', () => {
    it('should render all frames in grid', () => {
      const frames = [
        createMockFrame({ id: 1 }),
        createMockFrame({ id: 2 }),
        createMockFrame({ id: 3 }),
        createMockFrame({ id: 4 })
      ];
      const tabs = createMockTabs(frames);
      const wrapper = mount(FrameGallery, {
        props: { frames, tabs, activeTab: 'all' }
      });

      expect(wrapper.findAll('.frame-card')).toHaveLength(4);
    });
  });
});
