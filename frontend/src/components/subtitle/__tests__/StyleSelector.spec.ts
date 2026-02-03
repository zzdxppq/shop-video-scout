/**
 * Tests for StyleSelector component.
 * Story 4.5: 字幕设置页面
 *
 * Test IDs: 4.5-UNIT-018 to 4.5-UNIT-022
 */
import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import StyleSelector from '../StyleSelector.vue';
import StyleCard from '../StyleCard.vue';
import { SUBTITLE_STYLES } from '../../../types/subtitle';

describe('StyleSelector', () => {
  // 4.5-UNIT-018: Selector renders all 5 style cards
  it('should render 5 StyleCard components', () => {
    const wrapper = mount(StyleSelector, {
      props: { selectedStyle: 'simple_white' }
    });

    const cards = wrapper.findAllComponents(StyleCard);
    expect(cards).toHaveLength(5);
  });

  it('should render all styles from SUBTITLE_STYLES', () => {
    const wrapper = mount(StyleSelector, {
      props: { selectedStyle: 'simple_white' }
    });

    SUBTITLE_STYLES.forEach(style => {
      expect(wrapper.text()).toContain(style.name);
    });
  });

  // 4.5-UNIT-019: Selector passes selected to correct card
  it('should pass selected=true only to the selected style card', () => {
    const wrapper = mount(StyleSelector, {
      props: { selectedStyle: 'neon' }
    });

    const cards = wrapper.findAllComponents(StyleCard);

    cards.forEach(card => {
      const styleId = card.props('style').id;
      if (styleId === 'neon') {
        expect(card.props('selected')).toBe(true);
      } else {
        expect(card.props('selected')).toBe(false);
      }
    });
  });

  // 4.5-UNIT-020: Selector disabled prop propagates
  it('should pass disabled=true to all cards when disabled', () => {
    const wrapper = mount(StyleSelector, {
      props: { selectedStyle: 'simple_white', disabled: true }
    });

    const cards = wrapper.findAllComponents(StyleCard);
    cards.forEach(card => {
      expect(card.props('disabled')).toBe(true);
    });
  });

  it('should pass disabled=false to all cards when enabled', () => {
    const wrapper = mount(StyleSelector, {
      props: { selectedStyle: 'simple_white', disabled: false }
    });

    const cards = wrapper.findAllComponents(StyleCard);
    cards.forEach(card => {
      expect(card.props('disabled')).toBe(false);
    });
  });

  // 4.5-UNIT-021: Selector emits update:selectedStyle
  it('should emit update:selectedStyle when card emits select', async () => {
    const wrapper = mount(StyleSelector, {
      props: { selectedStyle: 'simple_white' }
    });

    const cards = wrapper.findAllComponents(StyleCard);
    const neonCard = cards.find(c => c.props('style').id === 'neon');

    await neonCard!.vm.$emit('select', 'neon');

    expect(wrapper.emitted('update:selectedStyle')).toBeTruthy();
    expect(wrapper.emitted('update:selectedStyle')![0]).toEqual(['neon']);
  });

  // 4.5-UNIT-022: Selector default selection is simple_white
  it('should default to simple_white when no selectedStyle provided', () => {
    const wrapper = mount(StyleSelector);

    const cards = wrapper.findAllComponents(StyleCard);
    const simpleWhiteCard = cards.find(c => c.props('style').id === 'simple_white');

    expect(simpleWhiteCard!.props('selected')).toBe(true);
  });

  it('should not emit when disabled and card clicked', async () => {
    const wrapper = mount(StyleSelector, {
      props: { selectedStyle: 'simple_white', disabled: true }
    });

    // Cards should not emit when disabled, so no event should propagate
    const cards = wrapper.findAllComponents(StyleCard);
    await cards[1].find('[role="button"]').trigger('click');

    expect(wrapper.emitted('update:selectedStyle')).toBeFalsy();
  });

  // Accessibility
  it('should have listbox role', () => {
    const wrapper = mount(StyleSelector, {
      props: { selectedStyle: 'simple_white' }
    });

    const listbox = wrapper.find('[role="listbox"]');
    expect(listbox.exists()).toBe(true);
  });

  it('should display section title', () => {
    const wrapper = mount(StyleSelector, {
      props: { selectedStyle: 'simple_white' }
    });

    expect(wrapper.text()).toContain('字幕样式');
  });
});
