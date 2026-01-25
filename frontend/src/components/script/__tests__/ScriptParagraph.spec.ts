/**
 * ScriptParagraph component tests.
 * Story 3.2: 脚本编辑页面 - T1
 *
 * Test coverage:
 * - 3.2-UNIT-Paragraph-001: Paragraph rendering with shot label
 * - 3.2-UNIT-Paragraph-002: Edit mode toggle on click
 * - 3.2-UNIT-Paragraph-003: Character count display
 * - 3.2-UNIT-Paragraph-004: Character count color based on limit
 * - 3.2-UNIT-Paragraph-005: Validation error display
 * - 3.2-UNIT-Paragraph-006: Save/cancel buttons in edit mode
 * - 3.2-UNIT-Paragraph-007: Empty content validation (BLIND)
 * - 3.2-UNIT-Paragraph-008: 500 char boundary test (BLIND)
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import ScriptParagraph from '../ScriptParagraph.vue';
import type { ScriptParagraph as ScriptParagraphType, CharCountInfo } from '../../../types/script';
import { SCRIPT_VALIDATION } from '../../../types/script';

/**
 * Helper to create mock paragraph.
 */
function createMockParagraph(overrides: Partial<ScriptParagraphType> = {}): ScriptParagraphType {
  return {
    id: 'para_1',
    section: '开场',
    shotId: 101,
    text: '这是一段测试脚本内容',
    estimatedDuration: 10,
    ...overrides
  };
}

/**
 * Helper to create mock char count info.
 */
function createCharCountInfo(overrides: Partial<CharCountInfo> = {}): CharCountInfo {
  return {
    current: 20,
    suggested: 40,
    isOverLimit: false,
    ...overrides
  };
}

describe('ScriptParagraph', () => {
  const defaultProps = {
    paragraph: createMockParagraph(),
    currentText: '这是一段测试脚本内容',
    isEdited: false,
    isEditing: false,
    editText: '',
    charCountInfo: createCharCountInfo()
  };

  describe('rendering', () => {
    it('should display shot label with section and shotId', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          paragraph: createMockParagraph({ section: '环境展示', shotId: 102 })
        }
      });

      expect(wrapper.text()).toContain('【环境展示】镜头#102');
    });

    it('should display current text in view mode', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          currentText: '显示的文案内容',
          isEditing: false
        }
      });

      expect(wrapper.text()).toContain('显示的文案内容');
    });

    it('should display character count', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          charCountInfo: createCharCountInfo({ current: 25, suggested: 40 })
        }
      });

      expect(wrapper.text()).toContain('25/40字');
    });

    it('should display "已修改" indicator when paragraph is edited', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEdited: true,
          isEditing: false
        }
      });

      expect(wrapper.text()).toContain('已修改');
    });

    it('should apply yellow border when edited but not editing', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEdited: true,
          isEditing: false
        }
      });

      const container = wrapper.find('.script-paragraph');
      expect(container.classes()).toContain('border-yellow-300');
    });

    it('should apply blue border when editing', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: '编辑中的内容'
        }
      });

      const container = wrapper.find('.script-paragraph');
      expect(container.classes()).toContain('border-blue-500');
    });
  });

  describe('edit mode', () => {
    it('should show textarea in edit mode', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: '编辑中的内容'
        }
      });

      expect(wrapper.find('textarea').exists()).toBe(true);
    });

    it('should not show textarea in view mode', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: false
        }
      });

      expect(wrapper.find('textarea').exists()).toBe(false);
    });

    it('should emit startEdit when clicking in view mode', async () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: false
        }
      });

      await wrapper.find('.p-4').trigger('click');

      expect(wrapper.emitted('startEdit')).toBeTruthy();
    });

    it('should not emit startEdit when already editing', async () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: '内容'
        }
      });

      await wrapper.find('.p-4').trigger('click');

      expect(wrapper.emitted('startEdit')).toBeFalsy();
    });

    it('should show save and cancel buttons in edit mode', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: '编辑内容'
        }
      });

      const buttons = wrapper.findAll('button[type="button"]');
      expect(buttons.length).toBeGreaterThanOrEqual(2);
      expect(wrapper.text()).toContain('取消');
      expect(wrapper.text()).toContain('确定');
    });
  });

  describe('text input', () => {
    it('should emit updateText on input', async () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: '初始内容'
        }
      });

      const textarea = wrapper.find('textarea');
      await textarea.setValue('新的内容');

      expect(wrapper.emitted('updateText')).toBeTruthy();
      expect(wrapper.emitted('updateText')![0]).toEqual(['新的内容']);
    });

    it('should display editText value in textarea', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: '当前编辑的内容'
        }
      });

      const textarea = wrapper.find('textarea');
      expect(textarea.element.value).toBe('当前编辑的内容');
    });
  });

  describe('save and cancel', () => {
    it('should emit commitEdit when clicking save button', async () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: '有效内容',
          charCountInfo: createCharCountInfo({ isOverLimit: false })
        }
      });

      const saveButton = wrapper.findAll('button[type="button"]').find(b => b.text() === '确定');
      await saveButton!.trigger('click');

      expect(wrapper.emitted('commitEdit')).toBeTruthy();
    });

    it('should emit cancelEdit when clicking cancel button', async () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: '内容'
        }
      });

      const cancelButton = wrapper.findAll('button[type="button"]').find(b => b.text() === '取消');
      await cancelButton!.trigger('click');

      expect(wrapper.emitted('cancelEdit')).toBeTruthy();
    });

    it('should emit commitEdit on Enter key (without Shift)', async () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: '有效内容'
        }
      });

      const textarea = wrapper.find('textarea');
      await textarea.trigger('keydown', { key: 'Enter', shiftKey: false });

      expect(wrapper.emitted('commitEdit')).toBeTruthy();
    });

    it('should not emit commitEdit on Shift+Enter', async () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: '有效内容'
        }
      });

      const textarea = wrapper.find('textarea');
      await textarea.trigger('keydown', { key: 'Enter', shiftKey: true });

      expect(wrapper.emitted('commitEdit')).toBeFalsy();
    });
  });

  describe('validation', () => {
    it('should show error message when text is empty', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: ''
        }
      });

      expect(wrapper.text()).toContain('不能为空');
    });

    it('should show error when text is only whitespace', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: '   '
        }
      });

      expect(wrapper.text()).toContain('不能为空');
    });

    it('should show error when text exceeds max length', () => {
      const longText = 'a'.repeat(SCRIPT_VALIDATION.MAX_PARAGRAPH_LENGTH + 1);
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: longText,
          charCountInfo: createCharCountInfo({ current: 501, isOverLimit: true })
        }
      });

      expect(wrapper.text()).toContain(`不能超过${SCRIPT_VALIDATION.MAX_PARAGRAPH_LENGTH}字`);
    });

    it('should disable save button when validation fails', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: ''
        }
      });

      const saveButton = wrapper.findAll('button[type="button"]').find(b => b.text() === '确定');
      expect(saveButton!.attributes('disabled')).toBeDefined();
    });

    it('should not disable save button when validation passes', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: '有效的脚本内容'
        }
      });

      const saveButton = wrapper.findAll('button[type="button"]').find(b => b.text() === '确定');
      expect(saveButton!.attributes('disabled')).toBeUndefined();
    });

    it('should apply red border on textarea when validation fails', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: ''
        }
      });

      const textarea = wrapper.find('textarea');
      expect(textarea.classes()).toContain('border-red-500');
    });

    it('should accept exactly 500 characters (boundary test)', () => {
      const exactText = 'a'.repeat(500);
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: exactText,
          charCountInfo: createCharCountInfo({ current: 500, isOverLimit: false })
        }
      });

      // No error message should be shown
      expect(wrapper.text()).not.toContain('不能超过');

      const saveButton = wrapper.findAll('button[type="button"]').find(b => b.text() === '确定');
      expect(saveButton!.attributes('disabled')).toBeUndefined();
    });

    it('should reject 501 characters (boundary test)', () => {
      const overText = 'a'.repeat(501);
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: overText,
          charCountInfo: createCharCountInfo({ current: 501, isOverLimit: true })
        }
      });

      expect(wrapper.text()).toContain(`不能超过${SCRIPT_VALIDATION.MAX_PARAGRAPH_LENGTH}字`);
    });
  });

  describe('character count styling', () => {
    it('should show gray text for normal character count', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          charCountInfo: createCharCountInfo({ current: 30, suggested: 40, isOverLimit: false })
        }
      });

      const charCountSpan = wrapper.findAll('span').find(s => s.text().includes('/40字'));
      expect(charCountSpan!.classes()).toContain('text-gray-400');
    });

    it('should show red text when over limit', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: 'a'.repeat(501),
          charCountInfo: createCharCountInfo({ current: 501, suggested: 40, isOverLimit: true })
        }
      });

      const charCountSpan = wrapper.findAll('span').find(s => s.text().includes('/40字'));
      expect(charCountSpan!.classes()).toContain('text-red-500');
    });

    it('should show yellow text when exceeding suggested by 20%', () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          charCountInfo: createCharCountInfo({ current: 50, suggested: 40, isOverLimit: false })
        }
      });

      const charCountSpan = wrapper.findAll('span').find(s => s.text().includes('/40字'));
      expect(charCountSpan!.classes()).toContain('text-yellow-500');
    });
  });

  describe('not emit save on invalid', () => {
    it('should not emit commitEdit when clicking save with empty text', async () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: ''
        }
      });

      const saveButton = wrapper.findAll('button[type="button"]').find(b => b.text() === '确定');
      await saveButton!.trigger('click');

      expect(wrapper.emitted('commitEdit')).toBeFalsy();
    });

    it('should not emit commitEdit on Enter when text is invalid', async () => {
      const wrapper = mount(ScriptParagraph, {
        props: {
          ...defaultProps,
          isEditing: true,
          editText: ''
        }
      });

      const textarea = wrapper.find('textarea');
      await textarea.trigger('keydown', { key: 'Enter', shiftKey: false });

      expect(wrapper.emitted('commitEdit')).toBeFalsy();
    });
  });
});
