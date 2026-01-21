# Stories Index

> Auto-generated: 2026-01-16

## Overview

| Status | Count |
|--------|-------|
| Approved | 6 |
| AwaitingTestDesign | 9 |
| **Total** | **15** |

---

## Epic 1: 基础设施

| ID | Story | Status | File |
|----|-------|--------|------|
| 1.1 | 搭建前后端项目脚手架 | Approved | [1.1.project-scaffold.md](1.1.project-scaffold.md) |
| 1.2 | 实现API网关路由和认证 | Approved | [1.2.api-gateway.md](1.2.api-gateway.md) |
| 1.3 | 实现用户认证和基础功能 | Approved | [1.3.user-service.md](1.3.user-service.md) |

## Epic 3: AI镜头分析

| ID | Story | Status | File |
|----|-------|--------|------|
| 3.1 | 集成通义千问VL实现镜头分析 | Approved | [3.1.qwen-vl-integration.md](3.1.qwen-vl-integration.md) |

## Epic 4: AI脚本生成

| ID | Story | Status | File |
|----|-------|--------|------|
| 4.1 | 集成豆包API实现脚本生成 | AwaitingTestDesign | [4.1.doubao-script-generation.md](4.1.doubao-script-generation.md) |

## Epic 5: AI配音合成

| ID | Story | Status | File |
|----|-------|--------|------|
| 5.1 | 集成阿里云TTS实现脚本配音 | AwaitingTestDesign | [5.1.aliyun-tts-integration.md](5.1.aliyun-tts-integration.md) |
| 5.2 | 支持用户上传声音样本进行克隆 | AwaitingTestDesign | [5.2.voice-clone-upload.md](5.2.voice-clone-upload.md) |
| 5.3 | 使用克隆声音进行脚本配音 | AwaitingTestDesign | [5.3.voice-clone-tts.md](5.3.voice-clone-tts.md) |

## Epic 6: 视频剪辑合成

| ID | Story | Status | File |
|----|-------|--------|------|
| 6.1 | 根据脚本自动裁剪视频片段 | AwaitingTestDesign | [6.1.video-segment-cutting.md](6.1.video-segment-cutting.md) |
| 6.2 | 合并视频片段与配音音频 | AwaitingTestDesign | [6.2.video-audio-merge.md](6.2.video-audio-merge.md) |

## Epic 7: 预览与导出

| ID | Story | Status | File |
|----|-------|--------|------|
| 7.1 | 实现成品视频在线预览 | Approved | [7.1.video-preview.md](7.1.video-preview.md) |
| 7.2 | 实现成品视频下载导出 | Approved | [7.2.video-export.md](7.2.video-export.md) |
| 7.3 | 实现前端核心页面和流程 | AwaitingTestDesign | [7.3.frontend-core-pages.md](7.3.frontend-core-pages.md) |

## Epic 8: 发布辅助

| ID | Story | Status | File |
|----|-------|--------|------|
| 8.1 | AI生成抖音热门话题标签推荐 | AwaitingTestDesign | [8.1.hot-topics-recommendation.md](8.1.hot-topics-recommendation.md) |
| 8.2 | AI生成视频标题建议 | AwaitingTestDesign | [8.2.title-suggestions.md](8.2.title-suggestions.md) |

---

## Related Documents

- **QA Assessments**: [../qa/assessments/](../qa/assessments/)
- **Dev Logs**: [../dev/logs/](../dev/logs/)

## Naming Convention

- **Story**: `{epic}.{story}.{title-slug}.md` (e.g., `1.2.api-gateway.md`)
- **Test Design**: `{epic}.{story}-test-design-{YYYYMMDD}.md` (e.g., `1.2-test-design-20260115.md`)
- **Dev Log**: `{epic}.{story}-dev-log.md` (e.g., `1.2-dev-log.md`)
