/**
 * Unit tests for TitleList and TitleCard components.
 * Story 5.4: 发布辅助UI组件 - AC2
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import TitleList from '../TitleList.vue';
import TitleCard from '../TitleCard.vue';

// Mock useClipboard
vi.mock('../../../composables/useClipboard', () => ({
  useClipboard: () => ({
    copyToClipboard: vi.fn().mockResolvedValue(true),
    copying: { value: false },
    copiedText: { value: null }
  })
}));

describe('TitleList', () => {
  const mockTitles = [
    '望京这家海底捞太绝了！服务好到让你感动流泪',
    '人均89吃海底捞？这个团购也太香了吧！',
    '海底捞探店｜必点招牌毛肚，七上八下太嫩了'
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('rendering', () => {
    it('should render all titles', () => {
      const wrapper = mount(TitleList, {
        props: { titles: mockTitles }
      });

      const cards = wrapper.findAllComponents(TitleCard);
      expect(cards).toHaveLength(3);
    });

    it('should show empty state when no titles', () => {
      const wrapper = mount(TitleList, {
        props: { titles: [] }
      });

      expect(wrapper.text()).toContain('暂无标题推荐');
      expect(wrapper.findAllComponents(TitleCard)).toHaveLength(0);
    });

    it('should mark first title as recommended', () => {
      const wrapper = mount(TitleList, {
        props: { titles: mockTitles }
      });

      const cards = wrapper.findAllComponents(TitleCard);
      expect(cards[0].props('isRecommended')).toBe(true);
      expect(cards[1].props('isRecommended')).toBe(false);
      expect(cards[2].props('isRecommended')).toBe(false);
    });
  });

  describe('events', () => {
    it('should emit copy event when TitleCard emits copy', async () => {
      const wrapper = mount(TitleList, {
        props: { titles: mockTitles }
      });

      const firstCard = wrapper.findComponent(TitleCard);
      await firstCard.vm.$emit('copy', mockTitles[0]);

      expect(wrapper.emitted('copy')).toBeTruthy();
      expect(wrapper.emitted('copy')![0]).toEqual([mockTitles[0]]);
    });

    it('should emit error event when TitleCard emits error', async () => {
      const wrapper = mount(TitleList, {
        props: { titles: mockTitles }
      });

      const firstCard = wrapper.findComponent(TitleCard);
      await firstCard.vm.$emit('error', '复制失败');

      expect(wrapper.emitted('error')).toBeTruthy();
      expect(wrapper.emitted('error')![0]).toEqual(['复制失败']);
    });
  });
});

describe('TitleCard', () => {
  const mockTitle = '望京这家海底捞太绝了！服务好到让你感动流泪';

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('rendering', () => {
    it('should render title text', () => {
      const wrapper = mount(TitleCard, {
        props: { title: mockTitle, index: 0 }
      });

      expect(wrapper.text()).toContain(mockTitle);
    });

    it('should show recommended badge when isRecommended is true', () => {
      const wrapper = mount(TitleCard, {
        props: { title: mockTitle, index: 0, isRecommended: true }
      });

      const badge = wrapper.find('.bg-orange-100');
      expect(badge.exists()).toBe(true);
      expect(badge.text()).toBe('推荐');
    });

    it('should not show recommended badge when isRecommended is false', () => {
      const wrapper = mount(TitleCard, {
        props: { title: mockTitle, index: 1, isRecommended: false }
      });

      expect(wrapper.find('.bg-orange-100').exists()).toBe(false);
    });

    it('should have copy button', () => {
      const wrapper = mount(TitleCard, {
        props: { title: mockTitle, index: 0 }
      });

      const copyButton = wrapper.find('button[title="点击复制"]');
      expect(copyButton.exists()).toBe(true);
    });
  });

  describe('interactions', () => {
    it('should emit copy event when copy button is clicked', async () => {
      const wrapper = mount(TitleCard, {
        props: { title: mockTitle, index: 0 }
      });

      const copyButton = wrapper.find('button[title="点击复制"]');
      await copyButton.trigger('click');

      expect(wrapper.emitted('copy')).toBeTruthy();
      expect(wrapper.emitted('copy')![0]).toEqual([mockTitle]);
    });
  });

  describe('styling', () => {
    it('should have card styling classes', () => {
      const wrapper = mount(TitleCard, {
        props: { title: mockTitle, index: 0 }
      });

      const card = wrapper.find('.title-card');
      expect(card.classes()).toContain('bg-white');
      expect(card.classes()).toContain('border');
      expect(card.classes()).toContain('rounded-lg');
    });
  });
});
