/**
 * Unit tests for PublishAssistPanel and RegenerateButton components.
 * Story 5.4: 发布辅助UI组件 - AC3
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { ref, readonly } from 'vue';
import RegenerateButton from '../RegenerateButton.vue';

// Mock useClipboard
vi.mock('../../../composables/useClipboard', () => ({
  useClipboard: () => ({
    copyToClipboard: vi.fn().mockResolvedValue(true),
    copying: { value: false },
    copiedText: { value: null }
  })
}));

// Mock usePublishAssist with controllable state
const mockTopics = ref<string[]>([]);
const mockTitles = ref<string[]>([]);
const mockRegenerateRemaining = ref(3);
const mockLoading = ref(false);
const mockRegenerating = ref(false);
const mockError = ref<string | null>(null);
const mockFetchPublishAssist = vi.fn();
const mockRegenerate = vi.fn();
const mockClearError = vi.fn();

vi.mock('../../../composables/usePublishAssist', () => ({
  usePublishAssist: () => ({
    topics: readonly(mockTopics),
    titles: readonly(mockTitles),
    regenerateRemaining: readonly(mockRegenerateRemaining),
    loading: readonly(mockLoading),
    regenerating: readonly(mockRegenerating),
    error: readonly(mockError),
    fetchPublishAssist: mockFetchPublishAssist,
    regenerate: mockRegenerate,
    clearError: mockClearError
  })
}));

describe('RegenerateButton', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('rendering', () => {
    it('should show remaining count when available', () => {
      const wrapper = mount(RegenerateButton, {
        props: { remaining: 2, loading: false }
      });

      expect(wrapper.text()).toContain('剩余2次');
    });

    it('should show "已达上限" when remaining is 0', () => {
      const wrapper = mount(RegenerateButton, {
        props: { remaining: 0, loading: false }
      });

      expect(wrapper.text()).toContain('已达上限');
    });

    it('should show loading text when loading', () => {
      const wrapper = mount(RegenerateButton, {
        props: { remaining: 2, loading: true }
      });

      expect(wrapper.text()).toContain('正在生成...');
    });

    it('should show spinner when loading', () => {
      const wrapper = mount(RegenerateButton, {
        props: { remaining: 2, loading: true }
      });

      const spinner = wrapper.find('.animate-spin');
      expect(spinner.exists()).toBe(true);
    });
  });

  describe('states', () => {
    it('should be disabled when remaining is 0', () => {
      const wrapper = mount(RegenerateButton, {
        props: { remaining: 0, loading: false }
      });

      const button = wrapper.find('button');
      expect(button.attributes('disabled')).toBeDefined();
    });

    it('should be disabled when loading', () => {
      const wrapper = mount(RegenerateButton, {
        props: { remaining: 2, loading: true }
      });

      const button = wrapper.find('button');
      expect(button.attributes('disabled')).toBeDefined();
    });

    it('should be enabled when remaining > 0 and not loading', () => {
      const wrapper = mount(RegenerateButton, {
        props: { remaining: 2, loading: false }
      });

      const button = wrapper.find('button');
      expect(button.attributes('disabled')).toBeUndefined();
    });
  });

  describe('interactions', () => {
    it('should emit regenerate when clicked', async () => {
      const wrapper = mount(RegenerateButton, {
        props: { remaining: 2, loading: false }
      });

      await wrapper.find('button').trigger('click');

      expect(wrapper.emitted('regenerate')).toBeTruthy();
    });

    it('should not emit regenerate when disabled', async () => {
      const wrapper = mount(RegenerateButton, {
        props: { remaining: 0, loading: false }
      });

      await wrapper.find('button').trigger('click');

      expect(wrapper.emitted('regenerate')).toBeFalsy();
    });
  });

  describe('styling', () => {
    it('should have active styling when enabled', () => {
      const wrapper = mount(RegenerateButton, {
        props: { remaining: 2, loading: false }
      });

      const button = wrapper.find('button');
      expect(button.classes()).toContain('bg-blue-500');
      expect(button.classes()).toContain('text-white');
    });

    it('should have disabled styling when disabled', () => {
      const wrapper = mount(RegenerateButton, {
        props: { remaining: 0, loading: false }
      });

      const button = wrapper.find('button');
      expect(button.classes()).toContain('bg-gray-100');
      expect(button.classes()).toContain('text-gray-400');
    });
  });
});

// Note: Full PublishAssistPanel tests would require more complex setup
// These are basic structure tests
describe('PublishAssistPanel structure', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockTopics.value = ['#话题1', '#话题2'];
    mockTitles.value = ['标题1标题1标题1标题1标题1', '标题2标题2标题2标题2标题2'];
    mockRegenerateRemaining.value = 3;
    mockLoading.value = false;
    mockError.value = null;
  });

  it('should fetch data on mount', async () => {
    // Import dynamically after mocks are set
    const { default: PublishAssistPanel } = await import('../PublishAssistPanel.vue');

    mount(PublishAssistPanel, {
      props: { taskId: 123 }
    });

    await flushPromises();

    expect(mockFetchPublishAssist).toHaveBeenCalledWith(123);
  });
});
