---
metadata:
  proposal_id: PCP-2026-002
  proposal_type: product
  title: "视频自动字幕功能"
  status: draft
  created_at: 2026-01-16
  author: PM (Liangning)

linkage:
  requires_tech_change: true
  related_tech_proposal: null
  triggered_by: user_requirement
---

# PCP-2026-002: 视频自动字幕功能

## Change Summary

为合成的探店视频自动添加字幕功能，提升视频的可读性和观看体验。

**核心功能：**
- 基于AI生成的脚本自动生成时间轴对齐的字幕
- 采用双行滚动显示方式（当前行高亮 + 下一行预显示）
- 提供3-5种预设字幕样式模板供用户选择
- 字幕默认开启，用户可手动关闭
- 字幕烧录到成品视频中

## Change Background

### Trigger Reason
- [x] User requirement change
- [ ] Market feedback
- [ ] Resource constraint
- [ ] Technical limitation
- [ ] Strategic pivot

### Context

探店视频在抖音、小红书等平台发布时，带字幕的视频有以下优势：
1. **提升完播率**：用户在静音模式下也能理解内容
2. **增强信息传达**：配合语音双重强调关键信息
3. **符合平台趋势**：主流探店视频都带有字幕
4. **提升SEO效果**：平台可能通过字幕内容进行推荐

由于系统已经有AI生成的脚本和配音时长信息，字幕功能可以低成本实现。

## PRD Changes

### Affected PRD Sections

| Section | Change Type | Impact Level |
|---------|-------------|--------------|
| 2.1 Functional Requirements | add | MEDIUM |
| Epic 4 (配音合成与视频剪辑) | modify | HIGH |
| Story 4.3 (视频自动剪辑合成) | modify | HIGH |
| 新增 Story 4.5 (字幕设置UI) | add | MEDIUM |

### Detailed Changes

#### Section: 2.1 Functional Requirements (新增FR41-FR45)

**Current:**
```
FR36-FR40: 任务管理和用户认证相关功能
```

**Proposed (新增):**
```markdown
**视频字幕**
- FR41: 系统应基于AI生成的脚本自动生成时间轴对齐的字幕
- FR42: 字幕应采用双行滚动显示，当前行高亮，下一行预显示
- FR43: 系统应提供3-5种预设字幕样式模板供用户选择（简约白字、活力黄字、小红书风、抖音热门、霓虹炫彩）
- FR44: 字幕功能默认开启，用户可在配音设置页面手动关闭
- FR45: 字幕应烧录到成品视频中，位置固定在底部居中
```

#### Section: Epic 4 Story 4.3 (修改)

**Current:**
```yaml
- id: "4.3"
  title: "视频自动剪辑合成"
  acceptance_criteria:
    - AC1: 视频片段裁剪
    - AC2: 音视频合成
```

**Proposed (新增AC3):**
```yaml
- id: "4.3"
  title: "视频自动剪辑合成"
  acceptance_criteria:
    - AC1: 视频片段裁剪
    - AC2: 音视频合成
    - AC3: 字幕生成与烧录 (NEW)
```

#### Section: Epic 4 新增 Story 4.5

**Proposed:**
```yaml
- id: "4.5"
  title: "字幕设置UI组件"
  repository_type: monolith
  estimated_complexity: medium
  priority: P0

  acceptance_criteria:
    - AC1: 字幕开关控制
    - AC2: 字幕样式模板选择
```

## Impact Analysis

### MVP Scope Impact

- [ ] Expand scope → **选中**
- [ ] Reduce scope
- [ ] Maintain scope with modifications

**Current MVP:** 视频合成不含字幕
**Proposed MVP:** 视频合成自动添加字幕（可关闭）

### Epic Impact

| Epic ID | Epic Title | Impact Type |
|---------|------------|-------------|
| E4 | 配音合成与视频剪辑 | modify |

### Estimated Story Impact

- New stories: 1 (Story 4.5)
- Modified stories: 1 (Story 4.3)
- Removed stories: 0

## Story Requirements

> This section is used by SM when executing `*apply-proposal`

### Story Requirement 1: 修改 Story 4.3 - 新增字幕AC

```yaml
epic_id: 4
action: modify
story_id: "4.3"
title: "视频自动剪辑合成"
modification: |
  新增 AC3: 字幕生成与烧录

acceptance_criteria_addition:
  - id: AC3
    title: "字幕生成与烧录"
    scenario:
      given: "脚本分段和配音时长已确定"
      when: "系统合成视频"
      then:
        - "基于脚本分段生成ASS格式字幕文件"
        - "字幕时间轴与配音时长对齐"
        - "使用FFmpeg将字幕烧录到视频中"
        - "字幕采用双行滚动显示效果"
        - "应用用户选择的字幕样式模板"

    business_rules:
      - id: "BR-3.1"
        rule: "字幕时间轴基于每段脚本的配音起止时间自动计算"
      - id: "BR-3.2"
        rule: "双行滚动：当前播放行高亮显示，下一行预显示（半透明）"
      - id: "BR-3.3"
        rule: "字幕位置固定在视频底部居中"
      - id: "BR-3.4"
        rule: "如果用户关闭字幕，则跳过字幕烧录步骤"

    error_handling:
      - scenario: "字幕生成失败"
        code: "500"
        message: "字幕生成失败，视频将不含字幕"
        action: "记录日志，继续合成无字幕视频，不阻塞流程"

    examples:
      - input: |
          脚本段落：
          段落1: "家人们！今天给你们探一家望京超火的海底捞..." (0-8秒)
          段落2: "一进门就被这个装修惊艳到了..." (8-14秒)
        expected: |
          生成ASS字幕：
          Dialogue: 0,0:00:00.00,0:00:08.00,Default,,0,0,0,,家人们！今天给你们探一家望京超火的海底捞...
          Dialogue: 0,0:00:08.00,0:00:14.00,Default,,0,0,0,,一进门就被这个装修惊艳到了...

complexity_hints: "中等复杂度 - 需要生成ASS格式字幕并集成到FFmpeg流程"
```

### Story Requirement 2: 新增 Story 4.5 - 字幕设置UI

```yaml
epic_id: 4
action: create
suggested_story_id: "4.5"
title: "字幕设置UI组件"
description: |
  作为用户，我希望能在配音设置页面控制字幕的开关和选择字幕样式，
  以便根据个人偏好调整视频字幕效果。

acceptance_criteria:
  - id: AC1
    title: "字幕开关控制"
    scenario:
      given: "用户在配音设置页面"
      when: "用户查看字幕设置"
      then:
        - "显示字幕开关，默认开启状态"
        - "开关旁显示说明文字：'为视频添加字幕'"
        - "关闭时字幕样式选择区域置灰禁用"

    business_rules:
      - id: "BR-1.1"
        rule: "字幕开关默认为开启状态"
      - id: "BR-1.2"
        rule: "开关状态保存到任务配置中"

    interaction:
      - trigger: "切换开关"
        behavior: "开启时样式选择区域激活，关闭时置灰"

    error_handling:
      - scenario: "状态保存失败"
        code: "500"
        message: "设置保存失败，请重试"
        action: "显示Toast提示，保持原状态"

  - id: AC2
    title: "字幕样式模板选择"
    scenario:
      given: "字幕功能已开启"
      when: "用户选择字幕样式"
      then:
        - "显示5种预设样式模板卡片"
        - "每个卡片显示样式名称和预览效果"
        - "默认选中'简约白字'样式"
        - "点击卡片切换选中状态"

    business_rules:
      - id: "BR-2.1"
        rule: "预设模板：简约白字、活力黄字、小红书风、抖音热门、霓虹炫彩"
      - id: "BR-2.2"
        rule: "默认选中第一个模板（简约白字）"
      - id: "BR-2.3"
        rule: "样式选择保存到任务配置中"

    interaction:
      - trigger: "点击样式卡片"
        behavior: "选中该样式，显示选中边框"
      - trigger: "悬停样式卡片"
        behavior: "显示样式详细说明（字体、颜色等）"

    error_handling:
      - scenario: "样式加载失败"
        code: "LOAD_ERROR"
        message: "样式加载失败"
        action: "显示默认样式，记录日志"

provides_apis: []
consumes_apis:
  - "PUT /api/v1/tasks/{id}/subtitle-settings"
dependencies:
  - "4.4"
sm_hints:
  front_end_spec: null
  architecture: null

complexity_hints: "低-中复杂度 - 前端组件开发，样式预览需要视觉设计支持"
```

## Technical Change Requirements

### Requires Technical Change

- [x] Yes
- [ ] No

### Technical Change Overview

```yaml
tech_change_hints:
  - "字幕格式选择ASS（支持双行滚动和样式控制）"
  - "FFmpeg命令扩展：-vf subtitles=xxx.ass"
  - "预设样式模板存储：可使用配置文件或数据库"
  - "任务表新增字段：subtitle_enabled, subtitle_style"
  - "字幕生成服务：基于脚本分段和时长生成ASS文件"
```

### Linked Technical Proposal

> Populated by Architect after creating TCP

```yaml
related_tech_proposal: null
tech_proposal_status: pending
```

## Subtitle Style Templates (Reference)

| 模板ID | 名称 | 样式描述 |
|--------|------|----------|
| simple_white | 简约白字 | 白色字体(#FFFFFF) + 黑色描边(2px) + 无背景 |
| vibrant_yellow | 活力黄字 | 黄色字体(#FFD700) + 黑色阴影(45°) + 无背景 |
| xiaohongshu | 小红书风 | 粉色渐变字体 + 圆角半透明白色背景框 |
| douyin_classic | 抖音热门 | 白色字体 + 半透明黑色背景条(60%透明度) |
| neon_gradient | 霓虹炫彩 | 彩色渐变字体 + 外发光效果 + 无背景 |

## Cost Impact Analysis

| 项目 | 增量成本 | 说明 |
|------|----------|------|
| 计算资源 | +¥0.05/次 | FFmpeg字幕烧录增加少量处理时间 |
| 存储 | 可忽略 | ASS文件很小(<10KB) |
| **合计** | ~+¥0.05/次 | 成本影响很小 |

## Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| 字幕与画面重叠影响观看 | LOW | 字幕位于底部固定位置，用户可关闭 |
| 字幕样式在不同设备显示不一致 | LOW | 使用通用字体，烧录后为固定画面 |
| 长句字幕超出屏幕宽度 | MEDIUM | 自动换行处理，单行最大字符数限制 |
| 双行滚动实现复杂度 | MEDIUM | 使用ASS高级样式特性，需要充分测试 |

## Approval Record

| Date | Approver | Decision | Notes |
|------|----------|----------|-------|
| 2026-01-16 | - | pending | 等待用户确认 |
