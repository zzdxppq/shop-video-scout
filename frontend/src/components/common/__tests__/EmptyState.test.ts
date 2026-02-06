/**
 * Unit tests for EmptyState component.
 * Story 5.5: 历史任务管理
 */
import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import EmptyState from '../EmptyState.vue';

describe('EmptyState', () => {
  describe('rendering', () => {
    it('should render title', () => {
      const wrapper = mount(EmptyState, {
        props: {
          title: '暂无任务'
        }
      });

      expect(wrapper.text()).toContain('暂无任务');
    });

    it('should render description when provided', () => {
      const wrapper = mount(EmptyState, {
        props: {
          title: '暂无任务',
          description: '开始创建您的第一个任务'
        }
      });

      expect(wrapper.text()).toContain('开始创建您的第一个任务');
    });

    it('should render custom icon when provided', () => {
      const wrapper = mount(EmptyState, {
        props: {
          title: 'Test',
          icon: '🎥'
        }
      });

      expect(wrapper.text()).toContain('🎥');
    });

    it('should render default icon when not provided', () => {
      const wrapper = mount(EmptyState, {
        props: {
          title: 'Test'
        }
      });

      expect(wrapper.text()).toContain('📋');
    });

    it('should render action button when actionText is provided', () => {
      const wrapper = mount(EmptyState, {
        props: {
          title: 'Test',
          actionText: '创建任务'
        }
      });

      const button = wrapper.find('button');
      expect(button.exists()).toBe(true);
      expect(button.text()).toBe('创建任务');
    });

    it('should not render action button when actionText is not provided', () => {
      const wrapper = mount(EmptyState, {
        props: {
          title: 'Test'
        }
      });

      expect(wrapper.find('button').exists()).toBe(false);
    });
  });

  describe('interactions', () => {
    it('should emit action when button is clicked', async () => {
      const wrapper = mount(EmptyState, {
        props: {
          title: 'Test',
          actionText: '创建任务'
        }
      });

      await wrapper.find('button').trigger('click');

      expect(wrapper.emitted('action')).toBeTruthy();
    });
  });

  describe('slots', () => {
    it('should render icon slot content', () => {
      const wrapper = mount(EmptyState, {
        props: {
          title: 'Test'
        },
        slots: {
          icon: '<span class="custom-icon">Custom</span>'
        }
      });

      expect(wrapper.find('.custom-icon').exists()).toBe(true);
    });

    it('should render extra slot content', () => {
      const wrapper = mount(EmptyState, {
        props: {
          title: 'Test'
        },
        slots: {
          extra: '<p class="extra-content">Extra info</p>'
        }
      });

      expect(wrapper.find('.extra-content').exists()).toBe(true);
    });
  });
});
