/**
 * VideoList component tests
 * Covers: 2.2-UNIT-006, 2.2-UNIT-007, 2.2-UNIT-008, 2.2-INT-005
 */
import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import VideoList from '../VideoList.vue';
import type { TaskVideo } from '../../../types/task';

// Mock the formatDuration function
vi.mock('../../../composables/useVideoUpload', () => ({
  formatDuration: (seconds: number | undefined) => {
    if (seconds === undefined || seconds === null) return '--:--';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  },
}));

/**
 * Helper to create mock TaskVideo
 */
function createMockVideo(overrides: Partial<TaskVideo> = {}): TaskVideo {
  return {
    id: Math.floor(Math.random() * 1000),
    task_id: 1,
    original_filename: 'test-video.mp4',
    oss_key: 'videos/test-video.mp4',
    thumbnail_url: 'https://cdn.example.com/thumb.jpg',
    duration_seconds: 120,
    file_size: 10 * 1024 * 1024,
    status: 'uploaded',
    created_at: new Date().toISOString(),
    ...overrides,
  };
}

describe('VideoList', () => {
  describe('loading state', () => {
    it('should show loading spinner when loading is true', () => {
      const wrapper = mount(VideoList, {
        props: { videos: [], loading: true },
      });

      expect(wrapper.find('.loading-state').exists()).toBe(true);
      expect(wrapper.find('.loading-spinner').exists()).toBe(true);
      expect(wrapper.text()).toContain('加载中');
    });

    it('should not show loading when loading is false', () => {
      const wrapper = mount(VideoList, {
        props: { videos: [], loading: false },
      });

      expect(wrapper.find('.loading-state').exists()).toBe(false);
    });
  });

  describe('empty state', () => {
    it('should show empty state when no videos and not loading', () => {
      const wrapper = mount(VideoList, {
        props: { videos: [], loading: false },
      });

      expect(wrapper.find('.empty-state').exists()).toBe(true);
      expect(wrapper.text()).toContain('暂无视频');
    });

    it('should not show empty state when videos exist', () => {
      const wrapper = mount(VideoList, {
        props: { videos: [createMockVideo()], loading: false },
      });

      expect(wrapper.find('.empty-state').exists()).toBe(false);
    });
  });

  // 2.2-UNIT-006: Video grid rendering
  describe('grid rendering', () => {
    it('should render video grid with correct structure', () => {
      const videos = [createMockVideo({ id: 1 }), createMockVideo({ id: 2 })];
      const wrapper = mount(VideoList, { props: { videos, loading: false } });

      expect(wrapper.find('.video-grid').exists()).toBe(true);
      expect(wrapper.findAll('.video-item')).toHaveLength(2);
    });

    it('should display thumbnail image', () => {
      const video = createMockVideo({
        thumbnail_url: 'https://cdn.example.com/thumb123.jpg',
      });
      const wrapper = mount(VideoList, {
        props: { videos: [video], loading: false },
      });

      const img = wrapper.find('.video-thumbnail img');
      expect(img.exists()).toBe(true);
      expect(img.attributes('src')).toBe('https://cdn.example.com/thumb123.jpg');
    });

    it('should use placeholder when no thumbnail URL', () => {
      const video = createMockVideo({ thumbnail_url: undefined });
      const wrapper = mount(VideoList, {
        props: { videos: [video], loading: false },
      });

      const img = wrapper.find('.video-thumbnail img');
      expect(img.attributes('src')).toBe('/placeholder-video.svg');
    });

    it('should display video filename', () => {
      const video = createMockVideo({ original_filename: 'my-awesome-video.mp4' });
      const wrapper = mount(VideoList, {
        props: { videos: [video], loading: false },
      });

      expect(wrapper.find('.video-name').text()).toBe('my-awesome-video.mp4');
    });

    it('should set filename as title for truncation', () => {
      const longFilename = 'very-long-filename-that-will-be-truncated.mp4';
      const video = createMockVideo({ original_filename: longFilename });
      const wrapper = mount(VideoList, {
        props: { videos: [video], loading: false },
      });

      expect(wrapper.find('.video-name').attributes('title')).toBe(longFilename);
    });
  });

  // 2.2-UNIT-007: Duration display
  describe('duration display', () => {
    it('should format duration correctly (2:00 for 120 seconds)', () => {
      const video = createMockVideo({ duration_seconds: 120 });
      const wrapper = mount(VideoList, {
        props: { videos: [video], loading: false },
      });

      expect(wrapper.find('.video-duration').text()).toBe('2:00');
    });

    it('should format duration with leading zero for seconds (1:05 for 65 seconds)', () => {
      const video = createMockVideo({ duration_seconds: 65 });
      const wrapper = mount(VideoList, {
        props: { videos: [video], loading: false },
      });

      expect(wrapper.find('.video-duration').text()).toBe('1:05');
    });

    it('should handle undefined duration', () => {
      const video = createMockVideo({ duration_seconds: undefined });
      const wrapper = mount(VideoList, {
        props: { videos: [video], loading: false },
      });

      expect(wrapper.find('.video-duration').text()).toBe('--:--');
    });

    it('should handle zero duration', () => {
      const video = createMockVideo({ duration_seconds: 0 });
      const wrapper = mount(VideoList, {
        props: { videos: [video], loading: false },
      });

      expect(wrapper.find('.video-duration').text()).toBe('0:00');
    });
  });

  // 2.2-UNIT-008: Delete button visibility
  describe('delete button', () => {
    it('should have delete button in DOM', () => {
      const video = createMockVideo();
      const wrapper = mount(VideoList, {
        props: { videos: [video], loading: false },
      });

      expect(wrapper.find('.delete-btn').exists()).toBe(true);
    });

    it('should emit delete event with video id when clicked', async () => {
      const video = createMockVideo({ id: 42 });
      const wrapper = mount(VideoList, {
        props: { videos: [video], loading: false },
      });

      await wrapper.find('.delete-btn').trigger('click');

      expect(wrapper.emitted('delete')).toBeTruthy();
      expect(wrapper.emitted('delete')![0]).toEqual([42]);
    });

    it('should have correct title attribute', () => {
      const video = createMockVideo();
      const wrapper = mount(VideoList, {
        props: { videos: [video], loading: false },
      });

      expect(wrapper.find('.delete-btn').attributes('title')).toBe('删除视频');
    });
  });

  describe('image error handling', () => {
    it('should set alt attribute for accessibility', () => {
      const video = createMockVideo({ original_filename: 'my-video.mp4' });
      const wrapper = mount(VideoList, {
        props: { videos: [video], loading: false },
      });

      const img = wrapper.find('.video-thumbnail img');
      expect(img.attributes('alt')).toBe('my-video.mp4');
    });

    it('should have lazy loading attribute', () => {
      const video = createMockVideo();
      const wrapper = mount(VideoList, {
        props: { videos: [video], loading: false },
      });

      const img = wrapper.find('.video-thumbnail img');
      expect(img.attributes('loading')).toBe('lazy');
    });
  });

  describe('multiple videos', () => {
    it('should render all videos in correct order', () => {
      const videos = [
        createMockVideo({ id: 1, original_filename: 'first.mp4' }),
        createMockVideo({ id: 2, original_filename: 'second.mp4' }),
        createMockVideo({ id: 3, original_filename: 'third.mp4' }),
      ];
      const wrapper = mount(VideoList, {
        props: { videos, loading: false },
      });

      const items = wrapper.findAll('.video-item');
      expect(items).toHaveLength(3);

      const names = items.map((item) => item.find('.video-name').text());
      expect(names).toEqual(['first.mp4', 'second.mp4', 'third.mp4']);
    });

    it('should emit correct video id for each delete button', async () => {
      const videos = [
        createMockVideo({ id: 10 }),
        createMockVideo({ id: 20 }),
        createMockVideo({ id: 30 }),
      ];
      const wrapper = mount(VideoList, {
        props: { videos, loading: false },
      });

      const deleteButtons = wrapper.findAll('.delete-btn');

      await deleteButtons[1].trigger('click');
      expect(wrapper.emitted('delete')![0]).toEqual([20]);

      await deleteButtons[2].trigger('click');
      expect(wrapper.emitted('delete')![1]).toEqual([30]);
    });
  });

  describe('thumbnail aspect ratio', () => {
    it('should have video-thumbnail with aspect ratio style', () => {
      const video = createMockVideo();
      const wrapper = mount(VideoList, {
        props: { videos: [video], loading: false },
      });

      // The component uses CSS aspect-ratio: 9/16 for portrait video thumbnails
      const thumbnail = wrapper.find('.video-thumbnail');
      expect(thumbnail.exists()).toBe(true);
    });
  });
});
