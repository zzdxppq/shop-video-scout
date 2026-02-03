/**
 * Tests for StyleCard component.
 * Story 4.5: 字幕设置页面
 *
 * Test IDs: 4.5-UNIT-008 to 4.5-UNIT-017
 */
import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import StyleCard from '../StyleCard.vue';
import type { SubtitleStyle } from '../../../types/subtitle';

const mockStyle: SubtitleStyle = {
  id: 'simple_white',
  name: '简约白字',
  previewUrl: '/images/subtitle-styles/simple_white.png'
};

describe('StyleCard', () => {
  // 4.5-UNIT-008: Card renders style name
  it('should display style name', () => {
    const wrapper = mount(StyleCard, {
      props: { style: mockStyle }
    });

    expect(wrapper.text()).toContain('简约白字');
  });

  // 4.5-UNIT-009: Card renders preview image
  it('should render preview image with correct src', () => {
    const wrapper = mount(StyleCard, {
      props: { style: mockStyle }
    });

    const img = wrapper.find('img');
    expect(img.exists()).toBe(true);
    expect(img.attributes('src')).toBe('/images/subtitle-styles/simple_white.png');
    expect(img.attributes('alt')).toContain('简约白字');
  });

  // 4.5-UNIT-010: Card selected state shows border
  it('should have blue border when selected=true', () => {
    const wrapper = mount(StyleCard, {
      props: { style: mockStyle, selected: true }
    });

    const card = wrapper.find('[role="button"]');
    expect(card.classes()).toContain('border-blue-500');
  });

  // 4.5-UNIT-011: Card unselected state no border
  it('should have gray border when selected=false', () => {
    const wrapper = mount(StyleCard, {
      props: { style: mockStyle, selected: false }
    });

    const card = wrapper.find('[role="button"]');
    expect(card.classes()).toContain('border-gray-200');
  });

  // 4.5-UNIT-012: Card emits select on click
  it('should emit select with style.id on click', async () => {
    const wrapper = mount(StyleCard, {
      props: { style: mockStyle }
    });

    await wrapper.find('[role="button"]').trigger('click');

    expect(wrapper.emitted('select')).toBeTruthy();
    expect(wrapper.emitted('select')![0]).toEqual(['simple_white']);
  });

  // 4.5-UNIT-013: Card disabled state grayscale
  it('should have grayscale filter when disabled=true', () => {
    const wrapper = mount(StyleCard, {
      props: { style: mockStyle, disabled: true }
    });

    const card = wrapper.find('[role="button"]');
    expect(card.classes()).toContain('grayscale');
  });

  // 4.5-UNIT-014: Card disabled prevents click
  it('should not emit select when disabled=true and clicked', async () => {
    const wrapper = mount(StyleCard, {
      props: { style: mockStyle, disabled: true }
    });

    await wrapper.find('[role="button"]').trigger('click');

    expect(wrapper.emitted('select')).toBeFalsy();
  });

  // 4.5-UNIT-015: Card hover effect when enabled
  it('should have hover:scale class when enabled', () => {
    const wrapper = mount(StyleCard, {
      props: { style: mockStyle, disabled: false }
    });

    const card = wrapper.find('[role="button"]');
    expect(card.classes()).toContain('hover:scale-[1.02]');
  });

  // 4.5-UNIT-016: Card no hover effect when disabled
  it('should not have hover:scale class when disabled', () => {
    const wrapper = mount(StyleCard, {
      props: { style: mockStyle, disabled: true }
    });

    const card = wrapper.find('[role="button"]');
    expect(card.classes()).not.toContain('hover:scale-[1.02]');
  });

  // 4.5-UNIT-017: Card image fallback on error
  it('should show placeholder on image error', async () => {
    const wrapper = mount(StyleCard, {
      props: { style: mockStyle }
    });

    const img = wrapper.find('img');
    await img.trigger('error');

    // Image should be hidden, placeholder shown
    expect(wrapper.find('img').exists()).toBe(false);
    expect(wrapper.text()).toContain('简约白字');
  });

  // Selected checkmark
  it('should show checkmark when selected', () => {
    const wrapper = mount(StyleCard, {
      props: { style: mockStyle, selected: true }
    });

    const checkmark = wrapper.find('svg');
    expect(checkmark.exists()).toBe(true);
  });

  it('should not show checkmark when not selected', () => {
    const wrapper = mount(StyleCard, {
      props: { style: mockStyle, selected: false }
    });

    // Only checkmark svg, not other svgs
    const checkmarkContainer = wrapper.find('.bg-blue-500.rounded-full');
    expect(checkmarkContainer.exists()).toBe(false);
  });

  // Keyboard accessibility
  it('should emit select on Enter key', async () => {
    const wrapper = mount(StyleCard, {
      props: { style: mockStyle }
    });

    await wrapper.find('[role="button"]').trigger('keydown', { key: 'Enter' });

    expect(wrapper.emitted('select')).toBeTruthy();
  });

  it('should have tabindex=0 when enabled', () => {
    const wrapper = mount(StyleCard, {
      props: { style: mockStyle, disabled: false }
    });

    const card = wrapper.find('[role="button"]');
    expect(card.attributes('tabindex')).toBe('0');
  });

  it('should have tabindex=-1 when disabled', () => {
    const wrapper = mount(StyleCard, {
      props: { style: mockStyle, disabled: true }
    });

    const card = wrapper.find('[role="button"]');
    expect(card.attributes('tabindex')).toBe('-1');
  });
});
