# Sprint 规划总览

## 项目信息

| 属性 | 值 |
|------|-----|
| 项目名称 | Shop Video Scout (探店视频生成器) |
| 总 Sprint 数 | 5 |
| 总 Story 数 | 23 (P0: 21, P1: 2) |
| 规划日期 | 2026-01-22 |

## Sprint 路线图

```
Sprint 1          Sprint 2          Sprint 3          Sprint 4          Sprint 5
Foundation        Video Upload      Script Gen        Voice/Video       Preview/Publish
────────────────────────────────────────────────────────────────────────────────────────►

[CI/CD]           [任务创建]        [AI脚本生成]      [TTS配音]         [视频预览]
[后端架构]        [视频上传]        [脚本编辑]        [视频合成]        [下载导出]
[用户认证]        [AI镜头分析]      [测试基础设施]    [配音设置]        [发布辅助]
[前端登录]        [分析结果UI]                        [字幕设置]        [监控告警]

    ▼                 ▼                 ▼                 ▼                 ▼
可登录系统        上传→分析        分析→脚本        脚本→视频         MVP发布
```

## Sprint 列表

| Sprint | 名称 | Stories | 主要交付物 | 状态 |
|--------|------|---------|-----------|------|
| [Sprint 1](sprint-1-foundation.md) | Foundation | 0.1, 1.1, 1.2, 1.3 | 可登录的前后端系统 | Planned |
| [Sprint 2](sprint-2-video-upload.md) | Video Upload | 1.4, 2.1, 2.2, 2.3, 2.4 | 视频上传和AI分析 | Planned |
| [Sprint 3](sprint-3-script-generation.md) | Script Generation | 3.1, 3.2, 0.2 | AI脚本生成和编辑 | Planned |
| [Sprint 4](sprint-4-voice-video.md) | Voice & Video | 4.1, 4.3, 4.4, 4.5 | TTS配音和视频合成 | Planned |
| [Sprint 5](sprint-5-preview-publish.md) | Preview & Publish | 5.1, 5.2, 5.3, 5.4, 0.3 | 预览导出和发布辅助 | Planned |

## Story 分配

### Epic 0: DevOps 基础设施 (3 Stories)
| Story | 名称 | Sprint |
|-------|------|--------|
| 0.1 | CI/CD流水线搭建 | Sprint 1 |
| 0.2 | 测试基础设施搭建 | Sprint 3 |
| 0.3 | 监控告警系统搭建 | Sprint 5 |

### Epic 1: 项目基础与认证 (4 Stories)
| Story | 名称 | Sprint |
|-------|------|--------|
| 1.1 | 后端微服务基础架构 | Sprint 1 |
| 1.2 | 用户认证服务 | Sprint 1 |
| 1.3 | 前端项目与登录页面 | Sprint 1 |
| 1.4 | 任务创建表单页面 | Sprint 2 |

### Epic 2: 视频上传与AI分析 (4 Stories)
| Story | 名称 | Sprint |
|-------|------|--------|
| 2.1 | 视频上传服务 | Sprint 2 |
| 2.2 | 视频上传UI组件 | Sprint 2 |
| 2.3 | AI镜头分析服务 | Sprint 2 |
| 2.4 | 镜头分析结果页面 | Sprint 2 |

### Epic 3: AI脚本生成 (2 Stories)
| Story | 名称 | Sprint |
|-------|------|--------|
| 3.1 | AI脚本生成服务 | Sprint 3 |
| 3.2 | 脚本编辑页面 | Sprint 3 |

### Epic 4: 配音合成与视频剪辑 (5 Stories)
| Story | 名称 | Sprint |
|-------|------|--------|
| 4.1 | TTS配音服务 | Sprint 4 |
| 4.2 | 声音克隆功能 | **Backlog (P1)** |
| 4.3 | 视频自动剪辑合成 | Sprint 4 |
| 4.4 | 配音设置页面 | Sprint 4 |
| 4.5 | 字幕设置页面 | Sprint 4 |

### Epic 5: 预览导出与发布 (5 Stories)
| Story | 名称 | Sprint |
|-------|------|--------|
| 5.1 | 视频预览播放器 | Sprint 5 |
| 5.2 | 下载导出功能 | Sprint 5 |
| 5.3 | 发布辅助服务 | Sprint 5 |
| 5.4 | 发布辅助UI组件 | Sprint 5 |
| 5.5 | 历史任务管理 | **Backlog (P1)** |

## Backlog (P1 - 后续迭代)

| Story | 名称 | 说明 |
|-------|------|------|
| 4.2 | 声音克隆功能 | 依赖 4.1，用户可使用自己的声音配音 |
| 5.5 | 历史任务管理 | 依赖 1.4，用户可查看和管理历史任务 |

## 关键里程碑

| 里程碑 | Sprint | 交付物 |
|--------|--------|--------|
| M1: 系统骨架 | Sprint 1 完成 | 可登录的前后端系统 |
| M2: 核心流程前半 | Sprint 2 完成 | 上传视频 → AI分析 |
| M3: 核心流程中段 | Sprint 3 完成 | AI分析 → 脚本生成 |
| M4: 核心流程后半 | Sprint 4 完成 | 脚本 → 成品视频 |
| M5: MVP发布 | Sprint 5 完成 | 完整用户流程 |

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2026-01-22 | SM Agent | 初始规划创建 |
