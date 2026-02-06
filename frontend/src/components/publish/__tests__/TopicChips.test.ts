/**
 * Unit tests for TopicChips component.
 * Story 5.4: 发布辅助UI组件 - AC1
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import TopicChips from '../TopicChips.vue';

// Mock useClipboard
vi.mock('../../../composables/useClipboard', () => ({
  useClipboard: () => ({
    copyToClipboard: vi.fn().mockResolvedValue(true),
    copying: { value: false },
    copiedText: { value: null }
  })
}));

describe('TopicChips', () => {
  const mockTopics = ['#海底捞', '#火锅探店', '#美食推荐'];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('rendering', () => {
    it('should render topics as chips', () => {
      const wrapper = mount(TopicChips, {
        props: { topics: mockTopics }
      });

      const chips = wrapper.findAll('button.bg-blue-100');
      expect(chips).toHaveLength(3);
      expect(chips[0].text()).toBe('#海底捞');
      expect(chips[1].text()).toBe('#火锅探店');
      expect(chips[2].text()).toBe('#美食推荐');
    });

    it('should show empty state when no topics', () => {
      const wrapper = mount(TopicChips, {
        props: { topics: [] }
      });

      expect(wrapper.text()).toContain('暂无话题推荐');
      expect(wrapper.findAll('button.bg-blue-100')).toHaveLength(0);
    });

    it('should render copy all button when topics exist', () => {
      const wrapper = mount(TopicChips, {
        props: { topics: mockTopics }
      });

      const copyAllButton = wrapper.find('button.bg-blue-50');
      expect(copyAllButton.exists()).toBe(true);
      expect(copyAllButton.text()).toContain('一键复制全部话题');
    });

    it('should not render copy all button when empty', () => {
      const wrapper = mount(TopicChips, {
        props: { topics: [] }
      });

      expect(wrapper.find('button.bg-blue-50').exists()).toBe(false);
    });
  });

  describe('interactions', () => {
    it('should emit copy event when chip is clicked', async () => {
      const wrapper = mount(TopicChips, {
        props: { topics: mockTopics }
      });

      const firstChip = wrapper.findAll('button.bg-blue-100')[0];
      await firstChip.trigger('click');

      expect(wrapper.emitted('copy')).toBeTruthy();
      expect(wrapper.emitted('copy')![0]).toEqual(['#海底捞']);
    });

    it('should emit copyAll event when copy all button is clicked', async () => {
      const wrapper = mount(TopicChips, {
        props: { topics: mockTopics }
      });

      const copyAllButton = wrapper.find('button.bg-blue-50');
      await copyAllButton.trigger('click');

      expect(wrapper.emitted('copyAll')).toBeTruthy();
    });
  });

  describe('styling', () => {
    it('should have correct chip styling classes', () => {
      const wrapper = mount(TopicChips, {
        props: { topics: mockTopics }
      });

      const chip = wrapper.find('button.bg-blue-100');
      expect(chip.classes()).toContain('rounded-full');
      expect(chip.classes()).toContain('text-blue-700');
      expect(chip.classes()).toContain('px-3');
      expect(chip.classes()).toContain('py-1');
    });
  });
});
