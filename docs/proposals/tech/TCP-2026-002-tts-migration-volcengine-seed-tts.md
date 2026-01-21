---
metadata:
  proposal_id: TCP-2026-002
  proposal_type: technical
  title: "TTS服务迁移至火山引擎Seed-TTS"
  status: draft
  created_at: 2026-01-21
  author: Architect

linkage:
  related_product_proposal: null
  triggered_by: product_change
---

# TCP-2026-002: TTS服务迁移至火山引擎Seed-TTS

## Change Summary

将语音合成(TTS)服务从阿里云语音合成迁移至火山引擎Seed-TTS。此变更带来两个核心优势：
1. **统一AI服务供应商**：脚本生成(豆包API)和语音合成(Seed-TTS)统一使用火山引擎平台
2. **声音克隆门槛大幅降低**：Seed-TTS仅需5秒音频样本即可完成高质量声音克隆（原方案需30秒-2分钟）

## Change Background

### Trigger Reason
- [x] Product requirement driven
- [ ] Technical debt cleanup
- [ ] Performance optimization
- [ ] Security hardening
- [ ] Architecture evolution

### Related Product Proposal

No related product proposal (PRD v1.2 直接更新技术选型)。

### Context

根据 PRD v1.2 (2026-01-21) 的更新，技术选型发生了以下变更：

| 原选型 | 新选型 | 变更原因 |
|--------|--------|----------|
| 阿里云语音合成 | 火山引擎 Seed-TTS | 更强的声音克隆能力 |
| 声音克隆需30秒-2分钟样本 | 仅需5秒样本 | 用户体验大幅提升 |
| 多供应商集成 | 统一火山引擎平台 | 降低集成复杂度 |

火山引擎Seed-TTS的优势：
- **100+预设音色**：覆盖多种场景需求
- **5秒极速克隆**：Seed-ICL技术仅需5秒样本即可生成高质量克隆音色
- **统一账号体系**：与豆包API共用火山引擎账号，简化密钥管理
- **性价比高**：定价与阿里云TTS相当，但克隆能力更强

## Technical Analysis

### Current State

当前架构设计（architecture.md）中TTS相关配置：

```yaml
# 当前设计
TTS: Aliyun Speech Synthesis
- Standard voices + voice cloning
- 声音克隆样本要求：30秒-2分钟
```

### Problem Identification

1. **供应商分散**：AI文本服务(豆包/火山引擎)与TTS服务(阿里云)分属不同平台，增加运维复杂度
2. **声音克隆门槛高**：30秒-2分钟的样本要求对用户不友好，影响声音克隆功能的采用率
3. **集成成本**：需要对接两套不同的云服务SDK和账号体系

### Technical Constraints

- **兼容性**：新TTS服务必须支持现有音色类型（活泼女声、阳光男声、知性女声）
- **API变更**：需要重新实现TTS和声音克隆的API调用层
- **数据迁移**：无需数据迁移（新项目尚未上线）

## Proposed Solution

### Solution Overview

迁移至火山引擎Seed-TTS，主要变更：

1. **Media Service TTS集成**：替换阿里云TTS SDK为火山引擎Seed-TTS SDK
2. **声音克隆流程优化**：
   - 样本要求从30秒-2分钟降低为5秒-2分钟
   - 使用Seed-ICL（In-Context Learning）技术实现即时克隆
3. **统一配置管理**：与豆包API共用火山引擎AccessKey

### Component Changes

| Component | Change Type | Description |
|-----------|-------------|-------------|
| media-service | modify | 替换TTS SDK实现，从Aliyun改为VolcEngine |
| user-service | modify | 更新声音样本验证逻辑，最小时长从30秒改为5秒 |
| architecture.md | modify | 更新技术栈说明 |
| 配置文件 | modify | 添加火山引擎Seed-TTS配置项 |

### API Design

**火山引擎Seed-TTS API集成：**

```yaml
# 标准TTS配置
volcengine:
  seed_tts:
    app_id: ${VOLCENGINE_APP_ID}
    access_token: ${VOLCENGINE_ACCESS_TOKEN}
    cluster: volcano_tts

# 标准音色映射
voice_mapping:
  xiaomei: zh_female_linjiayin_moon_bigtts  # 活泼女声
  xiaoshuai: zh_male_chunhou_mars_bigtts    # 阳光男声
  zhixing: zh_female_shuangkuaisisi_mars_bigtts  # 知性女声

# 声音克隆 (Seed-ICL)
voice_clone:
  api_endpoint: https://openspeech.bytedance.com/api/v1/mega_tts/audio/upload
  min_duration_seconds: 5   # 最小5秒
  max_duration_seconds: 120 # 最大2分钟
  supported_formats: [mp3, wav, m4a]
```

**API调用示例：**

```java
// 标准TTS调用
public byte[] synthesize(String text, String voiceType) {
    SpeechSynthesisRequest request = SpeechSynthesisRequest.builder()
        .appId(config.getAppId())
        .text(text)
        .voiceType(voiceMapping.get(voiceType))
        .audioConfig(AudioConfig.builder()
            .sampleRate(48000)
            .encoding("mp3")
            .build())
        .build();

    return volcengineTtsClient.synthesize(request);
}

// 声音克隆 (Seed-ICL)
public String cloneVoice(String audioUrl, String speakerName) {
    VoiceCloneRequest request = VoiceCloneRequest.builder()
        .audioUrl(audioUrl)
        .speakerName(speakerName)
        .build();

    VoiceCloneResponse response = volcengineTtsClient.cloneVoice(request);
    return response.getSpeakerId(); // 返回克隆音色ID
}
```

### Alternative Solutions Considered

| 方案 | 优点 | 缺点 | 决策 |
|------|------|------|------|
| 保持阿里云TTS | 无迁移成本 | 声音克隆门槛高，供应商分散 | 否决 |
| 火山引擎Seed-TTS | 5秒克隆，统一平台 | 需要重新集成 | **采纳** |
| 自建TTS服务 | 完全自控 | 成本高，周期长 | 否决 |

## Impact Analysis

### Affected Components

| Layer | Component | Change Type |
|-------|-----------|-------------|
| Backend | media-service | 核心代码修改 |
| Backend | user-service | 验证逻辑修改 |
| Config | application.yml | 新增配置项 |
| Docs | architecture.md | 文档更新 |
| Docs | prd.md | 已更新 |

### Dependencies

- 火山引擎账号开通及Seed-TTS服务激活
- 火山引擎Java SDK集成
- 标准音色选型确认（从100+音色中选择3种）

### Compatibility

- [x] Backward compatible (API接口不变，仅内部实现替换)
- [ ] Requires migration (无需数据迁移)

## Story Requirements

> This section is used by SM when executing `*apply-proposal`

### Story Requirement 1

```yaml
epic_id: 4
action: modify
target_story_id: "4.1"
title: "TTS配音服务实现"
description: |
  更新TTS配音服务，将阿里云TTS替换为火山引擎Seed-TTS。
  保持现有API接口不变，仅替换内部实现。
acceptance_criteria_hints:
  - 集成火山引擎Seed-TTS SDK
  - 配置3种标准音色映射（活泼女声、阳光男声、知性女声）
  - 配音采样率48000Hz，输出格式MP3
  - 支持配音进度跟踪
technical_notes:
  - 使用火山引擎官方Java SDK
  - 音色映射参考：xiaomei→zh_female_linjiayin_moon_bigtts
  - 配置文件添加volcengine.seed_tts相关配置
complexity: medium
```

### Story Requirement 2

```yaml
epic_id: 4
action: modify
target_story_id: "4.2"
title: "声音克隆功能"
description: |
  更新声音克隆实现，使用火山引擎Seed-ICL技术。
  关键变更：样本要求从30秒-2分钟降低为5秒-2分钟。
acceptance_criteria_hints:
  - AC1 更新：上传声音样本时长验证改为5秒-2分钟
  - AC2 更新：使用Seed-ICL API进行声音克隆
  - 前端提示文案更新：强调"仅需5秒即可高质量克隆"
technical_notes:
  - Seed-ICL API: openspeech.bytedance.com/api/v1/mega_tts/audio/upload
  - 克隆处理时间预计1-3分钟
  - 克隆成功后返回speaker_id用于后续TTS调用
complexity: medium
```

### Story Requirement 3

```yaml
epic_id: 4
action: modify
target_story_id: "4.4"
title: "配音设置UI组件"
description: |
  更新声音克隆相关的UI文案和验证逻辑。
acceptance_criteria_hints:
  - 更新BR-2.1：声音样本时长要求改为5秒-2分钟
  - 更新提示文案：强调"仅需5秒即可高质量克隆您的声音"
  - 保持其他功能不变
technical_notes:
  - 前端验证逻辑：minDuration从30改为5
  - 提示文案国际化考虑（如后续支持多语言）
complexity: low
```

## Implementation Plan

### Dependency Order

```
1. 火山引擎账号配置 (运维)
   │
   ├─→ 2. media-service TTS集成 (Story 4.1)
   │      │
   │      └─→ 3. 声音克隆功能更新 (Story 4.2)
   │
   └─→ 4. 前端文案更新 (Story 4.4)
```

### Key Milestones

1. 火山引擎Seed-TTS服务开通及AccessKey配置
2. media-service TTS SDK集成及标准音色测试
3. 声音克隆(Seed-ICL)功能集成及测试
4. 前端文案及验证逻辑更新
5. 端到端集成测试

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| 火山引擎服务稳定性 | LOW | HIGH | 监控告警 + 降级方案 |
| 音色质量差异 | LOW | MEDIUM | 提前试听对比，用户可接受即可 |
| 克隆效果不如预期 | LOW | MEDIUM | 保留标准音色作为备选 |
| SDK集成问题 | MEDIUM | LOW | 参考官方文档，联系技术支持 |

## Cost Impact

| 项目 | 阿里云TTS | 火山引擎Seed-TTS | 变化 |
|------|-----------|------------------|------|
| 标准TTS | ¥0.002/字符 | ¥0.002/字符 | 持平 |
| 声音克隆 | ~¥2.00/样本 | ~¥1.50/样本 | 降低 |
| 月度预估 | ¥1,200 | ¥1,100 | -8% |

## Approval Record

| Date | Approver | Decision | Notes |
|------|----------|----------|-------|
| 2026-01-21 | - | pending | 待审批 |
