/**
 * Unit tests for DeleteConfirmModal component.
 * Story 5.5: 历史任务管理
 */
import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import DeleteConfirmModal from '../DeleteConfirmModal.vue';

describe('DeleteConfirmModal', () => {
  describe('visibility', () => {
    it('should not render when visible is false', () => {
      const wrapper = mount(DeleteConfirmModal, {
        props: {
          visible: false,
          message: 'Test message'
        }
      });

      expect(wrapper.find('[role="dialog"]').exists()).toBe(false);
    });

    it('should render when visible is true', () => {
      const wrapper = mount(DeleteConfirmModal, {
        props: {
          visible: true,
          message: 'Test message'
        },
        global: {
          stubs: { Teleport: true }
        }
      });

      expect(wrapper.text()).toContain('Test message');
    });
  });

  describe('content', () => {
    it('should display title when provided', () => {
      const wrapper = mount(DeleteConfirmModal, {
        props: {
          visible: true,
          title: 'Delete Task',
          message: 'Are you sure?'
        },
        global: {
          stubs: { Teleport: true }
        }
      });

      expect(wrapper.text()).toContain('Delete Task');
    });

    it('should display message', () => {
      const wrapper = mount(DeleteConfirmModal, {
        props: {
          visible: true,
          message: '删除后无法恢复'
        },
        global: {
          stubs: { Teleport: true }
        }
      });

      expect(wrapper.text()).toContain('删除后无法恢复');
    });

    it('should use default button text', () => {
      const wrapper = mount(DeleteConfirmModal, {
        props: {
          visible: true,
          message: 'Test'
        },
        global: {
          stubs: { Teleport: true }
        }
      });

      expect(wrapper.text()).toContain('取消');
      expect(wrapper.text()).toContain('确认删除');
    });

    it('should use custom button text when provided', () => {
      const wrapper = mount(DeleteConfirmModal, {
        props: {
          visible: true,
          message: 'Test',
          confirmText: 'Yes, Delete',
          cancelText: 'No, Keep'
        },
        global: {
          stubs: { Teleport: true }
        }
      });

      expect(wrapper.text()).toContain('Yes, Delete');
      expect(wrapper.text()).toContain('No, Keep');
    });
  });

  describe('interactions', () => {
    it('should emit confirm when confirm button is clicked', async () => {
      const wrapper = mount(DeleteConfirmModal, {
        props: {
          visible: true,
          message: 'Test'
        },
        global: {
          stubs: { Teleport: true }
        }
      });

      const confirmButton = wrapper.findAll('button')[1];
      await confirmButton.trigger('click');

      expect(wrapper.emitted('confirm')).toBeTruthy();
    });

    it('should emit cancel when cancel button is clicked', async () => {
      const wrapper = mount(DeleteConfirmModal, {
        props: {
          visible: true,
          message: 'Test'
        },
        global: {
          stubs: { Teleport: true }
        }
      });

      const cancelButton = wrapper.findAll('button')[0];
      await cancelButton.trigger('click');

      expect(wrapper.emitted('cancel')).toBeTruthy();
    });

    it('should not emit events when loading', async () => {
      const wrapper = mount(DeleteConfirmModal, {
        props: {
          visible: true,
          message: 'Test',
          loading: true
        },
        global: {
          stubs: { Teleport: true }
        }
      });

      const buttons = wrapper.findAll('button');
      await buttons[0].trigger('click');
      await buttons[1].trigger('click');

      expect(wrapper.emitted('confirm')).toBeFalsy();
      expect(wrapper.emitted('cancel')).toBeFalsy();
    });
  });

  describe('loading state', () => {
    it('should show spinner when loading', () => {
      const wrapper = mount(DeleteConfirmModal, {
        props: {
          visible: true,
          message: 'Test',
          loading: true
        },
        global: {
          stubs: { Teleport: true }
        }
      });

      expect(wrapper.find('.animate-spin').exists()).toBe(true);
      expect(wrapper.text()).toContain('删除中...');
    });

    it('should disable buttons when loading', () => {
      const wrapper = mount(DeleteConfirmModal, {
        props: {
          visible: true,
          message: 'Test',
          loading: true
        },
        global: {
          stubs: { Teleport: true }
        }
      });

      const buttons = wrapper.findAll('button');
      expect(buttons[0].attributes('disabled')).toBeDefined();
      expect(buttons[1].attributes('disabled')).toBeDefined();
    });
  });
});
