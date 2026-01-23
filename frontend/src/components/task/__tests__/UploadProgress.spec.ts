/**
 * UploadProgress component tests
 * Covers: Progress display, cancel functionality, status rendering
 */
import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import UploadProgress from '../UploadProgress.vue';
import type { UploadingFile } from '../../../composables/useVideoUpload';

/**
 * Helper to create mock UploadingFile
 */
function createMockUploadingFile(
  overrides: Partial<UploadingFile> = {}
): UploadingFile {
  return {
    id: `upload-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
    file: new File([''], 'test-video.mp4', { type: 'video/mp4' }),
    progress: 0,
    status: 'pending',
    ...overrides,
  };
}

describe('UploadProgress', () => {
  describe('rendering', () => {
    it('should not render when files array is empty', () => {
      const wrapper = mount(UploadProgress, {
        props: { files: [] },
      });

      expect(wrapper.find('.upload-progress-list').exists()).toBe(false);
    });

    it('should render upload items for each file', () => {
      const files = [
        createMockUploadingFile({ id: '1' }),
        createMockUploadingFile({ id: '2' }),
      ];
      const wrapper = mount(UploadProgress, { props: { files } });

      expect(wrapper.findAll('.upload-item')).toHaveLength(2);
    });

    it('should display file name', () => {
      const file = new File([''], 'my-video.mp4', { type: 'video/mp4' });
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ file })] },
      });

      expect(wrapper.text()).toContain('my-video.mp4');
    });

    it('should display file size in MB', () => {
      const content = new Array(5 * 1024 * 1024).fill('a').join('');
      const file = new File([content], 'video.mp4', { type: 'video/mp4' });
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ file })] },
      });

      expect(wrapper.text()).toMatch(/5\.\d MB/);
    });

    it('should display file size in KB for small files', () => {
      const content = new Array(500 * 1024).fill('a').join('');
      const file = new File([content], 'video.mp4', { type: 'video/mp4' });
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ file })] },
      });

      expect(wrapper.text()).toMatch(/\d+\.\d KB/);
    });
  });

  describe('status display', () => {
    it('should show "等待上传" for pending status', () => {
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ status: 'pending' })] },
      });

      expect(wrapper.text()).toContain('等待上传');
    });

    it('should show "上传中" for uploading status', () => {
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ status: 'uploading', progress: 50 })] },
      });

      expect(wrapper.text()).toContain('上传中');
    });

    it('should show "上传完成" for uploaded status', () => {
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ status: 'uploaded' })] },
      });

      expect(wrapper.text()).toContain('上传完成');
    });

    it('should show "上传失败" for failed status', () => {
      const wrapper = mount(UploadProgress, {
        props: {
          files: [createMockUploadingFile({ status: 'failed', error: 'Network error' })],
        },
      });

      expect(wrapper.text()).toContain('上传失败');
    });

    it('should apply correct status class', () => {
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ status: 'uploading', progress: 50 })] },
      });

      expect(wrapper.find('.upload-item').classes()).toContain('status-uploading');
    });
  });

  describe('progress display', () => {
    it('should show progress bar during upload', () => {
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ status: 'uploading', progress: 50 })] },
      });

      expect(wrapper.find('.progress-bar').exists()).toBe(true);
    });

    it('should show correct progress percentage', () => {
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ status: 'uploading', progress: 75 })] },
      });

      expect(wrapper.text()).toContain('75%');
    });

    it('should update progress bar width', () => {
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ status: 'uploading', progress: 60 })] },
      });

      const progressFill = wrapper.find('.progress-fill');
      expect(progressFill.attributes('style')).toContain('width: 60%');
    });

    it('should show 0% at start of upload', () => {
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ status: 'uploading', progress: 0 })] },
      });

      expect(wrapper.text()).toContain('0%');
    });

    it('should show 100% when upload complete', () => {
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ status: 'uploading', progress: 100 })] },
      });

      expect(wrapper.text()).toContain('100%');
    });
  });

  describe('cancel functionality', () => {
    it('should show cancel button during upload', () => {
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ status: 'uploading', progress: 50 })] },
      });

      expect(wrapper.find('.cancel-btn').exists()).toBe(true);
    });

    it('should not show cancel button for pending files', () => {
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ status: 'pending' })] },
      });

      expect(wrapper.find('.cancel-btn').exists()).toBe(false);
    });

    it('should emit cancel event when cancel button clicked', async () => {
      const uploadFile = createMockUploadingFile({
        id: 'test-123',
        status: 'uploading',
        progress: 50,
      });
      const wrapper = mount(UploadProgress, { props: { files: [uploadFile] } });

      await wrapper.find('.cancel-btn').trigger('click');

      expect(wrapper.emitted('cancel')).toBeTruthy();
      expect(wrapper.emitted('cancel')![0]).toEqual(['test-123']);
    });
  });

  describe('remove functionality', () => {
    it('should show remove button for pending files', () => {
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ status: 'pending' })] },
      });

      expect(wrapper.find('.remove-btn').exists()).toBe(true);
    });

    it('should show remove button for failed files', () => {
      const wrapper = mount(UploadProgress, {
        props: {
          files: [createMockUploadingFile({ status: 'failed', error: 'Error' })],
        },
      });

      expect(wrapper.find('.remove-btn').exists()).toBe(true);
    });

    it('should emit remove event when remove button clicked', async () => {
      const uploadFile = createMockUploadingFile({ id: 'test-456', status: 'pending' });
      const wrapper = mount(UploadProgress, { props: { files: [uploadFile] } });

      await wrapper.find('.remove-btn').trigger('click');

      expect(wrapper.emitted('remove')).toBeTruthy();
      expect(wrapper.emitted('remove')![0]).toEqual(['test-456']);
    });
  });

  describe('retry functionality', () => {
    it('should show retry button for failed files', () => {
      const wrapper = mount(UploadProgress, {
        props: {
          files: [createMockUploadingFile({ status: 'failed', error: 'Network error' })],
        },
      });

      expect(wrapper.find('.retry-btn').exists()).toBe(true);
    });

    it('should not show retry button for successful uploads', () => {
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ status: 'uploaded' })] },
      });

      expect(wrapper.find('.retry-btn').exists()).toBe(false);
    });

    it('should emit retry event when retry button clicked', async () => {
      const uploadFile = createMockUploadingFile({
        id: 'test-789',
        status: 'failed',
        error: 'Timeout',
      });
      const wrapper = mount(UploadProgress, { props: { files: [uploadFile] } });

      await wrapper.find('.retry-btn').trigger('click');

      expect(wrapper.emitted('retry')).toBeTruthy();
      expect(wrapper.emitted('retry')![0]).toEqual(['test-789']);
    });

    it('should display error message for failed files', () => {
      const wrapper = mount(UploadProgress, {
        props: {
          files: [
            createMockUploadingFile({
              status: 'failed',
              error: '网络错误',
            }),
          ],
        },
      });

      expect(wrapper.find('.error-text').text()).toBe('网络错误');
    });
  });

  describe('success state', () => {
    it('should show success icon for uploaded files', () => {
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ status: 'uploaded' })] },
      });

      expect(wrapper.find('.success-icon').exists()).toBe(true);
    });

    it('should not show action buttons for uploaded files', () => {
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ status: 'uploaded' })] },
      });

      expect(wrapper.find('.cancel-btn').exists()).toBe(false);
      expect(wrapper.find('.remove-btn').exists()).toBe(false);
      expect(wrapper.find('.retry-btn').exists()).toBe(false);
    });
  });

  describe('file name display', () => {
    it('should truncate long file names', () => {
      const longFileName = 'very-long-file-name-that-should-be-truncated.mp4';
      const file = new File([''], longFileName, { type: 'video/mp4' });
      const wrapper = mount(UploadProgress, {
        props: { files: [createMockUploadingFile({ file })] },
      });

      const nameElement = wrapper.find('.file-name');
      expect(nameElement.attributes('title')).toBe(longFileName);
    });
  });
});
