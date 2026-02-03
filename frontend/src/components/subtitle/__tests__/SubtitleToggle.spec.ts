/**
 * Tests for SubtitleToggle component.
 * Story 4.5: 字幕设置页面
 *
 * Test IDs: 4.5-UNIT-001 to 4.5-UNIT-007
 */
import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import SubtitleToggle from '../SubtitleToggle.vue';

describe('SubtitleToggle', () => {
  // 4.5-UNIT-001: Toggle renders enabled state
  it('should render in ON position when enabled=true', () => {
    const wrapper = mount(SubtitleToggle, {
      props: { enabled: true }
    });

    const toggle = wrapper.find('button[role="switch"]');
    expect(toggle.attributes('aria-checked')).toBe('true');
    expect(toggle.classes()).toContain('bg-blue-500');
  });

  // 4.5-UNIT-002: Toggle renders disabled state
  it('should render in OFF position when enabled=false', () => {
    const wrapper = mount(SubtitleToggle, {
      props: { enabled: false }
    });

    const toggle = wrapper.find('button[role="switch"]');
    expect(toggle.attributes('aria-checked')).toBe('false');
    expect(toggle.classes()).toContain('bg-gray-200');
  });

  // 4.5-UNIT-003: Toggle emits update:enabled on click
  it('should emit update:enabled with !currentValue on click', async () => {
    const wrapper = mount(SubtitleToggle, {
      props: { enabled: true }
    });

    await wrapper.find('button[role="switch"]').trigger('click');

    expect(wrapper.emitted('update:enabled')).toBeTruthy();
    expect(wrapper.emitted('update:enabled')![0]).toEqual([false]);
  });

  it('should emit true when enabled=false and clicked', async () => {
    const wrapper = mount(SubtitleToggle, {
      props: { enabled: false }
    });

    await wrapper.find('button[role="switch"]').trigger('click');

    expect(wrapper.emitted('update:enabled')![0]).toEqual([true]);
  });

  // 4.5-UNIT-004: Toggle shows label text
  it('should display "为视频添加字幕" label', () => {
    const wrapper = mount(SubtitleToggle, {
      props: { enabled: true }
    });

    expect(wrapper.text()).toContain('为视频添加字幕');
  });

  // 4.5-UNIT-005: Toggle shows loading spinner
  it('should display spinner when loading=true', () => {
    const wrapper = mount(SubtitleToggle, {
      props: { enabled: true, loading: true }
    });

    const spinner = wrapper.find('svg.animate-spin');
    expect(spinner.exists()).toBe(true);
  });

  // 4.5-UNIT-006: Toggle disabled during loading
  it('should not emit when loading=true and clicked', async () => {
    const wrapper = mount(SubtitleToggle, {
      props: { enabled: true, loading: true }
    });

    await wrapper.find('button[role="switch"]').trigger('click');

    expect(wrapper.emitted('update:enabled')).toBeFalsy();
  });

  // 4.5-UNIT-007: Toggle disabled prop prevents interaction
  it('should not emit when disabled=true and clicked', async () => {
    const wrapper = mount(SubtitleToggle, {
      props: { enabled: true, disabled: true }
    });

    await wrapper.find('button[role="switch"]').trigger('click');

    expect(wrapper.emitted('update:enabled')).toBeFalsy();
  });

  it('should have cursor-not-allowed when disabled', () => {
    const wrapper = mount(SubtitleToggle, {
      props: { enabled: true, disabled: true }
    });

    const toggle = wrapper.find('button[role="switch"]');
    expect(toggle.classes()).toContain('cursor-not-allowed');
  });

  // Keyboard accessibility
  it('should toggle on Enter key press', async () => {
    const wrapper = mount(SubtitleToggle, {
      props: { enabled: true }
    });

    await wrapper.find('button[role="switch"]').trigger('keydown', { key: 'Enter' });

    expect(wrapper.emitted('update:enabled')).toBeTruthy();
  });

  it('should toggle on Space key press', async () => {
    const wrapper = mount(SubtitleToggle, {
      props: { enabled: true }
    });

    await wrapper.find('button[role="switch"]').trigger('keydown', { key: ' ' });

    expect(wrapper.emitted('update:enabled')).toBeTruthy();
  });
});
