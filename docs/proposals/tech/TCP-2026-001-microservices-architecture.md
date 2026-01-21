---
metadata:
  proposal_id: TCP-2026-001
  proposal_type: technical
  title: "微服务架构设计 - Vue3 + Java21 + Spring Cloud"
  status: applied
  created_at: 2026-01-14
  applied_at: 2026-01-14
  author: Architect

linkage:
  related_product_proposal: PCP-2026-001
  triggered_by: product_change
---

# TCP-2026-001: 微服务架构设计

## Change Summary

为探店宝V1.0设计完整的微服务架构，支持从视频上传到成品导出的端到端流程，并为V2.0 iOS版本做好扩展准备。

**技术栈：**
- 前端：Vue 3 + TypeScript + Vite + Element Plus
- 后端：Java 21 + Spring Cloud (Gateway + Nacos + OpenFeign)
- 数据库：MySQL 8.0 + Redis 7.x
- 存储：阿里云OSS + CDN
- AI服务：通义千问VL + 豆包API + 阿里云语音合成
- 视频处理：FFmpeg

## Change Background

### Trigger Reason
- [x] Product requirement driven
- [ ] Technical debt cleanup
- [ ] Performance optimization
- [ ] Security hardening
- [ ] Architecture evolution

### Related Product Proposal

```yaml
related_product_proposal: PCP-2026-001
product_proposal_title: "V1.0实现完整端到端流程"
```

### Context

基于产品需求，系统需要支持：
1. 视频批量上传与存储
2. AI多模态镜头分析（通义千问VL）
3. AI脚本生成（豆包API）
4. AI配音合成 + 声音克隆（阿里云TTS）
5. 视频自动剪辑合成（FFmpeg）
6. 热门话题与标题推荐（豆包API）
7. 为iOS 2.0版本预留扩展能力

## Technical Analysis

### Current State

项目为全新开发（Greenfield），无现有代码或架构约束。

### Technical Constraints

- **团队技术栈**：Vue 3 + Java，非 React/Python
- **目标规模**：内测阶段 100-500 用户
- **处理时间**：单次任务 5-8 分钟
- **成本控制**：单次任务成本 ~¥3.4
- **扩展性**：需支持后续 iOS App 接入

### Key Technical Decisions

| Decision | Choice | Rationale | Confidence |
|----------|--------|-----------|------------|
| 后端框架 | Spring Cloud | 企业级微服务支持，团队熟悉 | HIGH |
| 服务注册 | Nacos | 阿里开源，与阿里云生态兼容 | HIGH |
| API网关 | Spring Cloud Gateway | 原生集成，响应式 | HIGH |
| 消息队列 | Redis Streams | 轻量级，内测阶段够用 | MEDIUM |
| 任务调度 | XXL-Job | 分布式任务调度，可视化 | MEDIUM |

## Proposed Solution

### Solution Overview

采用微服务架构，按业务域拆分为6个核心服务：

```
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway                               │
│                  (Spring Cloud Gateway)                          │
│         路由 / 限流 / 熔断 / JWT认证 / API版本化                  │
└─────────────────────────────────────────────────────────────────┘
                                │
        ┌───────────┬───────────┼───────────┬───────────┐
        ▼           ▼           ▼           ▼           ▼
   ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
   │  user   │ │  task   │ │   ai    │ │  media  │ │ publish │
   │ service │ │ service │ │ service │ │ service │ │ service │
   └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘
```

### Component Design

#### 1. API Gateway (gateway-service)

**职责：** 统一入口、路由分发、安全认证

```yaml
service_name: gateway-service
port: 8080
tech_stack:
  - Spring Cloud Gateway 4.x
  - Spring Security
  - JWT (jjwt)

features:
  - 路由配置 (基于Path前缀)
  - JWT Token验证
  - 限流熔断 (Resilience4j)
  - API版本化 (/api/v1/*)
  - CORS配置
  - 请求日志

route_config:
  - path: /api/v1/auth/**     -> user-service
  - path: /api/v1/user/**     -> user-service
  - path: /api/v1/voice/**    -> user-service
  - path: /api/v1/tasks/**    -> task-service
  - path: /api/v1/ai/**       -> ai-service
  - path: /api/v1/media/**    -> media-service
  - path: /api/v1/publish/**  -> publish-service
```

#### 2. User Service (user-service)

**职责：** 用户认证、会员管理、声音样本管理

```yaml
service_name: user-service
port: 8081
tech_stack:
  - Spring Boot 3.2
  - MyBatis-Plus
  - Redis (Session/Cache)

modules:
  auth:
    - 手机验证码登录 (阿里云短信)
    - JWT Token生成/刷新
    - Token黑名单 (Redis)

  user:
    - 用户信息CRUD
    - 会员状态管理
    - 设备管理 (为iOS预留)

  voice:
    - 声音样本上传
    - 声音克隆状态管理
    - 克隆音色ID存储

database_tables:
  - users
  - user_voice_samples
  - user_devices
  - user_memberships
```

#### 3. Task Service (task-service)

**职责：** 任务生命周期管理、进度跟踪

```yaml
service_name: task-service
port: 8082
tech_stack:
  - Spring Boot 3.2
  - MyBatis-Plus
  - Redis (进度缓存)

modules:
  task:
    - 任务CRUD
    - 状态机管理
    - 进度更新 (WebSocket/SSE)

  video:
    - 视频元数据管理
    - OSS上传URL生成
    - 视频分析结果存储

state_machine:
  states:
    - CREATED      # 任务创建
    - UPLOADING    # 上传中
    - ANALYZING    # AI分析中
    - SCRIPTING    # 脚本生成中
    - VOICING      # 配音中
    - COMPOSING    # 视频合成中
    - COMPLETED    # 完成
    - FAILED       # 失败

database_tables:
  - tasks
  - task_videos
  - task_scripts
```

#### 4. AI Service (ai-service)

**职责：** AI模型调用、结果处理

```yaml
service_name: ai-service
port: 8083
tech_stack:
  - Spring Boot 3.2
  - WebClient (异步HTTP)
  - Redis (结果缓存)

modules:
  vision:
    - 通义千问VL API调用
    - 镜头分析 (分类/标签/评分)
    - 关键帧处理

  text:
    - 豆包API调用
    - 脚本生成
    - 话题/标题推荐

external_apis:
  qwen_vl:
    endpoint: "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"
    model: "qwen-vl-plus"
    timeout: 60s
    retry: 3

  doubao:
    endpoint: "https://ark.cn-beijing.volces.com/api/v3/chat/completions"
    model: "doubao-pro-32k"
    timeout: 30s
    retry: 3
```

#### 5. Media Service (media-service)

**职责：** 音视频处理、TTS配音

```yaml
service_name: media-service
port: 8084
tech_stack:
  - Spring Boot 3.2
  - FFmpeg (ProcessBuilder)
  - 阿里云OSS SDK
  - 阿里云语音合成 SDK

modules:
  tts:
    - 标准语音合成
    - 声音克隆调用
    - 音频文件管理

  video:
    - 关键帧提取
    - 视频片段裁剪
    - 音视频合并
    - 转场效果添加
    - 输出压缩

external_apis:
  aliyun_tts:
    endpoint: "wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1"
    voices:
      - xiaoyun  # 标准女声
      - xiaogang # 标准男声
      - xiaomeng # 甜美女声

  aliyun_voice_clone:
    endpoint: "https://nls-portal.cn-shanghai.aliyuncs.com"
    # 声音复刻API

ffmpeg_commands:
  extract_frames: "ffmpeg -i {input} -vf fps=0.5 {output}_%04d.jpg"
  cut_segment: "ffmpeg -i {input} -ss {start} -t {duration} -c copy {output}"
  merge_audio: "ffmpeg -i {video} -i {audio} -c:v copy -c:a aac {output}"
  concat_videos: "ffmpeg -f concat -safe 0 -i {list} -c copy {output}"
```

#### 6. Publish Service (publish-service)

**职责：** 发布辅助内容生成

```yaml
service_name: publish-service
port: 8085
tech_stack:
  - Spring Boot 3.2
  - WebClient

modules:
  topics:
    - 热门话题分析
    - 话题标签生成
    - 格式化输出

  titles:
    - 标题风格分析
    - 多标题生成
    - 去重优化

prompts:
  topic_generation: |
    基于以下探店视频内容，生成10个适合抖音发布的热门话题标签：
    店铺类型: {shop_type}
    店铺名称: {shop_name}
    视频脚本摘要: {script_summary}

    要求：
    1. 格式：#话题名#
    2. 包含店铺相关、美食/行业相关、地域相关话题
    3. 优先选择热度高的话题

  title_generation: |
    为以下探店视频生成5个吸引眼球的标题：
    店铺: {shop_name} ({shop_type})
    脚本摘要: {script_summary}
    优惠信息: {promotion}

    要求：
    1. 长度20-50字
    2. 符合抖音/小红书爆款风格
    3. 包含emoji可选
    4. 突出亮点和优惠
```

### Database Schema Design

```sql
-- ============================================================
-- 用户域 (user-service)
-- ============================================================

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(20) UNIQUE NOT NULL,
    nickname VARCHAR(50),
    avatar_url VARCHAR(500),
    membership_type ENUM('free', 'basic', 'pro') DEFAULT 'free',
    membership_expire_at DATETIME,
    remaining_quota INT DEFAULT 2,  -- 剩余使用次数
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_phone (phone),
    INDEX idx_membership (membership_type, membership_expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_voice_samples (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    sample_name VARCHAR(100) DEFAULT '我的声音',
    oss_key VARCHAR(500) NOT NULL,
    oss_url VARCHAR(1000),
    duration_seconds INT,
    file_size_bytes BIGINT,
    clone_status ENUM('pending', 'processing', 'completed', 'failed') DEFAULT 'pending',
    clone_voice_id VARCHAR(200),  -- 阿里云返回的克隆音色ID
    error_message TEXT,
    is_default BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_clone_status (clone_status),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_devices (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    device_type ENUM('web', 'ios', 'android') NOT NULL,
    device_id VARCHAR(200),
    device_name VARCHAR(100),
    push_token VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    last_active_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_device (user_id, device_type),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 任务域 (task-service)
-- ============================================================

CREATE TABLE tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,

    -- 店铺信息
    shop_name VARCHAR(200) NOT NULL,
    shop_type ENUM('food', 'beauty', 'entertainment', 'other') NOT NULL,
    promotion_text TEXT,
    video_style ENUM('recommend', 'review', 'vlog') NOT NULL,

    -- 配音设置
    voice_type VARCHAR(50) DEFAULT 'xiaoyun',
    voice_sample_id BIGINT,  -- 关联用户克隆音色

    -- 任务状态
    status ENUM('created', 'uploading', 'analyzing', 'scripting', 'voicing', 'composing', 'completed', 'failed') DEFAULT 'created',
    progress INT DEFAULT 0,
    current_step VARCHAR(50),
    error_message TEXT,

    -- AI生成结果
    generated_script TEXT,
    script_segments JSON,  -- 分段脚本结构

    -- 成品视频信息
    output_video_key VARCHAR(500),
    output_video_url VARCHAR(1000),
    output_video_duration INT,
    output_video_size BIGINT,

    -- 时间戳
    started_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE task_videos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,

    -- 文件信息
    original_filename VARCHAR(500),
    oss_key VARCHAR(500) NOT NULL,
    oss_url VARCHAR(1000),
    thumbnail_url VARCHAR(1000),
    duration_seconds INT,
    file_size_bytes BIGINT,
    resolution VARCHAR(20),  -- e.g., "1080x1920"

    -- AI分析结果
    category ENUM('food', 'person', 'environment', 'other'),
    tags JSON,
    description TEXT,
    quality_score INT,
    quality_details JSON,  -- {clarity, composition, lighting, stability}
    is_recommended BOOLEAN DEFAULT FALSE,

    -- 使用信息
    used_in_output BOOLEAN DEFAULT FALSE,
    used_segment_start INT,
    used_segment_end INT,
    sequence_order INT,  -- 在成品中的顺序

    -- 分析状态
    analysis_status ENUM('pending', 'processing', 'completed', 'failed') DEFAULT 'pending',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_task_id (task_id),
    INDEX idx_category (category),
    INDEX idx_recommended (task_id, is_recommended),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE task_voice_segments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,

    segment_index INT NOT NULL,
    segment_title VARCHAR(100),
    script_text TEXT NOT NULL,

    audio_key VARCHAR(500),
    audio_url VARCHAR(1000),
    duration_ms INT,

    status ENUM('pending', 'processing', 'completed', 'failed') DEFAULT 'pending',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_task_segment (task_id, segment_index),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE task_publish_assists (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL UNIQUE,

    hot_topics JSON,
    title_suggestions JSON,

    selected_topics JSON,
    selected_title VARCHAR(500),

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### API Design (OpenAPI Summary)

```yaml
openapi: 3.0.3
info:
  title: 探店宝 API
  version: v1.0.0

servers:
  - url: https://api.example.com/api/v1

paths:
  # ========== Auth ==========
  /auth/send-code:
    post:
      summary: 发送验证码
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                phone: { type: string }

  /auth/login:
    post:
      summary: 验证码登录
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                phone: { type: string }
                code: { type: string }
      responses:
        200:
          content:
            application/json:
              schema:
                type: object
                properties:
                  access_token: { type: string }
                  refresh_token: { type: string }
                  expires_in: { type: integer }

  # ========== Tasks ==========
  /tasks:
    post:
      summary: 创建任务
      security: [bearerAuth: []]
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateTaskRequest'
    get:
      summary: 获取任务列表
      security: [bearerAuth: []]
      parameters:
        - name: page
          in: query
          schema: { type: integer, default: 1 }
        - name: size
          in: query
          schema: { type: integer, default: 20 }

  /tasks/{taskId}:
    get:
      summary: 获取任务详情
      security: [bearerAuth: []]

  /tasks/{taskId}/videos/upload-url:
    post:
      summary: 获取视频上传URL
      security: [bearerAuth: []]
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                filename: { type: string }
                content_type: { type: string }

  /tasks/{taskId}/analyze:
    post:
      summary: 开始AI分析
      security: [bearerAuth: []]

  /tasks/{taskId}/compose:
    post:
      summary: 开始配音&合成
      security: [bearerAuth: []]
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                voice_type: { type: string }
                voice_sample_id: { type: integer }

  /tasks/{taskId}/progress:
    get:
      summary: 获取任务进度 (SSE)
      security: [bearerAuth: []]

  /tasks/{taskId}/output:
    get:
      summary: 获取成品视频信息
      security: [bearerAuth: []]

  /tasks/{taskId}/publish-assist:
    get:
      summary: 获取话题和标题推荐
      security: [bearerAuth: []]

  # ========== Voice ==========
  /voice/upload-url:
    post:
      summary: 获取声音样本上传URL
      security: [bearerAuth: []]

  /voice/samples:
    post:
      summary: 创建声音克隆任务
      security: [bearerAuth: []]
    get:
      summary: 获取声音样本列表
      security: [bearerAuth: []]

  /voice/samples/{sampleId}/preview:
    get:
      summary: 试听克隆声音
      security: [bearerAuth: []]

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

  schemas:
    CreateTaskRequest:
      type: object
      required: [shop_name, shop_type, video_style]
      properties:
        shop_name: { type: string, maxLength: 200 }
        shop_type: { type: string, enum: [food, beauty, entertainment, other] }
        promotion_text: { type: string }
        video_style: { type: string, enum: [recommend, review, vlog] }
```

### Infrastructure Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         阿里云 VPC                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐       │
│  │   SLB       │────▶│   Nginx     │────▶│   Gateway   │       │
│  │ (公网入口)   │     │ (静态资源)  │     │   Service   │       │
│  └─────────────┘     └─────────────┘     └──────┬──────┘       │
│                                                  │              │
│         ┌────────────────────────────────────────┤              │
│         │                                        │              │
│  ┌──────▼──────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   Nacos     │  │   Redis     │  │   MySQL     │             │
│  │ (服务注册)  │  │  Cluster    │  │  (主从)     │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    ECS 集群 (2-4台)                       │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐        │   │
│  │  │ user    │ │ task    │ │ ai      │ │ media   │ ...    │   │
│  │  │ service │ │ service │ │ service │ │ service │        │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      阿里云服务                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   OSS       │  │  通义千问VL  │  │  语音合成   │             │
│  │  + CDN      │  │    API      │  │   + 克隆    │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐                              │
│  │   短信服务   │  │   豆包 API  │                              │
│  └─────────────┘  └─────────────┘                              │
└─────────────────────────────────────────────────────────────────┘
```

## Impact Analysis

### Affected Components

| Layer | Component | Change Type | Description |
|-------|-----------|-------------|-------------|
| Frontend | Vue 3 App | CREATE | 全新前端应用 |
| Gateway | gateway-service | CREATE | API网关服务 |
| Backend | user-service | CREATE | 用户服务 |
| Backend | task-service | CREATE | 任务服务 |
| Backend | ai-service | CREATE | AI调用服务 |
| Backend | media-service | CREATE | 媒体处理服务 |
| Backend | publish-service | CREATE | 发布辅助服务 |
| Database | MySQL Schema | CREATE | 全新数据库表 |
| Infra | 阿里云资源 | CREATE | OSS/TTS/VL等 |

### Dependencies

- 阿里云账号及API密钥配置
- 豆包API开通及密钥
- 域名及SSL证书
- 服务器资源 (ECS 2-4台)

## Story Requirements

> This section is used by SM when executing `*apply-proposal`

### Story Requirement T1: 项目脚手架搭建

```yaml
epic_id: E1
action: create
suggested_story_id: E1.1
title: "搭建前后端项目脚手架"
description: |
  创建项目基础结构，包括：
  - Vue 3 + Vite 前端项目
  - Spring Cloud 多模块后端项目
  - 公共模块 (common-core, common-security)
acceptance_criteria_hints:
  - 前端项目可运行，显示首页
  - 后端Gateway可启动，返回健康检查
  - Nacos服务注册正常
technical_notes:
  - Vue 3.4+, Vite 5.x, Element Plus 2.x
  - Java 21, Spring Boot 3.2, Spring Cloud 2023.x
  - 使用 Maven 多模块结构
complexity: medium
```

### Story Requirement T2: API网关实现

```yaml
epic_id: E1
action: create
suggested_story_id: E1.2
title: "实现API网关路由和认证"
description: |
  实现Gateway服务核心功能：
  - 路由配置 (路由到各微服务)
  - JWT Token验证
  - 限流熔断基础配置
acceptance_criteria_hints:
  - 请求正确路由到对应服务
  - 无Token请求返回401
  - 有效Token请求通过验证
technical_notes:
  - Spring Cloud Gateway + Spring Security
  - JWT使用jjwt库
  - 配置白名单路径 (/auth/*)
complexity: medium
```

### Story Requirement T3: 用户服务实现

```yaml
epic_id: E1
action: create
suggested_story_id: E1.3
title: "实现用户认证和基础功能"
description: |
  实现user-service核心功能：
  - 手机验证码发送 (阿里云短信)
  - 验证码登录
  - JWT Token生成和刷新
  - 用户信息查询
acceptance_criteria_hints:
  - 验证码发送成功
  - 正确验证码可登录获取Token
  - Token可刷新
  - 用户信息接口正常
technical_notes:
  - 阿里云短信SDK
  - Redis存储验证码 (5分钟过期)
  - Token有效期: access 2h, refresh 7d
complexity: medium
```

### Story Requirement T4: 通义千问VL集成

```yaml
epic_id: E3
action: create
suggested_story_id: E3.1
title: "集成通义千问VL实现镜头分析"
description: |
  实现ai-service中的视觉分析功能：
  - 调用通义千问VL API
  - 分析视频关键帧
  - 返回分类、标签、评分
acceptance_criteria_hints:
  - 图片分析返回正确JSON结构
  - 分类准确率>80%
  - 支持批量帧分析
technical_notes:
  - 使用DashScope SDK
  - 图片Base64编码传输
  - 设置合理超时和重试
complexity: high
```

### Story Requirement T5: 豆包API集成

```yaml
epic_id: E4
action: create
suggested_story_id: E4.1
title: "集成豆包API实现脚本生成"
description: |
  实现ai-service中的文本生成功能：
  - 调用豆包API
  - 根据镜头分析结果生成脚本
  - 脚本分段并关联镜头
acceptance_criteria_hints:
  - 脚本生成成功，格式正确
  - 每段脚本对应具体镜头
  - 生成时间<30秒
technical_notes:
  - 火山引擎SDK或HTTP调用
  - 使用doubao-pro-32k模型
  - Prompt模板可配置
complexity: medium
```

### Story Requirement T6: 阿里云TTS集成

```yaml
epic_id: E5
action: create
suggested_story_id: E5.1
title: "集成阿里云TTS实现脚本配音"
description: |
  实现media-service中的TTS功能：
  - 调用阿里云语音合成API
  - 支持多种标准音色
  - 生成音频文件上传OSS
acceptance_criteria_hints:
  - 文本转语音成功
  - 支持至少3种音色切换
  - 音频清晰，语速正常
technical_notes:
  - 阿里云NLS SDK (WebSocket)
  - 音频格式: MP3, 16kHz
  - 分段合成再拼接
complexity: medium
```

### Story Requirement T7: 声音克隆集成

```yaml
epic_id: E5
action: create
suggested_story_id: E5.2
title: "集成阿里云声音复刻实现声音克隆"
description: |
  实现声音克隆功能：
  - 上传声音样本到OSS
  - 调用阿里云声音复刻API
  - 管理用户克隆音色
acceptance_criteria_hints:
  - 声音样本上传成功
  - 克隆任务提交成功
  - 克隆完成后可用于TTS
technical_notes:
  - 阿里云声音复刻服务
  - 样本要求: 30s-2min, 清晰
  - 克隆耗时约3-5分钟
complexity: high
```

### Story Requirement T8: FFmpeg视频处理

```yaml
epic_id: E6
action: create
suggested_story_id: E6.1
title: "实现FFmpeg视频裁剪和合成"
description: |
  实现media-service中的视频处理功能：
  - 视频片段裁剪
  - 多视频拼接
  - 音视频合并
  - 输出压缩
acceptance_criteria_hints:
  - 视频裁剪准确
  - 拼接无黑帧
  - 音视频同步
  - 输出文件大小合理
technical_notes:
  - FFmpeg 6.x
  - ProcessBuilder执行命令
  - 临时文件清理
complexity: high
```

### Story Requirement T9: 话题标题推荐

```yaml
epic_id: E8
action: create
suggested_story_id: E8.1
title: "实现AI话题和标题推荐"
description: |
  实现publish-service功能：
  - 调用豆包API生成话题标签
  - 生成视频标题建议
  - 格式化输出
acceptance_criteria_hints:
  - 生成5-10个话题标签
  - 生成3-5个标题
  - 格式符合平台规范
technical_notes:
  - 复用ai-service的豆包调用
  - 或通过Feign调用ai-service
complexity: medium
```

### Story Requirement T10: 前端核心页面

```yaml
epic_id: E7
action: create
suggested_story_id: E7.1
title: "实现前端核心页面和流程"
description: |
  实现Vue 3前端核心功能：
  - 创建任务页 (表单+上传)
  - 任务进度页 (实时进度)
  - 结果预览页 (播放器+下载)
acceptance_criteria_hints:
  - 表单验证完整
  - 上传进度显示
  - 视频预览流畅
  - 下载功能正常
technical_notes:
  - Element Plus组件库
  - Video.js播放器
  - SSE接收进度更新
complexity: high
```

## Implementation Plan

### Dependency Order

```
Phase 1: 基础设施
├── E1.1 项目脚手架
├── E1.2 API网关
└── E1.3 用户服务

Phase 2: 核心AI能力
├── E3.1 通义千问VL (镜头分析)
└── E4.1 豆包API (脚本生成)

Phase 3: 媒体处理
├── E5.1 阿里云TTS
├── E5.2 声音克隆
└── E6.1 FFmpeg视频处理

Phase 4: 发布辅助
└── E8.1 话题标题推荐

Phase 5: 前端集成
└── E7.1 前端核心页面
```

### Key Milestones

1. **M1**: 基础架构就绪 - Gateway + User服务可用
2. **M2**: AI分析流程打通 - 上传→分析→脚本
3. **M3**: 视频合成流程打通 - 配音→剪辑→合成
4. **M4**: 完整流程闭环 - 端到端可用
5. **M5**: 内测发布 - 部署上线

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| 通义千问VL调用延迟高 | MEDIUM | HIGH | 设置超时，批量并行处理 |
| 声音克隆效果不佳 | MEDIUM | MEDIUM | 提供样本录制指南，允许重试 |
| FFmpeg处理耗时长 | HIGH | MEDIUM | 优化命令参数，考虑GPU加速 |
| 微服务间调用复杂度 | MEDIUM | MEDIUM | 合理服务拆分，避免过度微服务化 |
| 阿里云服务费用超预期 | LOW | MEDIUM | 监控用量，设置预算告警 |

## Approval Record

| Date | Approver | Decision | Notes |
|------|----------|----------|-------|
| 2026-01-14 | User | approved | 用户确认技术方案 |
| 2026-01-14 | SM | applied | Stories created: E1.1-E1.3, E3.1, E4.1, E5.1-E5.2, E6.1, E7.3, E8.1 |
