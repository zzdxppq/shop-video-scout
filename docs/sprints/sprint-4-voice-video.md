# Sprint 4: 配音与视频合成

## Sprint 信息

| 属性 | 值 |
|------|-----|
| Sprint 编号 | 4 |
| Sprint 名称 | Voice & Video Composition |
| 状态 | Planned |
| 创建日期 | 2026-01-22 |

## Sprint 目标

完成 TTS 配音服务和视频自动剪辑合成功能，实现从脚本到成品视频的完整流程。

## 交付物

- TTS 配音服务 (火山引擎 Seed-TTS)
- 视频自动剪辑合成服务 (FFmpeg)
- 配音设置页面 (音色选择、试听)
- 字幕设置页面 (样式选择)

## Sprint Backlog

### Story 4.1: TTS配音服务实现
| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 类型 | Backend |
| 依赖 | 3.1 |
| 文件 | [4.1.tts-service.md](../stories/4.1.tts-service.md) |

**Tasks:**
- [ ] T1: 火山引擎 Seed-TTS SDK 集成
- [ ] T2: 音色选择逻辑
- [ ] T3: 配音任务队列

---

### Story 4.3: 视频自动剪辑合成
| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 类型 | Backend |
| 依赖 | 4.1 |
| 文件 | [4.3.video-composition.md](../stories/4.3.video-composition.md) |

**Tasks:**
- [ ] T1: FFmpeg 视频裁剪
- [ ] T2: FFmpeg 音视频合成
- [ ] T3: ASS 字幕生成和烧录

---

### Story 4.4: 配音设置页面
| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 类型 | Frontend |
| 依赖 | 4.1 |
| 文件 | [4.4.voice-settings-ui.md](../stories/4.4.voice-settings-ui.md) |

**Tasks:**
- [ ] T1: 音色选择组件
- [ ] T2: 试听播放功能
- [ ] T3: 合成进度页面

---

### Story 4.5: 字幕设置页面
| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 类型 | Frontend |
| 依赖 | 4.4 |
| 文件 | [4.5.subtitle-settings-ui.md](../stories/4.5.subtitle-settings-ui.md) |

**Tasks:**
- [ ] T1: 字幕开关组件
- [ ] T2: 样式选择组件
- [ ] T3: 样式预览图生成

---

## 依赖关系

```
[3.1 AI脚本] ──► [4.1 TTS服务] ──┬──► [4.3 视频合成]
                                 │
                                 └──► [4.4 配音设置UI] ──► [4.5 字幕设置UI]
```

## 验收标准

- [ ] 用户可选择音色并试听
- [ ] TTS 可将脚本转换为配音音频
- [ ] 系统可根据配音时长自动裁剪视频
- [ ] 视频和配音可自动合成
- [ ] 用户可选择字幕样式
- [ ] 字幕可正确烧录到视频

## 风险与阻塞

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| FFmpeg 处理耗时 | 用户等待时间长 | 显示进度，优化参数 |
| 火山引擎 API 延迟 | 配音生成慢 | 异步处理 + 进度反馈 |
| 字幕时间轴不准 | 字幕不同步 | 精确计算配音时长 |

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2026-01-22 | SM Agent | Sprint 创建 |
