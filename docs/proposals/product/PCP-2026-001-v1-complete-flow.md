---
metadata:
  proposal_id: PCP-2026-001
  proposal_type: product
  title: "V1.0实现完整端到端流程"
  status: applied
  created_at: 2026-01-14
  applied_at: 2026-01-14
  author: PM

linkage:
  requires_tech_change: true
  related_tech_proposal: TCP-2026-001
  triggered_by: user_requirement
---

# PCP-2026-001: V1.0实现完整端到端流程

## Change Summary

将原计划分阶段实现的功能整合到V1.0，实现从素材上传到成品视频导出的完整闭环：
- 店铺信息输入 → 批量上传视频 → AI分析优选镜头 → AI生成脚本 → **AI配音合成** → **视频自动剪辑** → **预览&导出成品MP4** → **热门话题&标题推荐**

新增功能：
- **抖音热门话题标签推荐**：生成视频后自动推荐相关热门标签
- **视频标题建议**：提供3-5个标题供用户选择复制
- **声音克隆**：支持用户上传自己的声音样本进行克隆配音

技术栈调整：
- 前端：Vue 3 + TypeScript
- 后端：Java 21 + Spring Cloud 微服务架构（为2.0 iOS版本做准备）

同时调整版本规划：V2.0定位为iOS会员订阅App。

## Change Background

### Trigger Reason
- [x] User requirement change
- [ ] Market feedback
- [ ] Resource constraint
- [ ] Technical limitation
- [ ] Strategic pivot

### Context

用户明确希望V1.0就能交付完整的产品体验，而不是分阶段交付半成品。核心诉求：

1. **完整闭环体验**：用户上传素材后，能直接获得可发布的成品视频
2. **降低使用门槛**：用户无需自行剪辑配音，AI全自动完成
3. **快速验证价值**：内测阶段(100-500用户)验证完整流程的市场价值
4. **后续规划清晰**：Web版验证成功后，2.0开发iOS会员订阅App

## PRD Changes

### Affected PRD Sections

| Section | Change Type | Impact Level |
|---------|-------------|--------------|
| MVP范围 | modify | HIGH |
| 用户流程设计 | modify | HIGH |
| 技术架构设计 | modify | HIGH |
| 核心处理流程 | modify | HIGH |
| 数据库设计 | modify | MEDIUM |
| API设计 | modify | MEDIUM |
| 成本估算 | modify | MEDIUM |
| 开发计划 | modify | MEDIUM |
| 后续迭代方向 | modify | LOW |

### Detailed Changes

#### Section: MVP范围 (1.2)

**Current:**
```
| 功能 | 状态 | 说明 |
|------|------|------|
| 视频批量上传 | ✅ MVP | 支持10-20个短视频 |
| 店铺信息输入 | ✅ MVP | 名称+类型+优惠描述 |
| AI镜头分析 | ✅ MVP | 内容识别+质量评分+分类 |
| AI脚本生成 | ✅ MVP | 基于镜头生成口播文案 |
| AI配音合成 | ❌ V2 | 后续迭代 |
| 视频自动剪辑 | ❌ V2 | 后续迭代 |
```

**Proposed:**
```
| 功能 | 状态 | 说明 |
|------|------|------|
| 视频批量上传 | ✅ MVP | 支持10-20个短视频 |
| 店铺信息输入 | ✅ MVP | 名称+类型+优惠描述 |
| AI镜头分析 | ✅ MVP | 内容识别+质量评分+分类 |
| AI脚本生成 | ✅ MVP | 基于镜头生成口播文案 |
| AI配音合成 | ✅ MVP | TTS云服务(阿里云/讯飞) |
| 视频自动剪辑 | ✅ MVP | FFmpeg自动拼接推荐镜头 |
| 预览播放 | ✅ MVP | 在线预览合成效果 |
| 导出成品 | ✅ MVP | 下载完整MP4视频 |
```

#### Section: 用户流程设计 (二)

**Current:** 4步流程 (填写信息 → 上传视频 → AI分析 → 查看结果)

**Proposed:** 6步完整流程
```
Step 1: 填写店铺信息
Step 2: 批量上传视频
Step 3: AI分析&优选镜头
Step 4: AI生成脚本
Step 5: AI配音&视频合成
Step 6: 预览&导出成品
```

#### Section: 后续迭代方向 (8.2)

**Current:**
```
- V2: AI配音合成（TTS）
- V3: 视频自动剪辑合成
- V4: 会员体系 + 付费功能
- V5: 移动端App
```

**Proposed:**
```
- V1.0 (MVP): 完整Web版 - 上传到成品视频全流程
- V1.x: 优化迭代 - 多音色选择、剪辑风格模板、批量任务
- V2.0: iOS会员订阅App - 移动端体验、会员体系、订阅付费
```

## Impact Analysis

### MVP Scope Impact

- [ ] Expand scope → **选中**
- [ ] Reduce scope
- [ ] Maintain scope with modifications

**Current MVP:** 上传 + AI分析 + 脚本生成 (输出文字脚本，用户自行配音剪辑)
**Proposed MVP:** 上传 + AI分析 + 脚本生成 + **AI配音** + **自动剪辑** + **预览导出** (输出完整成品视频)

### Epic Impact

| Epic ID | Epic Title | Impact Type |
|---------|------------|-------------|
| E1 | 用户认证与基础设施 | maintain |
| E2 | 视频上传与存储 | maintain |
| E3 | AI镜头分析 | maintain |
| E4 | AI脚本生成 | maintain |
| E5 | AI配音合成 | **create (NEW)** - 含声音克隆 |
| E6 | 视频自动剪辑合成 | **create (NEW)** |
| E7 | 预览与导出 | **create (NEW)** |
| E8 | 发布辅助(话题&标题) | **create (NEW)** |

### Estimated Story Impact

- New stories: ~18-22
- Modified stories: ~3-5
- Removed stories: 0

## Story Requirements

> This section is used by SM when executing `*apply-proposal`

### Story Requirement 1: TTS服务集成

```yaml
epic_id: E5
action: create
suggested_story_id: E5.1
title: "集成TTS云服务实现脚本配音"
description: |
  作为系统，需要将AI生成的口播脚本转换为语音音频，
  支持阿里云语音合成或讯飞TTS服务。
acceptance_criteria_hints:
  - 支持将文本脚本转换为MP3/WAV音频
  - 支持选择不同音色(默认提供2-3种)
  - 音频时长与脚本长度匹配
  - 处理失败时有重试机制
complexity_hints: "中等复杂度 - 需要对接第三方API"
```

### Story Requirement 2: 视频片段剪辑

```yaml
epic_id: E6
action: create
suggested_story_id: E6.1
title: "根据脚本自动裁剪视频片段"
description: |
  作为系统，需要根据脚本段落和对应镜头，
  从原始视频中裁剪出合适时长的片段。
acceptance_criteria_hints:
  - 根据配音时长确定片段长度
  - 从推荐镜头中提取视频片段
  - 支持片段的起始时间调整
complexity_hints: "中等复杂度 - FFmpeg命令处理"
```

### Story Requirement 3: 视频音频合成

```yaml
epic_id: E6
action: create
suggested_story_id: E6.2
title: "合并视频片段与配音音频"
description: |
  作为系统，需要将裁剪的视频片段与TTS生成的配音
  合并为完整的成品视频。
acceptance_criteria_hints:
  - 视频片段按脚本顺序拼接
  - 配音音频与视频画面同步
  - 输出MP4格式，分辨率保持原始质量
  - 支持添加简单转场效果(可选)
complexity_hints: "中等复杂度 - FFmpeg视频合成"
```

### Story Requirement 4: 成品预览功能

```yaml
epic_id: E7
action: create
suggested_story_id: E7.1
title: "实现成品视频在线预览"
description: |
  作为用户，我希望在导出前能预览合成的视频效果，
  确认满意后再下载。
acceptance_criteria_hints:
  - 视频播放器组件(支持播放/暂停/进度条)
  - 预览视频可以全屏播放
  - 显示视频时长和当前进度
complexity_hints: "低复杂度 - 前端播放器组件"
```

### Story Requirement 5: 视频导出下载

```yaml
epic_id: E7
action: create
suggested_story_id: E7.2
title: "实现成品视频下载导出"
description: |
  作为用户，我希望能下载合成好的成品视频到本地，
  用于发布到抖音/小红书等平台。
acceptance_criteria_hints:
  - 提供下载按钮，点击即可下载
  - 显示下载进度(大文件)
  - 文件名包含店铺名称和日期
  - 支持下载原始推荐镜头素材包(可选)
complexity_hints: "低复杂度 - 文件下载服务"
```

### Story Requirement 6: 抖音热门话题推荐

```yaml
epic_id: E8
action: create
suggested_story_id: E8.1
title: "AI生成抖音热门话题标签推荐"
description: |
  作为用户，我希望在视频生成完成后，系统能推荐与我的视频内容相关的
  抖音热门话题标签，方便我发布时直接复制使用，提升视频曝光。
acceptance_criteria_hints:
  - 基于店铺类型、脚本内容生成相关话题标签
  - 推荐5-10个热门标签，按热度排序
  - 支持一键复制全部标签或单个标签
  - 标签格式符合抖音规范(#话题名#)
complexity_hints: "中等复杂度 - 需要调用豆包API分析内容生成标签"
```

### Story Requirement 7: 视频标题建议

```yaml
epic_id: E8
action: create
suggested_story_id: E8.2
title: "AI生成视频标题建议"
description: |
  作为用户，我希望系统能为我的视频提供3-5个吸引眼球的标题建议，
  我可以选择一个直接复制使用或作为参考。
acceptance_criteria_hints:
  - 基于店铺信息和脚本内容生成3-5个标题
  - 标题风格符合抖音/小红书爆款特征
  - 支持点击复制单个标题
  - 标题长度控制在20-50字
complexity_hints: "中等复杂度 - 需要调用豆包API生成标题"
```

### Story Requirement 8: 声音克隆上传

```yaml
epic_id: E5
action: create
suggested_story_id: E5.2
title: "支持用户上传声音样本进行克隆"
description: |
  作为用户，我希望能上传自己的声音样本，让AI克隆我的声音进行配音，
  使视频更有个人特色。
acceptance_criteria_hints:
  - 支持上传30秒-2分钟的声音样本(MP3/WAV)
  - 提供录音指导(安静环境、清晰朗读)
  - 声音样本处理后可重复使用
  - 克隆音色保存在用户账户下
  - 配音时可选择"我的声音"
complexity_hints: "高复杂度 - 需要对接阿里云声音克隆API"
```

### Story Requirement 9: 声音克隆配音

```yaml
epic_id: E5
action: create
suggested_story_id: E5.3
title: "使用克隆声音进行脚本配音"
description: |
  作为用户，我希望在配音时能选择使用之前克隆的"我的声音"，
  让配音效果更自然、更有个人特色。
acceptance_criteria_hints:
  - 音色选择器中显示"我的声音"选项(如已克隆)
  - 克隆声音与标准音色切换方便
  - 首次使用提示用户先上传声音样本
  - 克隆配音质量可接受，语调自然
complexity_hints: "中等复杂度 - 调用已克隆音色进行TTS"
```

## Technical Change Requirements

### Requires Technical Change

- [x] Yes
- [ ] No

### Technical Change Overview

```yaml
tech_stack_changes:
  frontend:
    from: "React 18 + TypeScript"
    to: "Vue 3 + TypeScript + Vite"
    reason: "团队技术栈熟悉度"
  backend:
    from: "FastAPI (Python) + Celery"
    to: "Java 21 + Spring Cloud 微服务架构"
    reason: "企业级架构，便于2.0 iOS版本扩展"
  ai_services:
    vision: "通义千问VL"
    text: "豆包 (ByteDance)"
    tts: "阿里云语音合成"
  storage: "阿里云OSS"

tech_change_hints:
  - "微服务架构设计: API Gateway + 业务服务拆分"
  - "集成阿里云TTS: 支持标准音色 + 声音克隆"
  - "FFmpeg视频处理服务: 独立微服务"
  - "豆包API集成: 脚本生成 + 话题/标题推荐"
  - "通义千问VL集成: 镜头分析"
  - "阿里云OSS: 视频存储 + CDN加速"
  - "数据库设计: 考虑iOS 2.0多端同步"
  - "API设计: RESTful + 版本化，支持移动端"
```

### Linked Technical Proposal

```yaml
related_tech_proposal: TCP-2026-001
tech_proposal_title: "微服务架构设计 - Vue3 + Java21 + Spring Cloud"
tech_proposal_status: draft
tech_proposal_path: docs/proposals/tech/TCP-2026-001-microservices-architecture.md
```

## Cost Impact Analysis

### 单次任务成本变化

| 项目 | 原成本 | 新增成本 | 新总成本 |
|------|--------|----------|----------|
| 视频存储 | ¥0.12 | +¥0.10 (成品视频) | ¥0.22 |
| AI视觉分析 | ¥2.00 | - | ¥2.00 |
| LLM脚本生成 | ¥0.02 | - | ¥0.02 |
| **TTS配音** | - | **+¥0.10** | ¥0.10 |
| **视频合成计算** | - | **+¥0.50** | ¥0.50 |
| 服务器计算 | ¥0.30 | +¥0.20 | ¥0.50 |
| **合计** | ¥2.5/次 | +¥0.90 | **~¥3.4/次** |

### 定价建议调整

考虑成本增加36%，建议：
- 免费体验: 2次 (原3次)
- 基础包: ¥39/20次 (原¥29)
- 月卡: ¥99/100次 (原¥79)

## Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| TTS音质不满足用户期望 | MEDIUM | 提供多音色选择，后续可升级高质量TTS |
| 视频合成耗时过长 | MEDIUM | 优化FFmpeg参数，使用GPU加速(可选) |
| 成品视频文件过大 | LOW | 压缩参数优化，提供不同质量选项 |
| 开发周期延长 | MEDIUM | 内测阶段优先核心流程，细节功能后续迭代 |

## Approval Record

| Date | Approver | Decision | Notes |
|------|----------|----------|-------|
| 2026-01-14 | User | approved | 用户确认产品方案 |
| 2026-01-14 | SM | applied | Stories created: E5.1-E5.3, E6.1-E6.2, E7.1-E7.2, E8.1-E8.2 |
