/**
 * Unit tests for TaskHistoryCard component.
 * Story 5.5: 历史任务管理
 */
import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import TaskHistoryCard from '../TaskHistoryCard.vue';
import type { TaskSummary } from '../../../types/task';

describe('TaskHistoryCard', () => {
  const mockTask: TaskSummary = {
    id: 1,
    shop_name: '海底捞火锅(望京店)',
    shop_type: 'food',
    status: 'completed',
    thumbnail_url: 'https://example.com/thumb.jpg',
    created_at: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString() // 2 hours ago
  };

  describe('rendering', () => {
    it('should render shop name', () => {
      const wrapper = mount(TaskHistoryCard, {
        props: { task: mockTask }
      });

      expect(wrapper.text()).toContain('海底捞火锅(望京店)');
    });

    it('should render shop type label', () => {
      const wrapper = mount(TaskHistoryCard, {
        props: { task: mockTask }
      });

      expect(wrapper.text()).toContain('餐饮美食');
    });

    it('should render completed status with check icon', () => {
      const wrapper = mount(TaskHistoryCard, {
        props: { task: mockTask }
      });

      expect(wrapper.text()).toContain('已完成');
      expect(wrapper.text()).toContain('✓');
    });

    it('should render composing status with spinner', () => {
      const wrapper = mount(TaskHistoryCard, {
        props: { task: { ...mockTask, status: 'composing' as const } }
      });

      expect(wrapper.text()).toContain('处理中');
      expect(wrapper.find('.animate-spin').exists()).toBe(true);
    });

    it('should render failed status with X icon', () => {
      const wrapper = mount(TaskHistoryCard, {
        props: { task: { ...mockTask, status: 'failed' as const } }
      });

      expect(wrapper.text()).toContain('失败');
      expect(wrapper.text()).toContain('✕');
    });

    it('should render relative time', () => {
      const wrapper = mount(TaskHistoryCard, {
        props: { task: mockTask }
      });

      expect(wrapper.text()).toContain('2小时前');
    });

    it('should render thumbnail when available', () => {
      const wrapper = mount(TaskHistoryCard, {
        props: { task: mockTask }
      });

      const img = wrapper.find('img');
      expect(img.exists()).toBe(true);
      expect(img.attributes('src')).toBe('https://example.com/thumb.jpg');
    });

    it('should render placeholder when no thumbnail', () => {
      const wrapper = mount(TaskHistoryCard, {
        props: { task: { ...mockTask, thumbnail_url: undefined } }
      });

      expect(wrapper.text()).toContain('🎬');
    });
  });

  describe('interactions', () => {
    it('should emit click when card is clicked', async () => {
      const wrapper = mount(TaskHistoryCard, {
        props: { task: mockTask }
      });

      await wrapper.trigger('click');

      expect(wrapper.emitted('click')).toBeTruthy();
      expect(wrapper.emitted('click')![0]).toEqual([mockTask]);
    });

    it('should emit delete when delete button is clicked', async () => {
      const wrapper = mount(TaskHistoryCard, {
        props: { task: mockTask }
      });

      const deleteButton = wrapper.find('button');
      await deleteButton.trigger('click');

      expect(wrapper.emitted('delete')).toBeTruthy();
      expect(wrapper.emitted('delete')![0]).toEqual([mockTask]);
      // Should not also emit click
      expect(wrapper.emitted('click')).toBeFalsy();
    });

    it('should not emit delete for in-progress tasks', async () => {
      const wrapper = mount(TaskHistoryCard, {
        props: { task: { ...mockTask, status: 'composing' as const } }
      });

      const deleteButton = wrapper.find('button');
      await deleteButton.trigger('click');

      expect(wrapper.emitted('delete')).toBeFalsy();
    });

    it('should disable delete button for analyzing tasks', () => {
      const wrapper = mount(TaskHistoryCard, {
        props: { task: { ...mockTask, status: 'analyzing' as const } }
      });

      const deleteButton = wrapper.find('button');
      expect(deleteButton.attributes('disabled')).toBeDefined();
    });
  });

  describe('relative time formatting', () => {
    it('should show "刚刚" for very recent tasks', () => {
      const recentTask = {
        ...mockTask,
        created_at: new Date(Date.now() - 30 * 1000).toISOString() // 30 seconds ago
      };

      const wrapper = mount(TaskHistoryCard, {
        props: { task: recentTask }
      });

      expect(wrapper.text()).toContain('刚刚');
    });

    it('should show minutes for tasks within an hour', () => {
      const task = {
        ...mockTask,
        created_at: new Date(Date.now() - 15 * 60 * 1000).toISOString() // 15 minutes ago
      };

      const wrapper = mount(TaskHistoryCard, {
        props: { task }
      });

      expect(wrapper.text()).toContain('15分钟前');
    });

    it('should show days for older tasks', () => {
      const task = {
        ...mockTask,
        created_at: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString() // 3 days ago
      };

      const wrapper = mount(TaskHistoryCard, {
        props: { task }
      });

      expect(wrapper.text()).toContain('3天前');
    });
  });
});
