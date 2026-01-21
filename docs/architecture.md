# 探店宝 (Shop Video Scout) - Technical Architecture Document

---

## 1. Overview

### 1.1 Purpose

This document defines the technical architecture for 探店宝 (Shop Video Scout), an AI-powered video creation assistant for shop reviewers. It covers system design, service architecture, data models, API specifications, infrastructure, and deployment strategies.

### 1.2 Scope

- **V1.0 Web MVP:** Focus on web application with responsive design
- **V2.0 Preparation:** Architecture designed to support future iOS app integration

### 1.3 Change Log

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2026-01-17 | 1.0 | Initial Architecture Document | Architect |
| 2026-01-21 | 1.1 | TTS迁移至火山引擎Seed-TTS (TCP-2026-002) | Architect |

### 1.4 References

- [PRD Document](./prd.md)
- [UI/UX Specification](./front-end-spec.md)

---

## 2. Architecture Principles

### 2.1 Guiding Principles

| Principle | Description |
|-----------|-------------|
| **API-First** | All functionality exposed via versioned REST APIs (`/api/v1/`) |
| **Separation of Concerns** | Clear boundaries between services; each service owns its data |
| **Async Processing** | Long-running tasks (AI analysis, video composition) handled asynchronously |
| **Fail-Safe Defaults** | Graceful degradation when external services are unavailable |
| **Cost Awareness** | Monitor and optimize AI/storage costs per task |
| **Stateless Services** | Application servers are stateless; state stored in Redis/MySQL |
| **12-Factor App** | Follow 12-factor methodology for cloud-native deployment |

### 2.2 Technology Stack

| Layer | Technology | Rationale |
|-------|------------|-----------|
| **Frontend** | Vue 3 + TypeScript + Vite | Team familiarity, modern tooling |
| **UI Framework** | TailwindCSS + Element Plus | Rapid development, consistent design |
| **Video Player** | Video.js | Customizable, well-supported |
| **Backend Framework** | Java 21 + Spring Boot 3.x | Enterprise stability, ecosystem |
| **Microservices** | Spring Cloud Gateway + Nacos | Service discovery, routing |
| **Message Queue** | RabbitMQ | Async task processing |
| **Database** | MySQL 8.0 + MyBatis-Plus | Structured data, familiar ORM |
| **Cache** | Redis 7.x | Session, caching, distributed locks |
| **Object Storage** | Aliyun OSS + CDN | Video/image storage, delivery |
| **AI - Vision** | 通义千问 VL (Qwen-VL) | Multimodal image understanding |
| **AI - Text** | 豆包 API (火山引擎) | Script generation, recommendations |
| **TTS** | 火山引擎 Seed-TTS | Standard voices (100+) + voice cloning (5s sample) |
| **Video Processing** | FFmpeg | Frame extraction, composition |

---

## 3. System Architecture

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                  CLIENTS                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                   │
│  │  Web Browser │    │  iOS App     │    │  Admin Panel │                   │
│  │  (Vue 3 SPA) │    │  (V2.0)      │    │  (Internal)  │                   │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘                   │
│         │                   │                   │                           │
│         └───────────────────┼───────────────────┘                           │
│                             │                                               │
│                             ▼                                               │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                        Aliyun CDN / SLB                               │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                             │                                               │
└─────────────────────────────┼───────────────────────────────────────────────┘
                              │
┌─────────────────────────────┼───────────────────────────────────────────────┐
│                             ▼                           GATEWAY LAYER        │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                     API Gateway (Spring Cloud Gateway)                │  │
│  │  - Route Distribution    - Rate Limiting    - JWT Validation          │  │
│  │  - CORS Handling         - Circuit Breaker  - Request Logging         │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                             │                                               │
└─────────────────────────────┼───────────────────────────────────────────────┘
                              │
┌─────────────────────────────┼───────────────────────────────────────────────┐
│                             ▼                           SERVICE LAYER        │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │   User     │  │   Task     │  │    AI      │  │   Media    │            │
│  │  Service   │  │  Service   │  │  Service   │  │  Service   │            │
│  │            │  │            │  │            │  │            │            │
│  │ - Auth     │  │ - CRUD     │  │ - Analysis │  │ - TTS      │            │
│  │ - Profile  │  │ - Progress │  │ - Script   │  │ - Clone    │            │
│  │ - Voice    │  │ - History  │  │ - Tags     │  │ - Compose  │            │
│  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘            │
│        │               │               │               │                    │
│  ┌────────────┐                                                            │
│  │  Publish   │        │               │               │                    │
│  │  Service   │        │               │               │                    │
│  │            │        │               │               │                    │
│  │ - Topics   │        │               │               │                    │
│  │ - Titles   │        │               │               │                    │
│  └─────┬──────┘        │               │               │                    │
│        │               │               │               │                    │
│        └───────────────┼───────────────┼───────────────┘                    │
│                        │               │                                    │
│                        ▼               ▼                                    │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                         Nacos (Service Discovery)                     │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────┼───────────────────────────────────────────────┐
│                             ▼                           DATA LAYER           │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │   MySQL    │  │   Redis    │  │  RabbitMQ  │  │ Aliyun OSS │            │
│  │  Primary   │  │  Cluster   │  │  Cluster   │  │  + CDN     │            │
│  │            │  │            │  │            │  │            │            │
│  │ - Users    │  │ - Session  │  │ - Analysis │  │ - Videos   │            │
│  │ - Tasks    │  │ - Cache    │  │ - Compose  │  │ - Audios   │            │
│  │ - Videos   │  │ - Locks    │  │ - Publish  │  │ - Outputs  │            │
│  └────────────┘  └────────────┘  └────────────┘  └────────────┘            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────┼───────────────────────────────────────────────┐
│                             ▼                           EXTERNAL SERVICES    │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │  Qwen-VL   │  │  Doubao    │  │  Aliyun    │  │   SMS      │            │
│  │  (Vision)  │  │  API       │  │  TTS       │  │  Service   │            │
│  │            │  │            │  │            │  │            │            │
│  │ - Frame    │  │ - Script   │  │ - Voices   │  │ - Verify   │            │
│  │   Analysis │  │ - Topics   │  │ - Clone    │  │   Code     │            │
│  └────────────┘  └────────────┘  └────────────┘  └────────────┘            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Service Responsibilities

| Service | Responsibilities | Database | Queue |
|---------|------------------|----------|-------|
| **gateway** | Routing, rate limiting, auth validation, CORS | - | - |
| **user-service** | Authentication, user profile, voice samples | MySQL (users, voice_samples) | - |
| **task-service** | Task CRUD, progress tracking, video metadata | MySQL (tasks, videos, scripts) | - |
| **ai-service** | Frame analysis, script generation, quality scoring | Redis (cache) | analysis.queue |
| **media-service** | TTS synthesis, voice cloning, video composition | MySQL (audio_files) | compose.queue |
| **publish-service** | Topic recommendations, title generation | Redis (cache) | - |

---

## 4. Service Architecture Details

### 4.1 Gateway Service

**Technology:** Spring Cloud Gateway

**Responsibilities:**
- Request routing to downstream services
- JWT token validation
- Rate limiting (per user, per IP)
- CORS configuration
- Circuit breaker for downstream services
- Request/response logging

**Route Configuration:**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/v1/auth/**, /api/v1/users/**, /api/v1/voice/**
        - id: task-service
          uri: lb://task-service
          predicates:
            - Path=/api/v1/tasks/**
        - id: ai-service
          uri: lb://ai-service
          predicates:
            - Path=/api/v1/analysis/**
        - id: media-service
          uri: lb://media-service
          predicates:
            - Path=/api/v1/compose/**
        - id: publish-service
          uri: lb://publish-service
          predicates:
            - Path=/api/v1/publish/**
```

**Rate Limiting:**

| Endpoint Pattern | Rate Limit | Window |
|------------------|------------|--------|
| `/api/v1/auth/send-code` | 1 req | 60s (per phone) |
| `/api/v1/auth/login` | 5 req | 60s (per phone) |
| `/api/v1/tasks` (POST) | 10 req | 60s (per user) |
| `/api/v1/*/regenerate*` | 5 req | 300s (per task) |
| Default | 100 req | 60s (per user) |

---

### 4.2 User Service

**Responsibilities:**
- Phone + SMS verification login
- JWT token generation and refresh
- User profile management
- Voice sample management

**Key APIs:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/send-code` | Send SMS verification code |
| POST | `/api/v1/auth/login` | Login with phone + code |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Invalidate tokens |
| GET | `/api/v1/users/me` | Get current user profile |
| GET | `/api/v1/voice/samples` | List user's voice samples |
| POST | `/api/v1/voice/upload-url` | Get OSS presigned upload URL |
| POST | `/api/v1/voice/samples` | Create voice sample record |
| DELETE | `/api/v1/voice/samples/{id}` | Delete voice sample |

**JWT Token Structure:**

```json
{
  "sub": "user_id",
  "phone": "138****8000",
  "membership": "free",
  "iat": 1705401600,
  "exp": 1705402500
}
```

**Token Configuration:**

| Token Type | Expiration | Storage |
|------------|------------|---------|
| Access Token | 15 minutes | Client (localStorage) |
| Refresh Token | 7 days | Client (localStorage) + Redis blacklist |

---

### 4.3 Task Service

**Responsibilities:**
- Task lifecycle management (create, read, update, delete)
- Video metadata storage
- Script storage and versioning
- Progress tracking
- Task history

**Key APIs:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/tasks` | Create new task |
| GET | `/api/v1/tasks` | List user's tasks (paginated) |
| GET | `/api/v1/tasks/{id}` | Get task details |
| DELETE | `/api/v1/tasks/{id}` | Delete task |
| POST | `/api/v1/tasks/{id}/videos/upload-url` | Get video upload URL |
| POST | `/api/v1/tasks/{id}/videos` | Confirm video upload |
| GET | `/api/v1/tasks/{id}/videos` | List task videos |
| DELETE | `/api/v1/tasks/{id}/videos/{videoId}` | Delete video |
| GET | `/api/v1/tasks/{id}/script` | Get task script |
| PUT | `/api/v1/tasks/{id}/script` | Update task script |
| GET | `/api/v1/tasks/{id}/progress` | Get task progress |
| GET | `/api/v1/tasks/{id}/output` | Get output video info |

**Task Status Flow:**

```
┌─────────┐     ┌───────────┐     ┌───────────┐     ┌─────────────┐
│ CREATED │────▶│ UPLOADING │────▶│ ANALYZING │────▶│ SCRIPT_READY│
└─────────┘     └───────────┘     └───────────┘     └──────┬──────┘
                                                          │
                                                          ▼
┌─────────┐     ┌───────────┐     ┌───────────┐     ┌─────────────┐
│ COMPLETED│◀────│ COMPOSING │◀────│ VOICE_SET │◀────│SCRIPT_EDITED│
└─────────┘     └───────────┘     └───────────┘     └─────────────┘
      │
      ▼
┌─────────┐
│  FAILED │ (can occur from any state)
└─────────┘
```

---

### 4.4 AI Service

**Responsibilities:**
- Video frame extraction (FFmpeg)
- Frame analysis using Qwen-VL
- Shot classification and tagging
- Quality scoring
- Script generation using Doubao API
- Recommended shot marking

**Key APIs:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/tasks/{id}/analyze` | Start AI analysis |
| GET | `/api/v1/tasks/{id}/analysis-progress` | Get analysis progress |
| POST | `/api/v1/tasks/{id}/regenerate-script` | Regenerate script |

**Analysis Pipeline:**

```
┌─────────────────────────────────────────────────────────────────┐
│                      AI Analysis Pipeline                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Frame Extraction (FFmpeg)                                    │
│     ┌─────────┐                                                  │
│     │ Video   │──▶ Extract 1 frame per 2 seconds                │
│     │ File    │──▶ Output: JPEG keyframes                       │
│     └─────────┘                                                  │
│                                                                  │
│  2. Frame Analysis (Qwen-VL) - Parallel Processing               │
│     ┌─────────┐     ┌─────────────────────────────────┐         │
│     │ Frames  │──▶  │ Qwen-VL API                     │         │
│     │ (batch) │     │ - Category: food/person/env/other│         │
│     └─────────┘     │ - Tags: up to 5 tags            │         │
│                     │ - Description: 1 sentence        │         │
│                     │ - Quality: 0-100 score           │         │
│                     └─────────────────────────────────┘         │
│                                                                  │
│  3. Aggregation & Recommendation                                 │
│     ┌─────────────────────────────────────────────────┐         │
│     │ - Group by category                             │         │
│     │ - Sort by quality score                         │         │
│     │ - Mark top 2 per category as recommended        │         │
│     └─────────────────────────────────────────────────┘         │
│                                                                  │
│  4. Script Generation (Doubao API)                               │
│     ┌─────────────────────────────────────────────────┐         │
│     │ Input: shop info + shot summaries + style       │         │
│     │ Output: 5-7 paragraph script with shot mapping  │         │
│     └─────────────────────────────────────────────────┘         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Qwen-VL Prompt Template:**

```
分析这个探店视频的画面帧。请返回以下JSON格式：
{
  "category": "food|person|environment|other",
  "tags": ["标签1", "标签2", ...],  // 最多5个
  "description": "一句话描述画面内容",
  "quality_score": 85,  // 0-100，综合清晰度、构图、光线、稳定性
  "quality_factors": {
    "clarity": 90,
    "composition": 80,
    "lighting": 85,
    "stability": 85
  }
}
```

**Doubao Script Prompt Template:**

```
你是一位专业的探店视频文案撰写专家。请根据以下信息生成探店口播脚本：

店铺信息：
- 名称：{{shop_name}}
- 类型：{{shop_type}}
- 特色/优惠：{{promotion_text}}

视频风格：{{video_style}}（种草安利型/真实测评型/探店vlog型）

可用镜头：
{{shot_summaries}}

要求：
1. 生成5-7个段落
2. 每段标注对应使用的镜头
3. 包含：开场hook、环境介绍、重点内容、优惠信息、结尾互动
4. 总时长控制在60秒左右
5. 语言风格符合{{video_style}}调性

输出JSON格式：
{
  "paragraphs": [
    {
      "section": "开场",
      "shot_id": "xxx",
      "text": "...",
      "estimated_duration": 8
    },
    ...
  ],
  "total_duration": 60
}
```

---

### 4.5 Media Service

**Responsibilities:**
- TTS audio synthesis (火山引擎 Seed-TTS)
- Voice cloning management (Seed-ICL, 5秒样本即可克隆)
- Video segment cutting (FFmpeg)
- Audio-video composition (FFmpeg)
- Subtitle generation and burning

**Key APIs:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| PUT | `/api/v1/tasks/{id}/voice-type` | Set voice type |
| PUT | `/api/v1/tasks/{id}/subtitle-settings` | Set subtitle settings |
| POST | `/api/v1/tasks/{id}/compose` | Start composition |
| GET | `/api/v1/tasks/{id}/compose-progress` | Get composition progress |
| GET | `/api/v1/tasks/{id}/output/download` | Get download URL |
| GET | `/api/v1/tasks/{id}/assets-pack` | Get assets pack URL |

**Composition Pipeline:**

```
┌─────────────────────────────────────────────────────────────────┐
│                    Video Composition Pipeline                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. TTS Synthesis (火山引擎 Seed-TTS)                              │
│     ┌─────────┐     ┌─────────────────────────────────┐         │
│     │ Script  │──▶  │ TTS API                         │         │
│     │ Paras   │     │ - Voice: xiaomei/xiaoshuai/etc  │         │
│     └─────────┘     │ - Output: MP3, 48kHz            │         │
│                     │ - Return: duration per paragraph │         │
│                     └─────────────────────────────────┘         │
│                                                                  │
│  2. Subtitle Generation                                          │
│     ┌─────────────────────────────────────────────────┐         │
│     │ - Generate ASS subtitle file                    │         │
│     │ - Align timestamps with TTS durations           │         │
│     │ - Apply selected style template                 │         │
│     │ - Dual-line scrolling effect                    │         │
│     └─────────────────────────────────────────────────┘         │
│                                                                  │
│  3. Video Segment Cutting (FFmpeg)                               │
│     ┌─────────┐     ┌─────────────────────────────────┐         │
│     │ Source  │──▶  │ ffmpeg -ss {start} -t {duration}│         │
│     │ Videos  │     │ - Cut from center of video      │         │
│     └─────────┘     │ - Duration = TTS + 0.5s overlap │         │
│                     └─────────────────────────────────┘         │
│                                                                  │
│  4. Composition (FFmpeg)                                         │
│     ┌─────────────────────────────────────────────────┐         │
│     │ - Concatenate video segments                    │         │
│     │ - Overlay audio track                           │         │
│     │ - Burn subtitles (if enabled)                   │         │
│     │ - Output: 1080x1920, H.264, 4Mbps               │         │
│     └─────────────────────────────────────────────────┘         │
│                                                                  │
│  5. Upload to OSS                                                │
│     ┌─────────────────────────────────────────────────┐         │
│     │ - Upload final video to OSS                     │         │
│     │ - Generate CDN URL                              │         │
│     │ - Update task status to COMPLETED               │         │
│     └─────────────────────────────────────────────────┘         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**FFmpeg Commands:**

```bash
# Frame extraction (1 frame per 2 seconds)
ffmpeg -i input.mp4 -vf "fps=0.5" -q:v 2 frames/frame_%04d.jpg

# Video segment cutting
ffmpeg -i input.mp4 -ss 10 -t 8.5 -c copy segment.mp4

# Video concatenation with audio overlay
ffmpeg -f concat -safe 0 -i segments.txt -i audio.mp3 \
  -c:v libx264 -c:a aac -map 0:v -map 1:a output.mp4

# Subtitle burning
ffmpeg -i input.mp4 -vf "subtitles=subtitle.ass" output.mp4

# Full composition with subtitle
ffmpeg -f concat -safe 0 -i segments.txt -i audio.mp3 \
  -vf "scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2,subtitles=subtitle.ass" \
  -c:v libx264 -preset medium -crf 23 -c:a aac -b:a 128k \
  -r 30 -movflags +faststart output.mp4
```

**Subtitle Style Templates:**

| Style | Font | Color | Outline | Shadow |
|-------|------|-------|---------|--------|
| 简约白字 | PingFang SC | #FFFFFF | 2px #000000 | Yes |
| 活力黄字 | PingFang SC Bold | #FFD700 | 2px #000000 | Yes |
| 小红书风 | PingFang SC | #FFFFFF | 3px #FF4757 | No |
| 抖音热门 | PingFang SC Bold | #FFFFFF | 2px #000000 | Yes, blur |
| 霓虹炫彩 | PingFang SC Bold | Gradient | Glow effect | Yes |

---

### 4.6 Publish Service

**Responsibilities:**
- Hot topic recommendation
- Title generation
- Content caching

**Key APIs:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/tasks/{id}/publish-assist` | Get topics + titles |
| POST | `/api/v1/tasks/{id}/publish-assist/regenerate` | Regenerate suggestions |

**Doubao Topic/Title Prompt:**

```
基于以下探店视频信息，生成发布辅助内容：

店铺：{{shop_name}}
类型：{{shop_type}}
脚本摘要：{{script_summary}}

请生成：
1. 8-10个相关话题标签（以#开头，符合抖音/小红书规范）
2. 4个吸引眼球的视频标题（20-50字）

输出JSON：
{
  "topics": ["#话题1", "#话题2", ...],
  "titles": ["标题1", "标题2", ...]
}
```

---

## 5. Data Architecture

### 5.1 Database Schema

#### Users Table

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(20) NOT NULL UNIQUE,
    nickname VARCHAR(100),
    avatar_url VARCHAR(500),
    membership_type ENUM('free', 'basic', 'pro') DEFAULT 'free',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Voice Samples Table

```sql
CREATE TABLE voice_samples (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100),
    oss_key VARCHAR(500) NOT NULL,
    duration_seconds INT NOT NULL,
    clone_voice_id VARCHAR(100),  -- Aliyun clone ID
    status ENUM('uploading', 'processing', 'completed', 'failed') DEFAULT 'uploading',
    error_message VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Tasks Table

```sql
CREATE TABLE tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    shop_name VARCHAR(200) NOT NULL,
    shop_type ENUM('food', 'beauty', 'entertainment', 'other') NOT NULL,
    promotion_text TEXT,
    video_style ENUM('recommend', 'review', 'vlog') NOT NULL,
    status ENUM('created', 'uploading', 'analyzing', 'script_ready',
                'script_edited', 'voice_set', 'composing', 'completed', 'failed')
           DEFAULT 'created',
    voice_type VARCHAR(50) DEFAULT 'xiaomei',
    voice_sample_id BIGINT,
    subtitle_enabled BOOLEAN DEFAULT TRUE,
    subtitle_style VARCHAR(50) DEFAULT 'simple_white',
    output_oss_key VARCHAR(500),
    output_duration_seconds INT,
    output_file_size BIGINT,
    error_message VARCHAR(500),
    script_regenerate_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (voice_sample_id) REFERENCES voice_samples(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Videos Table

```sql
CREATE TABLE videos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    oss_key VARCHAR(500) NOT NULL,
    thumbnail_oss_key VARCHAR(500),
    duration_seconds INT,
    file_size BIGINT,
    width INT,
    height INT,
    status ENUM('uploading', 'uploaded', 'analyzing', 'analyzed', 'failed') DEFAULT 'uploading',
    category ENUM('food', 'person', 'environment', 'other'),
    tags JSON,  -- ["tag1", "tag2", ...]
    description VARCHAR(500),
    quality_score INT,  -- 0-100
    is_recommended BOOLEAN DEFAULT FALSE,
    sort_order INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    INDEX idx_task_id (task_id),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Scripts Table

```sql
CREATE TABLE scripts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL UNIQUE,
    content JSON NOT NULL,  -- Full script structure
    version INT DEFAULT 1,
    is_user_edited BOOLEAN DEFAULT FALSE,
    total_duration_seconds INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Script Content JSON Structure:**

```json
{
  "paragraphs": [
    {
      "id": "para_1",
      "section": "开场",
      "shot_id": 123,
      "text": "家人们！今天给你们探一家望京超火的海底捞...",
      "estimated_duration": 8
    }
  ],
  "total_duration": 62
}
```

#### Publish Assist Table

```sql
CREATE TABLE publish_assists (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL UNIQUE,
    topics JSON,  -- ["#topic1", "#topic2", ...]
    titles JSON,  -- ["title1", "title2", ...]
    regenerate_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5.2 Redis Data Structures

| Key Pattern | Type | TTL | Purpose |
|-------------|------|-----|---------|
| `sms:code:{phone}` | String | 5min | SMS verification code |
| `sms:limit:{phone}` | String | 60s | SMS rate limit |
| `login:attempts:{phone}` | String | 15min | Failed login attempts |
| `token:blacklist:{jti}` | String | 7d | Revoked refresh tokens |
| `task:progress:{taskId}` | Hash | 1h | Real-time progress |
| `analysis:result:{videoId}` | String | 24h | Cached analysis result |
| `user:active_tasks:{userId}` | Set | - | User's in-progress tasks |

### 5.3 OSS Storage Structure

```
bucket: shop-video-scout/
├── videos/
│   └── {user_id}/
│       └── {task_id}/
│           ├── {uuid}.mp4          # Original videos
│           └── {uuid}_thumb.jpg    # Thumbnails
├── frames/
│   └── {task_id}/
│       └── {video_id}/
│           └── frame_{n}.jpg       # Extracted frames
├── voice/
│   └── {user_id}/
│       └── {uuid}.mp3              # Voice samples
├── audio/
│   └── {task_id}/
│       └── tts_{paragraph_id}.mp3  # TTS audio segments
├── output/
│   └── {task_id}/
│       ├── final.mp4               # Final composed video
│       └── subtitle.ass            # Subtitle file
└── assets/
    └── {task_id}/
        └── assets.zip              # Downloadable assets pack
```

---

## 6. API Design

### 6.1 API Conventions

**Base URL:** `https://api.shopvideoscout.com/api/v1`

**Request Headers:**

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes* | `Bearer {access_token}` |
| `Content-Type` | Yes | `application/json` |
| `X-Request-ID` | No | Client-generated request ID |

*Except for auth endpoints

**Response Format:**

```json
// Success
{
  "code": 0,
  "message": "success",
  "data": { ... }
}

// Error
{
  "code": 40001,
  "message": "Invalid phone number",
  "details": { ... }
}

// Paginated
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [ ... ],
    "pagination": {
      "page": 1,
      "page_size": 10,
      "total": 100,
      "total_pages": 10
    }
  }
}
```

**Error Codes:**

| Code Range | Category |
|------------|----------|
| 0 | Success |
| 40000-40099 | Authentication errors |
| 40100-40199 | Authorization errors |
| 40200-40299 | Validation errors |
| 40300-40399 | Resource errors |
| 42900 | Rate limit exceeded |
| 50000-50099 | Server errors |
| 50300-50399 | External service errors |

### 6.2 Key API Specifications

#### POST /api/v1/auth/send-code

**Request:**
```json
{
  "phone": "13800138000"
}
```

**Response:**
```json
{
  "code": 0,
  "message": "验证码已发送",
  "data": {
    "expires_in": 300
  }
}
```

#### POST /api/v1/auth/login

**Request:**
```json
{
  "phone": "13800138000",
  "code": "123456"
}
```

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "user": {
      "id": 1,
      "phone": "138****8000",
      "nickname": "用户138000",
      "membership_type": "free"
    },
    "access_token": "eyJhbGciOiJIUzI1NiIs...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
    "expires_in": 900
  }
}
```

#### POST /api/v1/tasks

**Request:**
```json
{
  "shop_name": "海底捞火锅(望京店)",
  "shop_type": "food",
  "promotion_text": "人均89，招牌毛肚七上八下",
  "video_style": "recommend"
}
```

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 12345,
    "status": "created",
    "created_at": "2026-01-17T10:00:00Z"
  }
}
```

#### POST /api/v1/tasks/{id}/videos/upload-url

**Request:**
```json
{
  "filename": "video1.mp4",
  "file_size": 52428800,
  "content_type": "video/mp4"
}
```

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "upload_url": "https://oss-cn-beijing.aliyuncs.com/...",
    "oss_key": "videos/1/12345/abc123.mp4",
    "expires_in": 900
  }
}
```

#### GET /api/v1/tasks/{id}/progress

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "analyzing",
    "phase": "frame_analysis",
    "progress": 65,
    "details": {
      "videos_total": 20,
      "videos_analyzed": 13,
      "current_step": "镜头内容识别",
      "steps": [
        {"name": "帧提取", "status": "completed"},
        {"name": "镜头内容识别", "status": "in_progress"},
        {"name": "质量评估", "status": "pending"},
        {"name": "脚本生成", "status": "pending"}
      ]
    },
    "estimated_remaining_seconds": 60
  }
}
```

#### GET /api/v1/tasks/{id}/script

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "task_id": 12345,
    "version": 1,
    "is_user_edited": false,
    "paragraphs": [
      {
        "id": "para_1",
        "section": "开场",
        "shot": {
          "id": 101,
          "thumbnail_url": "https://cdn.../thumb.jpg",
          "category": "person"
        },
        "text": "家人们！今天给你们探一家望京超火的海底捞...",
        "estimated_duration": 8
      }
    ],
    "total_duration": 62,
    "regenerate_remaining": 4
  }
}
```

#### POST /api/v1/tasks/{id}/compose

**Request:**
```json
{
  "voice_type": "xiaomei",
  "voice_sample_id": null,
  "subtitle_enabled": true,
  "subtitle_style": "simple_white"
}
```

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "composing",
    "estimated_duration_seconds": 360
  }
}
```

#### GET /api/v1/tasks/{id}/output

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "completed",
    "video": {
      "url": "https://cdn.../output/12345/final.mp4",
      "duration_seconds": 62,
      "file_size": 47841280,
      "width": 1080,
      "height": 1920,
      "format": "mp4"
    },
    "shots_used": [
      {
        "id": 101,
        "thumbnail_url": "https://cdn.../thumb.jpg",
        "category": "person",
        "duration_seconds": 8
      }
    ]
  }
}
```

---

## 7. Message Queue Design

### 7.1 Queue Configuration

**Exchange:** `shop-video-scout` (topic exchange)

**Queues:**

| Queue | Routing Key | Consumers | Purpose |
|-------|-------------|-----------|---------|
| `analysis.queue` | `task.analyze` | ai-service (3) | Video analysis jobs |
| `compose.queue` | `task.compose` | media-service (2) | Video composition jobs |
| `clone.queue` | `voice.clone` | media-service (1) | Voice cloning jobs |
| `notify.queue` | `task.notify` | task-service (1) | Status notifications |

### 7.2 Message Formats

**Analysis Job:**
```json
{
  "task_id": 12345,
  "video_ids": [1, 2, 3, ...],
  "shop_info": {
    "name": "海底捞火锅(望京店)",
    "type": "food",
    "promotion": "人均89",
    "style": "recommend"
  },
  "callback_url": "http://task-service/internal/tasks/12345/analysis-complete"
}
```

**Compose Job:**
```json
{
  "task_id": 12345,
  "script": { ... },
  "voice_config": {
    "type": "standard",
    "voice_id": "xiaomei"
  },
  "subtitle_config": {
    "enabled": true,
    "style": "simple_white"
  },
  "callback_url": "http://task-service/internal/tasks/12345/compose-complete"
}
```

### 7.3 Retry Strategy

| Queue | Max Retries | Retry Delay | Dead Letter Queue |
|-------|-------------|-------------|-------------------|
| `analysis.queue` | 3 | 30s, 60s, 120s | `analysis.dlq` |
| `compose.queue` | 2 | 60s, 120s | `compose.dlq` |
| `clone.queue` | 3 | 60s, 120s, 300s | `clone.dlq` |

---

## 8. Infrastructure

### 8.1 Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Aliyun Cloud                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    VPC (10.0.0.0/16)                     │    │
│  ├─────────────────────────────────────────────────────────┤    │
│  │                                                          │    │
│  │  Public Subnet (10.0.1.0/24)                             │    │
│  │  ┌──────────┐  ┌──────────┐                              │    │
│  │  │   SLB    │  │   NAT    │                              │    │
│  │  │ (Public) │  │ Gateway  │                              │    │
│  │  └────┬─────┘  └──────────┘                              │    │
│  │       │                                                   │    │
│  │  ─────┼───────────────────────────────────────────────   │    │
│  │       │                                                   │    │
│  │  Private Subnet (10.0.2.0/24)                            │    │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐               │    │
│  │  │ Gateway  │  │ Gateway  │  │  Nacos   │               │    │
│  │  │  Pod 1   │  │  Pod 2   │  │ Cluster  │               │    │
│  │  └──────────┘  └──────────┘  └──────────┘               │    │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐               │    │
│  │  │  User    │  │  Task    │  │   AI     │               │    │
│  │  │ Service  │  │ Service  │  │ Service  │               │    │
│  │  │ (2 pods) │  │ (2 pods) │  │ (3 pods) │               │    │
│  │  └──────────┘  └──────────┘  └──────────┘               │    │
│  │  ┌──────────┐  ┌──────────┐                              │    │
│  │  │  Media   │  │ Publish  │                              │    │
│  │  │ Service  │  │ Service  │                              │    │
│  │  │ (2 pods) │  │ (1 pod)  │                              │    │
│  │  └──────────┘  └──────────┘                              │    │
│  │                                                          │    │
│  │  ─────────────────────────────────────────────────────   │    │
│  │                                                          │    │
│  │  Data Subnet (10.0.3.0/24)                               │    │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐               │    │
│  │  │  MySQL   │  │  Redis   │  │ RabbitMQ │               │    │
│  │  │ Primary  │  │ Cluster  │  │ Cluster  │               │    │
│  │  │+ Replica │  │ (3 node) │  │ (3 node) │               │    │
│  │  └──────────┘  └──────────┘  └──────────┘               │    │
│  │                                                          │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  External Services                                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │   OSS    │  │   CDN    │  │   SMS    │  │   SLS    │        │
│  │ (Videos) │  │(Delivery)│  │ Service  │  │ (Logs)   │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 8.2 Container Specifications

| Service | CPU | Memory | Replicas | Auto-scale |
|---------|-----|--------|----------|------------|
| gateway | 0.5 | 1Gi | 2 | 2-4 (CPU 70%) |
| user-service | 0.5 | 1Gi | 2 | 2-4 (CPU 70%) |
| task-service | 0.5 | 1Gi | 2 | 2-4 (CPU 70%) |
| ai-service | 1 | 2Gi | 3 | 3-6 (Queue depth) |
| media-service | 2 | 4Gi | 2 | 2-4 (Queue depth) |
| publish-service | 0.25 | 512Mi | 1 | 1-2 (CPU 70%) |

### 8.3 Database Specifications

| Component | Specification | High Availability |
|-----------|---------------|-------------------|
| MySQL | 4 vCPU, 16GB RAM, 500GB SSD | Primary + Read Replica |
| Redis | 4GB Cluster (3 nodes) | Automatic failover |
| RabbitMQ | 2 vCPU, 4GB RAM (3 nodes) | Mirrored queues |

---

## 9. Security

### 9.1 Authentication & Authorization

**Authentication Flow:**

```
┌────────┐     ┌─────────┐     ┌──────────────┐     ┌─────────┐
│ Client │────▶│ Gateway │────▶│ User Service │────▶│  Redis  │
└────────┘     └─────────┘     └──────────────┘     └─────────┘
     │              │                  │                  │
     │  1. Login    │                  │                  │
     │─────────────▶│  2. Forward     │                  │
     │              │─────────────────▶│  3. Verify code │
     │              │                  │─────────────────▶│
     │              │                  │◀─────────────────│
     │              │  4. JWT tokens  │                  │
     │◀─────────────│◀─────────────────│                  │
     │              │                  │                  │
     │  5. API call │                  │                  │
     │─────────────▶│  6. Validate JWT│                  │
     │              │─────────────────▶│                  │
     │              │  7. Forward     │                  │
     │              │─────────────────▶│ (downstream)    │
```

**JWT Validation (Gateway):**
1. Extract token from `Authorization` header
2. Verify signature using secret key
3. Check token expiration
4. Check token not in blacklist (Redis)
5. Add user context to request headers

### 9.2 Data Security

| Data Type | At Rest | In Transit | Access Control |
|-----------|---------|------------|----------------|
| User credentials | N/A (phone only) | TLS 1.3 | - |
| Video files | OSS encryption | HTTPS | Signed URLs (1h) |
| Voice samples | OSS encryption | HTTPS | User-only access |
| JWT tokens | N/A | HTTPS | Short expiry |

### 9.3 API Security

| Measure | Implementation |
|---------|----------------|
| Rate Limiting | Per-user and per-IP limits |
| Input Validation | JSON schema validation |
| SQL Injection | Parameterized queries (MyBatis) |
| XSS | Response escaping, CSP headers |
| CORS | Whitelist allowed origins |
| HTTPS | TLS 1.3 enforced |

### 9.4 OSS Security

```java
// Generate presigned URL with expiration
public String generatePresignedUrl(String objectKey, int expirationMinutes) {
    Date expiration = new Date(System.currentTimeMillis() + expirationMinutes * 60 * 1000);
    GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, objectKey);
    request.setExpiration(expiration);
    request.setMethod(HttpMethod.PUT);  // or GET for download
    return ossClient.generatePresignedUrl(request).toString();
}
```

---

## 10. Monitoring & Observability

### 10.1 Logging

**Log Format (JSON):**
```json
{
  "timestamp": "2026-01-17T10:00:00.000Z",
  "level": "INFO",
  "service": "task-service",
  "traceId": "abc123",
  "spanId": "def456",
  "userId": 1,
  "message": "Task created",
  "context": {
    "taskId": 12345,
    "shopName": "海底捞"
  }
}
```

**Log Levels:**
- ERROR: System errors, external service failures
- WARN: Recoverable issues, rate limits
- INFO: Business events, API requests
- DEBUG: Detailed debugging (dev only)

### 10.2 Metrics

**Application Metrics:**

| Metric | Type | Labels |
|--------|------|--------|
| `http_request_total` | Counter | method, path, status |
| `http_request_duration_seconds` | Histogram | method, path |
| `task_created_total` | Counter | shop_type, video_style |
| `task_completed_total` | Counter | status |
| `task_duration_seconds` | Histogram | phase |
| `ai_api_calls_total` | Counter | service, status |
| `ai_api_latency_seconds` | Histogram | service |
| `queue_depth` | Gauge | queue_name |

**Infrastructure Metrics:**
- CPU, Memory, Disk usage
- Network I/O
- Database connections
- Redis memory usage

### 10.3 Alerting

| Alert | Condition | Severity | Action |
|-------|-----------|----------|--------|
| High Error Rate | 5xx > 1% for 5min | Critical | PagerDuty |
| API Latency | p99 > 5s for 5min | Warning | Slack |
| Queue Backup | depth > 100 for 10min | Warning | Slack |
| Database CPU | > 80% for 10min | Warning | Slack |
| AI Service Failure | failures > 10 in 5min | Critical | PagerDuty |
| Disk Space | < 20% free | Warning | Slack |

### 10.4 Tracing

**Distributed Tracing:** Spring Cloud Sleuth + Zipkin

**Trace Context:**
- Propagate `X-Request-ID` across services
- Include in all log entries
- Store in response headers

---

## 11. Cost Estimation

### 11.1 Per-Task Cost Breakdown

| Component | Unit Cost | Units/Task | Cost/Task |
|-----------|-----------|------------|-----------|
| Qwen-VL (frame analysis) | ¥0.008/image | ~50 frames | ¥0.40 |
| Doubao (script) | ¥0.012/1K tokens | ~2K tokens | ¥0.024 |
| Doubao (topics/titles) | ¥0.012/1K tokens | ~1K tokens | ¥0.012 |
| Seed-TTS (火山引擎) | ¥0.002/character | ~500 chars | ¥1.00 |
| OSS Storage | ¥0.12/GB/month | ~200MB | ¥0.024 |
| CDN Transfer | ¥0.24/GB | ~50MB | ¥0.012 |
| Compute (processing) | ¥0.5/hour | ~0.2 hours | ¥0.10 |
| **Total** | | | **~¥1.57** |

*Note: Voice cloning (Seed-ICL) adds ~¥1.50/sample (one-time, 5秒样本即可)*

### 11.2 Infrastructure Monthly Cost (Estimated)

| Component | Specification | Monthly Cost |
|-----------|---------------|--------------|
| ECS (Services) | 8 vCPU, 16GB x 3 | ¥2,400 |
| RDS MySQL | 4 vCPU, 16GB | ¥1,200 |
| Redis | 4GB Cluster | ¥600 |
| RabbitMQ | Self-hosted on ECS | (included) |
| OSS | 1TB storage | ¥120 |
| CDN | 500GB transfer | ¥120 |
| SLB | Standard | ¥100 |
| **Total** | | **~¥4,540/month** |

---

## 12. Development Guidelines

### 12.1 Code Organization

```
shop-video-scout/
├── frontend/                    # Vue 3 SPA
│   ├── src/
│   │   ├── api/                # API client
│   │   ├── components/         # Reusable components
│   │   ├── composables/        # Vue composables
│   │   ├── layouts/            # Page layouts
│   │   ├── pages/              # Page components
│   │   ├── router/             # Vue Router
│   │   ├── stores/             # Pinia stores
│   │   ├── styles/             # Global styles
│   │   └── utils/              # Utilities
│   └── ...
├── backend/
│   ├── gateway/                # API Gateway
│   ├── user-service/           # User & Auth
│   ├── task-service/           # Task Management
│   ├── ai-service/             # AI Processing
│   ├── media-service/          # Media Processing
│   ├── publish-service/        # Publish Assist
│   └── common/                 # Shared libraries
├── docs/                       # Documentation
├── scripts/                    # Deployment scripts
└── docker-compose.yml          # Local development
```

### 12.2 API Versioning

- URL path versioning: `/api/v1/`, `/api/v2/`
- Breaking changes require new version
- Support previous version for 6 months minimum
- Deprecation notices in response headers

### 12.3 Database Migrations

- Use Flyway for schema migrations
- Migration files: `V{version}__{description}.sql`
- Always provide rollback scripts
- Test migrations on staging before production

### 12.4 Testing Strategy

| Test Type | Coverage Target | Tools |
|-----------|-----------------|-------|
| Unit Tests | 80% line coverage | JUnit 5, Mockito |
| Integration Tests | Critical paths | Spring Boot Test |
| E2E Tests | Happy paths | Cypress |
| Load Tests | Before release | k6, JMeter |

---

## 13. Deployment

### 13.1 CI/CD Pipeline

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│  Code   │────▶│  Build  │────▶│  Test   │────▶│ Deploy  │
│  Push   │     │         │     │         │     │         │
└─────────┘     └─────────┘     └─────────┘     └─────────┘
                    │               │               │
                    ▼               ▼               ▼
              ┌─────────┐     ┌─────────┐     ┌─────────┐
              │ Compile │     │  Unit   │     │ Staging │
              │  Build  │     │ Integr. │     │  Prod   │
              │ Docker  │     │  E2E    │     │         │
              └─────────┘     └─────────┘     └─────────┘
```

### 13.2 Deployment Strategy

- **Staging:** Auto-deploy on merge to `develop`
- **Production:** Manual approval after staging validation
- **Rollback:** Keep last 3 versions, one-click rollback
- **Blue-Green:** For zero-downtime deployments

### 13.3 Environment Configuration

| Environment | Database | External Services |
|-------------|----------|-------------------|
| Local | Docker MySQL | Mock services |
| Development | Shared dev DB | Sandbox APIs |
| Staging | Staging DB | Sandbox APIs |
| Production | Production DB | Production APIs |

---

## 14. Future Considerations (V2.0)

### 14.1 iOS App Support

- API already versioned and mobile-ready
- Add push notification support (APNs)
- Consider GraphQL for mobile efficiency
- Add offline mode considerations

### 14.2 Scalability

- Horizontal scaling for AI/Media services
- Read replicas for MySQL
- CDN for static assets
- Consider Kubernetes for orchestration

### 14.3 Feature Extensions

- Multi-language support
- Custom voice training (advanced)
- Social sharing integrations
- Analytics dashboard

---

## Appendix A: API Reference

See [API Documentation](./api-reference.md) for complete OpenAPI specification.

## Appendix B: Database ERD

See [Database Schema](./database-schema.md) for detailed ERD diagram.

## Appendix C: Glossary

| Term | Definition |
|------|------------|
| Task | A single video creation job |
| Shot | A video clip/segment |
| Frame | A single image extracted from video |
| Script | The voiceover text for the video |
| Voice Sample | User-uploaded audio for voice cloning |
| Composition | The process of combining video+audio+subtitles |
