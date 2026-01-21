# 探店宝 (Shop Video Scout) - API Reference

---

## 1. Overview

### 1.1 Base URL

| Environment | Base URL |
|-------------|----------|
| Production | `https://api.shopvideoscout.com/api/v1` |
| Staging | `https://api-staging.shopvideoscout.com/api/v1` |
| Development | `http://localhost:8080/api/v1` |

### 1.2 Authentication

All API requests (except auth endpoints) require a valid JWT access token in the Authorization header:

```
Authorization: Bearer {access_token}
```

### 1.3 Common Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes* | `Bearer {access_token}` (*except auth endpoints) |
| `Content-Type` | Yes | `application/json` |
| `X-Request-ID` | No | Client-generated request ID for tracing |

### 1.4 Response Format

**Success Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": { ... }
}
```

**Error Response:**
```json
{
  "code": 40001,
  "message": "Invalid phone number",
  "details": { ... }
}
```

**Paginated Response:**
```json
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

### 1.5 Error Codes

| Code Range | Category | Description |
|------------|----------|-------------|
| 0 | Success | Request completed successfully |
| 40000-40099 | Authentication | Login, token, verification errors |
| 40100-40199 | Authorization | Permission denied, resource access |
| 40200-40299 | Validation | Input validation failures |
| 40300-40399 | Resource | Not found, conflict, state errors |
| 42900 | Rate Limit | Too many requests |
| 50000-50099 | Server | Internal server errors |
| 50300-50399 | External Service | Third-party service failures |

### 1.6 Change Log

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2026-01-17 | 1.0 | Initial API Reference | Architect |

---

## 2. Authentication APIs

### 2.1 Send Verification Code

Send SMS verification code to phone number.

**Endpoint:** `POST /auth/send-code`

**Authentication:** Not required

**Rate Limit:** 1 request per 60 seconds per phone number

**Request Body:**
```json
{
  "phone": "13800138000"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| phone | string | Yes | China mobile phone number (11 digits, starts with 1) |

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

| Field | Type | Description |
|-------|------|-------------|
| expires_in | integer | Code expiration time in seconds |

**Error Responses:**

| Code | Message | Condition |
|------|---------|-----------|
| 40200 | 请输入正确的手机号 | Invalid phone format |
| 42900 | 请求过于频繁，请60秒后重试 | Rate limit exceeded |
| 50300 | 短信服务暂时不可用 | SMS service unavailable |

---

### 2.2 Login

Authenticate user with phone number and verification code.

**Endpoint:** `POST /auth/login`

**Authentication:** Not required

**Rate Limit:** 5 requests per 60 seconds per phone number

**Request Body:**
```json
{
  "phone": "13800138000",
  "code": "123456"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| phone | string | Yes | China mobile phone number |
| code | string | Yes | 6-digit verification code |

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
      "avatar_url": null,
      "membership_type": "free",
      "created_at": "2026-01-17T10:00:00Z"
    },
    "access_token": "eyJhbGciOiJIUzI1NiIs...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
    "expires_in": 900
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| user | object | User profile object |
| access_token | string | JWT access token (15 min expiry) |
| refresh_token | string | JWT refresh token (7 day expiry) |
| expires_in | integer | Access token expiration in seconds |

**Error Responses:**

| Code | Message | Condition |
|------|---------|-----------|
| 40001 | 验证码错误，请重新输入 | Incorrect verification code |
| 40002 | 验证码已过期，请重新获取 | Expired verification code |
| 40003 | 验证码错误次数过多，请15分钟后重试 | Account locked (5+ failed attempts) |

---

### 2.3 Refresh Token

Refresh access token using refresh token.

**Endpoint:** `POST /auth/refresh`

**Authentication:** Not required

**Request Body:**
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIs...",
    "expires_in": 900
  }
}
```

**Error Responses:**

| Code | Message | Condition |
|------|---------|-----------|
| 40004 | 刷新令牌无效或已过期 | Invalid or expired refresh token |

---

### 2.4 Logout

Invalidate current tokens.

**Endpoint:** `POST /auth/logout`

**Authentication:** Required

**Request Body:** None

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

## 3. User APIs

### 3.1 Get Current User

Get current authenticated user's profile.

**Endpoint:** `GET /users/me`

**Authentication:** Required

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "phone": "138****8000",
    "nickname": "用户138000",
    "avatar_url": "https://cdn.../avatar.jpg",
    "membership_type": "free",
    "created_at": "2026-01-17T10:00:00Z",
    "updated_at": "2026-01-17T10:00:00Z"
  }
}
```

---

## 4. Voice Sample APIs

### 4.1 List Voice Samples

Get all voice samples for current user.

**Endpoint:** `GET /voice/samples`

**Authentication:** Required

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "name": "我的声音1",
        "duration_seconds": 45,
        "status": "completed",
        "clone_voice_id": "voice_abc123",
        "preview_url": "https://cdn.../preview.mp3",
        "created_at": "2026-01-17T10:00:00Z"
      }
    ],
    "max_count": 3,
    "current_count": 1
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| items | array | List of voice samples |
| max_count | integer | Maximum allowed samples (3) |
| current_count | integer | Current number of samples |

**Voice Sample Status:**

| Status | Description |
|--------|-------------|
| uploading | Sample is being uploaded |
| processing | Voice cloning in progress |
| completed | Clone successful, ready to use |
| failed | Clone failed |

---

### 4.2 Get Voice Upload URL

Get presigned URL for uploading voice sample.

**Endpoint:** `POST /voice/upload-url`

**Authentication:** Required

**Request Body:**
```json
{
  "filename": "my_voice.mp3",
  "file_size": 2097152,
  "content_type": "audio/mpeg"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| filename | string | Yes | Original filename |
| file_size | integer | Yes | File size in bytes (max 20MB) |
| content_type | string | Yes | MIME type (audio/mpeg, audio/wav, audio/x-m4a) |

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "upload_url": "https://oss-cn-beijing.aliyuncs.com/...",
    "oss_key": "voice/1/abc123.mp3",
    "expires_in": 900
  }
}
```

**Error Responses:**

| Code | Message | Condition |
|------|---------|-----------|
| 40200 | 仅支持MP3、WAV、M4A格式 | Invalid audio format |
| 40201 | 声音样本不能超过20MB | File too large |
| 40300 | 您已有3个声音样本，请删除后再上传 | Sample limit reached |

---

### 4.3 Create Voice Sample

Confirm upload and start voice cloning.

**Endpoint:** `POST /voice/samples`

**Authentication:** Required

**Request Body:**
```json
{
  "name": "我的声音1",
  "oss_key": "voice/1/abc123.mp3",
  "duration_seconds": 45
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | string | No | Sample name (auto-generated if not provided) |
| oss_key | string | Yes | OSS key from upload-url response |
| duration_seconds | integer | Yes | Audio duration (30-120 seconds) |

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "name": "我的声音1",
    "status": "processing",
    "estimated_minutes": 5,
    "created_at": "2026-01-17T10:00:00Z"
  }
}
```

**Error Responses:**

| Code | Message | Condition |
|------|---------|-----------|
| 40202 | 声音样本时长需要在30秒到2分钟之间 | Duration out of range |

---

### 4.4 Get Voice Sample

Get single voice sample details.

**Endpoint:** `GET /voice/samples/{id}`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Voice sample ID |

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "name": "我的声音1",
    "duration_seconds": 45,
    "status": "completed",
    "clone_voice_id": "voice_abc123",
    "preview_url": "https://cdn.../preview.mp3",
    "error_message": null,
    "created_at": "2026-01-17T10:00:00Z",
    "updated_at": "2026-01-17T10:05:00Z"
  }
}
```

---

### 4.5 Delete Voice Sample

Delete a voice sample.

**Endpoint:** `DELETE /voice/samples/{id}`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Voice sample ID |

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

## 5. Task APIs

### 5.1 Create Task

Create a new video creation task.

**Endpoint:** `POST /tasks`

**Authentication:** Required

**Rate Limit:** 10 requests per 60 seconds per user

**Request Body:**
```json
{
  "shop_name": "海底捞火锅(望京店)",
  "shop_type": "food",
  "promotion_text": "人均89，招牌毛肚七上八下",
  "video_style": "recommend"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| shop_name | string | Yes | Shop name (1-200 characters) |
| shop_type | enum | Yes | `food`, `beauty`, `entertainment`, `other` |
| promotion_text | string | No | Promotion description (max 500 characters) |
| video_style | enum | Yes | `recommend`, `review`, `vlog` |

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

**Error Responses:**

| Code | Message | Condition |
|------|---------|-----------|
| 40200 | 请输入店铺名称 | Missing shop_name |
| 40201 | 请选择店铺类型 | Missing shop_type |
| 40202 | 请选择视频风格 | Missing video_style |
| 42900 | 您有太多进行中的任务 | More than 5 active tasks |

---

### 5.2 List Tasks

Get paginated list of user's tasks.

**Endpoint:** `GET /tasks`

**Authentication:** Required

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | integer | 1 | Page number |
| page_size | integer | 10 | Items per page (max 50) |
| status | string | - | Filter by status |

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 12345,
        "shop_name": "海底捞火锅(望京店)",
        "shop_type": "food",
        "video_style": "recommend",
        "status": "completed",
        "thumbnail_url": "https://cdn.../thumb.jpg",
        "output_duration_seconds": 62,
        "created_at": "2026-01-17T10:00:00Z",
        "updated_at": "2026-01-17T10:15:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "page_size": 10,
      "total": 25,
      "total_pages": 3
    }
  }
}
```

**Task Status Values:**

| Status | Description |
|--------|-------------|
| created | Task created, ready for video upload |
| uploading | Videos being uploaded |
| analyzing | AI analyzing video frames |
| script_ready | Script generated, ready for editing |
| script_edited | User confirmed script |
| voice_set | Voice and subtitle settings confirmed |
| composing | Video composition in progress |
| completed | Final video ready |
| failed | Task failed |

---

### 5.3 Get Task

Get task details.

**Endpoint:** `GET /tasks/{id}`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 12345,
    "shop_name": "海底捞火锅(望京店)",
    "shop_type": "food",
    "promotion_text": "人均89，招牌毛肚七上八下",
    "video_style": "recommend",
    "status": "script_ready",
    "voice_type": "xiaomei",
    "voice_sample_id": null,
    "subtitle_enabled": true,
    "subtitle_style": "simple_white",
    "script_regenerate_count": 0,
    "error_message": null,
    "created_at": "2026-01-17T10:00:00Z",
    "updated_at": "2026-01-17T10:10:00Z"
  }
}
```

---

### 5.4 Delete Task

Delete a task and all associated files.

**Endpoint:** `DELETE /tasks/{id}`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

## 6. Video Upload APIs

### 6.1 Get Video Upload URL

Get presigned URL for uploading video.

**Endpoint:** `POST /tasks/{id}/videos/upload-url`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

**Request Body:**
```json
{
  "filename": "video1.mp4",
  "file_size": 52428800,
  "content_type": "video/mp4"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| filename | string | Yes | Original filename |
| file_size | integer | Yes | File size in bytes (max 100MB) |
| content_type | string | Yes | MIME type (video/mp4, video/quicktime) |

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

**Error Responses:**

| Code | Message | Condition |
|------|---------|-----------|
| 40200 | 仅支持MP4和MOV格式 | Invalid video format |
| 40201 | 单个视频不能超过100MB | File too large |
| 40300 | 每个任务最多上传20个视频 | Video limit reached |

---

### 6.2 Confirm Video Upload

Confirm video upload completion and process metadata.

**Endpoint:** `POST /tasks/{id}/videos`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

**Request Body:**
```json
{
  "oss_key": "videos/1/12345/abc123.mp4",
  "original_filename": "店门口.mp4"
}
```

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "original_filename": "店门口.mp4",
    "thumbnail_url": "https://cdn.../thumb.jpg",
    "duration_seconds": 45,
    "width": 1080,
    "height": 1920,
    "file_size": 52428800,
    "status": "uploaded",
    "created_at": "2026-01-17T10:00:00Z"
  }
}
```

**Error Responses:**

| Code | Message | Condition |
|------|---------|-----------|
| 40203 | 视频时长不能超过3分钟 | Duration exceeds 3 minutes |

---

### 6.3 List Task Videos

Get all videos for a task.

**Endpoint:** `GET /tasks/{id}/videos`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "original_filename": "店门口.mp4",
        "thumbnail_url": "https://cdn.../thumb.jpg",
        "duration_seconds": 45,
        "status": "analyzed",
        "category": "environment",
        "tags": ["店面", "门头", "装修"],
        "description": "店铺门口外观，装修现代",
        "quality_score": 85,
        "is_recommended": true,
        "sort_order": 1
      }
    ],
    "total": 15,
    "by_category": {
      "food": 5,
      "person": 3,
      "environment": 4,
      "other": 3
    }
  }
}
```

---

### 6.4 Delete Video

Delete a video from task.

**Endpoint:** `DELETE /tasks/{id}/videos/{videoId}`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |
| videoId | integer | Video ID |

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

## 7. AI Analysis APIs

### 7.1 Start Analysis

Start AI analysis on uploaded videos.

**Endpoint:** `POST /tasks/{id}/analyze`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

**Request Body:** None

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "analyzing",
    "estimated_minutes": 3,
    "started_at": "2026-01-17T10:00:00Z"
  }
}
```

**Error Responses:**

| Code | Message | Condition |
|------|---------|-----------|
| 40300 | 请至少上传10个视频 | Less than 10 videos |
| 40301 | 任务状态不正确 | Task not in uploadable state |

---

### 7.2 Get Task Progress

Get current task processing progress.

**Endpoint:** `GET /tasks/{id}/progress`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

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

**Progress Phases:**

| Phase | Description |
|-------|-------------|
| frame_extraction | Extracting frames from videos |
| frame_analysis | AI analyzing frame content |
| quality_scoring | Evaluating frame quality |
| script_generation | Generating voiceover script |
| tts_synthesis | Generating TTS audio |
| video_cutting | Cutting video segments |
| composition | Composing final video |

---

## 8. Script APIs

### 8.1 Get Script

Get generated script for task.

**Endpoint:** `GET /tasks/{id}/script`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

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
      },
      {
        "id": "para_2",
        "section": "环境介绍",
        "shot": {
          "id": 102,
          "thumbnail_url": "https://cdn.../thumb2.jpg",
          "category": "environment"
        },
        "text": "一进门就被这个装修惊艳到了...",
        "estimated_duration": 6
      }
    ],
    "total_duration": 62,
    "regenerate_remaining": 4
  }
}
```

---

### 8.2 Update Script

Update script content.

**Endpoint:** `PUT /tasks/{id}/script`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

**Request Body:**
```json
{
  "paragraphs": [
    {
      "id": "para_1",
      "section": "开场",
      "shot_id": 101,
      "text": "家人们！今天必须给你们安利这家...",
      "estimated_duration": 8
    }
  ]
}
```

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "version": 2,
    "is_user_edited": true,
    "updated_at": "2026-01-17T10:10:00Z"
  }
}
```

---

### 8.3 Regenerate Script

Regenerate script using AI.

**Endpoint:** `POST /tasks/{id}/regenerate-script`

**Authentication:** Required

**Rate Limit:** 5 regenerations per task

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

**Request Body:** None

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "regenerating",
    "regenerate_remaining": 3
  }
}
```

**Error Responses:**

| Code | Message | Condition |
|------|---------|-----------|
| 42900 | 重新生成次数已达上限，请手动编辑 | Limit exceeded (5 times) |

---

## 9. Composition APIs

### 9.1 Set Voice Type

Set voice configuration for task.

**Endpoint:** `PUT /tasks/{id}/voice-type`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

**Request Body:**
```json
{
  "voice_type": "xiaomei",
  "voice_sample_id": null
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| voice_type | string | Yes | Standard voice: `xiaomei`, `xiaoshuai`, `xiaoya` |
| voice_sample_id | integer | No | Custom voice sample ID (overrides voice_type) |

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "voice_type": "xiaomei",
    "voice_sample_id": null,
    "preview_url": "https://cdn.../xiaomei_preview.mp3"
  }
}
```

---

### 9.2 Set Subtitle Settings

Configure subtitle options.

**Endpoint:** `PUT /tasks/{id}/subtitle-settings`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

**Request Body:**
```json
{
  "subtitle_enabled": true,
  "subtitle_style": "simple_white"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| subtitle_enabled | boolean | Yes | Enable/disable subtitles |
| subtitle_style | string | No | Style: `simple_white`, `vibrant_yellow`, `xiaohongshu`, `douyin_hot`, `neon_glow` |

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "subtitle_enabled": true,
    "subtitle_style": "simple_white",
    "style_preview_url": "https://cdn.../style_simple_white.png"
  }
}
```

**Subtitle Styles:**

| Style | Description |
|-------|-------------|
| simple_white | 简约白字 - White text with black outline |
| vibrant_yellow | 活力黄字 - Yellow bold text |
| xiaohongshu | 小红书风 - White with red outline |
| douyin_hot | 抖音热门 - White bold with blur shadow |
| neon_glow | 霓虹炫彩 - Gradient with glow effect |

---

### 9.3 Start Composition

Start video composition process.

**Endpoint:** `POST /tasks/{id}/compose`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

**Request Body:**
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

**Error Responses:**

| Code | Message | Condition |
|------|---------|-----------|
| 40301 | 请先确认脚本内容 | Script not confirmed |
| 40302 | 声音克隆尚未完成 | Custom voice still processing |

---

### 9.4 Get Composition Progress

Get video composition progress.

**Endpoint:** `GET /tasks/{id}/compose-progress`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "composing",
    "phase": "tts_synthesis",
    "progress": 45,
    "details": {
      "paragraphs_total": 6,
      "paragraphs_completed": 3,
      "current_step": "配音合成中"
    },
    "estimated_remaining_seconds": 180
  }
}
```

---

## 10. Output APIs

### 10.1 Get Task Output

Get completed task output information.

**Endpoint:** `GET /tasks/{id}/output`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

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
    ],
    "completed_at": "2026-01-17T10:15:00Z"
  }
}
```

---

### 10.2 Get Download URL

Get signed download URL for output video.

**Endpoint:** `GET /tasks/{id}/output/download`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "download_url": "https://oss.../output/12345/final.mp4?...",
    "filename": "海底捞火锅-探店视频-20260117.mp4",
    "expires_in": 3600
  }
}
```

---

### 10.3 Get Assets Pack

Get download URL for recommended shots assets pack.

**Endpoint:** `GET /tasks/{id}/assets-pack`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "download_url": "https://oss.../assets/12345/assets.zip?...",
    "filename": "海底捞火锅-素材包-20260117.zip",
    "file_size": 157286400,
    "expires_in": 3600
  }
}
```

---

## 11. Publish Assist APIs

### 11.1 Get Publish Assist

Get AI-generated topics and titles.

**Endpoint:** `GET /tasks/{id}/publish-assist`

**Authentication:** Required

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "topics": [
      "#海底捞",
      "#火锅探店",
      "#望京美食",
      "#必吃榜",
      "#美食推荐",
      "#吃货日常",
      "#探店达人",
      "#北京美食",
      "#团购优惠",
      "#周末去哪吃"
    ],
    "titles": [
      "望京这家海底捞太绝了！服务好到让你感动流泪",
      "人均89吃海底捞？这个团购也太香了吧！",
      "海底捞探店｜必点招牌毛肚，七上八下太嫩了",
      "朋友聚餐首选！望京海底捞氛围感拉满"
    ],
    "regenerate_remaining": 2
  }
}
```

---

### 11.2 Regenerate Publish Assist

Regenerate topics and titles.

**Endpoint:** `POST /tasks/{id}/publish-assist/regenerate`

**Authentication:** Required

**Rate Limit:** 2 regenerations per task

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Task ID |

**Response:**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "topics": ["#新话题1", "..."],
    "titles": ["新标题1", "..."],
    "regenerate_remaining": 1
  }
}
```

---

## 12. Rate Limiting

### 12.1 Rate Limit Configuration

| Endpoint Pattern | Rate Limit | Window |
|------------------|------------|--------|
| `/auth/send-code` | 1 request | 60 seconds (per phone) |
| `/auth/login` | 5 requests | 60 seconds (per phone) |
| `/tasks` (POST) | 10 requests | 60 seconds (per user) |
| `/*/regenerate*` | 5 requests | 300 seconds (per task) |
| Default | 100 requests | 60 seconds (per user) |

### 12.2 Rate Limit Headers

Rate limit information is returned in response headers:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705401660
```

### 12.3 Rate Limit Error

When rate limit is exceeded:

```json
{
  "code": 42900,
  "message": "请求过于频繁，请稍后重试",
  "details": {
    "retry_after": 30
  }
}
```

---

## 13. Webhooks (Future)

> **Note:** Webhook support is planned for V2.0 to enable real-time status updates.

### Planned Webhook Events

| Event | Description |
|-------|-------------|
| `task.analysis_complete` | AI analysis finished |
| `task.composition_complete` | Video composition finished |
| `task.failed` | Task failed |
| `voice.clone_complete` | Voice cloning finished |

---

## Appendix A: Data Types

### A.1 Shop Type Enum

| Value | Display Name |
|-------|--------------|
| food | 餐饮美食 |
| beauty | 美容美发 |
| entertainment | 休闲娱乐 |
| other | 其他 |

### A.2 Video Style Enum

| Value | Display Name |
|-------|--------------|
| recommend | 种草安利型 |
| review | 真实测评型 |
| vlog | 探店vlog型 |

### A.3 Task Status Enum

| Value | Description |
|-------|-------------|
| created | 任务已创建 |
| uploading | 视频上传中 |
| analyzing | AI分析中 |
| script_ready | 脚本已生成 |
| script_edited | 脚本已编辑 |
| voice_set | 配音已设置 |
| composing | 视频合成中 |
| completed | 已完成 |
| failed | 失败 |

### A.4 Video Category Enum

| Value | Description |
|-------|-------------|
| food | 食物 |
| person | 人物 |
| environment | 环境 |
| other | 其他 |

### A.5 Voice Type Enum

| Value | Display Name | Description |
|-------|--------------|-------------|
| xiaomei | 活泼女声 | Energetic female voice |
| xiaoshuai | 阳光男声 | Upbeat male voice |
| xiaoya | 知性女声 | Intellectual female voice |

### A.6 Subtitle Style Enum

| Value | Display Name |
|-------|--------------|
| simple_white | 简约白字 |
| vibrant_yellow | 活力黄字 |
| xiaohongshu | 小红书风 |
| douyin_hot | 抖音热门 |
| neon_glow | 霓虹炫彩 |

---

## Appendix B: OpenAPI Specification

The complete OpenAPI 3.0 specification is available at:

- **Production:** `https://api.shopvideoscout.com/api/v1/openapi.json`
- **Swagger UI:** `https://api.shopvideoscout.com/api/docs`

---

## Appendix C: SDK Examples

### C.1 JavaScript/TypeScript

```typescript
import { ShopVideoScoutClient } from '@shopvideoscout/sdk';

const client = new ShopVideoScoutClient({
  baseUrl: 'https://api.shopvideoscout.com/api/v1',
  accessToken: 'your-access-token'
});

// Create task
const task = await client.tasks.create({
  shop_name: '海底捞火锅(望京店)',
  shop_type: 'food',
  video_style: 'recommend'
});

// Upload video
const uploadUrl = await client.tasks.getVideoUploadUrl(task.id, {
  filename: 'video.mp4',
  file_size: 52428800,
  content_type: 'video/mp4'
});

// Start analysis
await client.tasks.analyze(task.id);

// Poll for progress
const progress = await client.tasks.getProgress(task.id);
```

### C.2 Python

```python
from shopvideoscout import Client

client = Client(
    base_url="https://api.shopvideoscout.com/api/v1",
    access_token="your-access-token"
)

# Create task
task = client.tasks.create(
    shop_name="海底捞火锅(望京店)",
    shop_type="food",
    video_style="recommend"
)

# Upload video
upload_info = client.tasks.get_video_upload_url(
    task_id=task.id,
    filename="video.mp4",
    file_size=52428800
)

# Start analysis
client.tasks.analyze(task_id=task.id)

# Get progress
progress = client.tasks.get_progress(task_id=task.id)
```

---

## Appendix D: Postman Collection

Import the Postman collection for API testing:

```
https://api.shopvideoscout.com/postman/collection.json
```
