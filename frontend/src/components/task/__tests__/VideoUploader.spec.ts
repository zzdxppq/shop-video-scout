/**
 * VideoUploader component tests
 * Covers: Drag-drop zone, file selection, validation display
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import VideoUploader from '../VideoUploader.vue';
import { VIDEO_UPLOAD_CONSTRAINTS } from '../../../types/task';

/**
 * Helper to create a mock File object with specified size
 * Uses Object.defineProperty to mock size without allocating actual memory
 */
function createMockFile(
  name: string,
  size: number,
  type: string = 'video/mp4'
): File {
  const blob = new Blob(['x'], { type });
  const file = new File([blob], name, { type });
  // Override size property without allocating memory
  Object.defineProperty(file, 'size', { value: size, writable: false });
  return file;
}

/**
 * Helper to create mock DataTransfer
 */
function createMockDataTransfer(files: File[]): DataTransfer {
  const dataTransfer = {
    files: files as unknown as FileList,
    items: files.map((file) => ({ kind: 'file', type: file.type, getAsFile: () => file })),
    types: ['Files'],
  };
  return dataTransfer as unknown as DataTransfer;
}

describe('VideoUploader', () => {
  const defaultProps = {
    currentCount: 0,
    maxCount: VIDEO_UPLOAD_CONSTRAINTS.MAX_VIDEO_COUNT,
    disabled: false,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('rendering', () => {
    it('should render upload zone with correct text', () => {
      const wrapper = mount(VideoUploader, { props: defaultProps });

      expect(wrapper.text()).toContain('拖拽视频文件到此处');
      expect(wrapper.text()).toContain('点击选择文件');
      expect(wrapper.text()).toContain('支持 MP4/MOV');
    });

    it('should show count display', () => {
      const wrapper = mount(VideoUploader, {
        props: { ...defaultProps, currentCount: 5, maxCount: 20 },
      });

      expect(wrapper.text()).toContain('已上传: 5/20');
    });

    it('should show disabled state when max reached', () => {
      const wrapper = mount(VideoUploader, {
        props: { ...defaultProps, currentCount: 20, maxCount: 20 },
      });

      expect(wrapper.text()).toContain('已达上传上限');
      expect(wrapper.find('.video-uploader').classes()).toContain('is-disabled');
    });

    it('should show disabled state when disabled prop is true', () => {
      const wrapper = mount(VideoUploader, {
        props: { ...defaultProps, disabled: true },
      });

      expect(wrapper.find('.video-uploader').classes()).toContain('is-disabled');
    });
  });

  describe('drag and drop', () => {
    it('should add dragging class on dragenter', async () => {
      const wrapper = mount(VideoUploader, { props: defaultProps });
      const dropZone = wrapper.find('.video-uploader');

      await dropZone.trigger('dragenter', {
        preventDefault: vi.fn(),
        dataTransfer: createMockDataTransfer([]),
      });

      expect(wrapper.find('.video-uploader').classes()).toContain('is-dragging');
      expect(wrapper.text()).toContain('释放上传');
    });

    it('should remove dragging class on dragleave', async () => {
      const wrapper = mount(VideoUploader, { props: defaultProps });
      const dropZone = wrapper.find('.video-uploader');

      await dropZone.trigger('dragenter', { preventDefault: vi.fn() });
      await dropZone.trigger('dragleave', {
        preventDefault: vi.fn(),
        relatedTarget: null,
        currentTarget: dropZone.element,
      });

      expect(wrapper.find('.video-uploader').classes()).not.toContain('is-dragging');
    });

    it('should not activate drag when disabled', async () => {
      const wrapper = mount(VideoUploader, {
        props: { ...defaultProps, currentCount: 20, maxCount: 20 },
      });
      const dropZone = wrapper.find('.video-uploader');

      await dropZone.trigger('dragenter', { preventDefault: vi.fn() });

      expect(wrapper.find('.video-uploader').classes()).not.toContain('is-dragging');
    });

    it('should emit files-selected on valid drop', async () => {
      const wrapper = mount(VideoUploader, { props: defaultProps });
      const dropZone = wrapper.find('.video-uploader');
      const file = createMockFile('test.mp4', 1024);

      await dropZone.trigger('drop', {
        preventDefault: vi.fn(),
        dataTransfer: createMockDataTransfer([file]),
      });

      expect(wrapper.emitted('files-selected')).toBeTruthy();
      expect(wrapper.emitted('files-selected')![0][0]).toEqual([file]);
    });

    it('should emit validation-error for invalid files', async () => {
      const wrapper = mount(VideoUploader, { props: defaultProps });
      const dropZone = wrapper.find('.video-uploader');
      const invalidFile = createMockFile('test.txt', 1024, 'text/plain');

      await dropZone.trigger('drop', {
        preventDefault: vi.fn(),
        dataTransfer: createMockDataTransfer([invalidFile]),
      });

      expect(wrapper.emitted('validation-error')).toBeTruthy();
      const errors = wrapper.emitted('validation-error')![0][0] as string[];
      expect(errors[0]).toContain('不支持的文件格式');
    });

    it('should filter out non-video files from batch', async () => {
      const wrapper = mount(VideoUploader, { props: defaultProps });
      const dropZone = wrapper.find('.video-uploader');
      const validFile = createMockFile('video.mp4', 1024);
      const invalidFile = createMockFile('doc.pdf', 1024, 'application/pdf');

      await dropZone.trigger('drop', {
        preventDefault: vi.fn(),
        dataTransfer: createMockDataTransfer([validFile, invalidFile]),
      });

      expect(wrapper.emitted('files-selected')).toBeTruthy();
      expect(wrapper.emitted('files-selected')![0][0]).toEqual([validFile]);
      expect(wrapper.emitted('validation-error')).toBeTruthy();
    });

    it('should not process drop when at max count', async () => {
      const wrapper = mount(VideoUploader, {
        props: { ...defaultProps, currentCount: 20, maxCount: 20 },
      });
      const dropZone = wrapper.find('.video-uploader');
      const file = createMockFile('test.mp4', 1024);

      await dropZone.trigger('drop', {
        preventDefault: vi.fn(),
        dataTransfer: createMockDataTransfer([file]),
      });

      expect(wrapper.emitted('files-selected')).toBeFalsy();
    });
  });

  describe('file size validation', () => {
    it('should reject files over 100MB', async () => {
      const wrapper = mount(VideoUploader, { props: defaultProps });
      const dropZone = wrapper.find('.video-uploader');
      const largeFile = createMockFile('large.mp4', 150 * 1024 * 1024);

      await dropZone.trigger('drop', {
        preventDefault: vi.fn(),
        dataTransfer: createMockDataTransfer([largeFile]),
      });

      expect(wrapper.emitted('validation-error')).toBeTruthy();
      const errors = wrapper.emitted('validation-error')![0][0] as string[];
      expect(errors[0]).toContain('文件过大');
    });
  });

  describe('file count validation', () => {
    it('should show error when exceeding remaining slots', async () => {
      const wrapper = mount(VideoUploader, {
        props: { ...defaultProps, currentCount: 18, maxCount: 20 },
      });
      const dropZone = wrapper.find('.video-uploader');
      const files = [
        createMockFile('v1.mp4', 1024),
        createMockFile('v2.mp4', 1024),
        createMockFile('v3.mp4', 1024),
      ];

      await dropZone.trigger('drop', {
        preventDefault: vi.fn(),
        dataTransfer: createMockDataTransfer(files),
      });

      expect(wrapper.emitted('validation-error')).toBeTruthy();
      const errors = wrapper.emitted('validation-error')![0][0] as string[];
      expect(errors.some((e) => e.includes('最多还能上传'))).toBe(true);
    });

    it('should only accept files up to remaining slots', async () => {
      const wrapper = mount(VideoUploader, {
        props: { ...defaultProps, currentCount: 18, maxCount: 20 },
      });
      const dropZone = wrapper.find('.video-uploader');
      const files = [
        createMockFile('v1.mp4', 1024),
        createMockFile('v2.mp4', 1024),
        createMockFile('v3.mp4', 1024),
      ];

      await dropZone.trigger('drop', {
        preventDefault: vi.fn(),
        dataTransfer: createMockDataTransfer(files),
      });

      // Should only accept 2 files (remaining slots)
      const emitted = wrapper.emitted('files-selected');
      if (emitted) {
        const selectedFiles = emitted[0][0] as File[];
        expect(selectedFiles.length).toBeLessThanOrEqual(2);
      }
    });
  });

  describe('click to upload', () => {
    it('should trigger file input on click', async () => {
      const wrapper = mount(VideoUploader, { props: defaultProps });
      const fileInput = wrapper.find('input[type="file"]');

      // Mock the click method directly on the element
      const clickMock = vi.fn();
      (fileInput.element as HTMLInputElement).click = clickMock;

      await wrapper.find('.video-uploader').trigger('click');

      expect(clickMock).toHaveBeenCalled();
    });

    it('should not trigger file input when disabled', async () => {
      const wrapper = mount(VideoUploader, {
        props: { ...defaultProps, currentCount: 20, maxCount: 20 },
      });
      const fileInput = wrapper.find('input[type="file"]');
      const clickSpy = vi.spyOn(fileInput.element, 'click');

      await wrapper.find('.video-uploader').trigger('click');

      expect(clickSpy).not.toHaveBeenCalled();
    });

    it('should have correct accept attribute', () => {
      const wrapper = mount(VideoUploader, { props: defaultProps });
      const fileInput = wrapper.find('input[type="file"]');

      const accept = fileInput.attributes('accept');
      expect(accept).toContain('.mp4');
      expect(accept).toContain('.mov');
    });

    it('should allow multiple file selection', () => {
      const wrapper = mount(VideoUploader, { props: defaultProps });
      const fileInput = wrapper.find('input[type="file"]');

      expect(fileInput.attributes('multiple')).toBeDefined();
    });
  });

  // 2.2-BLIND-FLOW-003: Double-click prevention (handled by file input native behavior)
  describe('double-click prevention', () => {
    it('should reset input value after selection to allow re-selection', async () => {
      const wrapper = mount(VideoUploader, { props: defaultProps });
      const fileInput = wrapper.find('input[type="file"]');

      // Simulate file selection
      const file = createMockFile('test.mp4', 1024);
      Object.defineProperty(fileInput.element, 'files', {
        value: [file],
        configurable: true,
      });

      await fileInput.trigger('change');

      // Input should be reset
      expect((fileInput.element as HTMLInputElement).value).toBe('');
    });
  });
});
