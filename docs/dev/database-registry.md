# Database Cumulative Registry

> Auto-generated on first story creation
> Updated by Dev Agent after each story completion

## Registry Metadata

**Last Updated**: 2026-01-28
**Total Stories Tracked**: 3
**Repository**: shop-video-scout
**Mode**: monolith

## Database Tables Registry

### users
| Field | Type | Constraints | Added in Story |
|-------|------|-------------|----------------|
| id | BIGINT | PRIMARY KEY AUTO_INCREMENT | 1.1 |
| phone | VARCHAR(20) | NOT NULL UNIQUE | 1.1 |
| nickname | VARCHAR(100) | - | 1.1 |
| avatar_url | VARCHAR(500) | - | 1.1 |
| membership_type | ENUM('free','basic','pro') | DEFAULT 'free' | 1.1 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 1.1 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 1.1 |

### tasks
| Field | Type | Constraints | Added in Story |
|-------|------|-------------|----------------|
| id | BIGINT | PRIMARY KEY AUTO_INCREMENT | 1.1 |
| user_id | BIGINT | NOT NULL, FK(users.id) | 1.1 |
| shop_name | VARCHAR(200) | NOT NULL | 1.1 |
| shop_type | ENUM('food','beauty','entertainment','other') | NOT NULL | 1.1 |
| promotion_text | TEXT | - | 1.1 |
| video_style | ENUM('recommend','review','vlog') | NOT NULL | 1.1 |
| status | ENUM('created','uploading','analyzing','script_ready','script_edited','voice_set','composing','completed','failed') | DEFAULT 'created' | 1.1 |
| voice_type | VARCHAR(50) | DEFAULT 'xiaomei' | 4.1 |
| voice_sample_id | BIGINT | NULL | 4.1 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 1.1 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 1.1 |

### videos
| Field | Type | Constraints | Added in Story |
|-------|------|-------------|----------------|
| id | BIGINT | PRIMARY KEY AUTO_INCREMENT | 1.1 |
| task_id | BIGINT | NOT NULL, FK(tasks.id) | 1.1 |
| file_key | VARCHAR(500) | NOT NULL | 1.1 |
| file_name | VARCHAR(200) | NOT NULL | 1.1 |
| file_size | BIGINT | NOT NULL | 1.1 |
| duration | INT | - | 1.1 |
| width | INT | - | 1.1 |
| height | INT | - | 1.1 |
| thumbnail_url | VARCHAR(500) | - | 1.1 |
| status | ENUM('uploading','processing','analyzed','failed') | DEFAULT 'uploading' | 1.1 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 1.1 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 1.1 |

### video_frames
| Field | Type | Constraints | Added in Story |
|-------|------|-------------|----------------|
| id | BIGINT | PRIMARY KEY AUTO_INCREMENT | 1.1 |
| video_id | BIGINT | NOT NULL, FK(videos.id) | 1.1 |
| frame_number | INT | NOT NULL | 1.1 |
| timestamp_ms | INT | NOT NULL | 1.1 |
| frame_url | VARCHAR(500) | NOT NULL | 1.1 |
| category | ENUM('food','person','environment','other') | - | 1.1 |
| tags | JSON | - | 1.1 |
| quality_score | INT | DEFAULT 0 | 1.1 |
| is_recommended | BOOLEAN | DEFAULT FALSE | 1.1 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 1.1 |

### scripts
| Field | Type | Constraints | Added in Story |
|-------|------|-------------|----------------|
| id | BIGINT | PRIMARY KEY AUTO_INCREMENT | 1.1 |
| task_id | BIGINT | NOT NULL, FK(tasks.id) | 1.1 |
| content | TEXT | NOT NULL | 1.1 |
| is_ai_generated | BOOLEAN | DEFAULT TRUE | 1.1 |
| version | INT | DEFAULT 1 | 1.1 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 1.1 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 1.1 |

### voice_samples
| Field | Type | Constraints | Added in Story |
|-------|------|-------------|----------------|
| id | BIGINT | PRIMARY KEY AUTO_INCREMENT | 4.2 |
| user_id | BIGINT | NOT NULL, FK(users.id) ON DELETE CASCADE | 4.2 |
| name | VARCHAR(100) | - | 4.2 |
| oss_key | VARCHAR(500) | NOT NULL | 4.2 |
| duration_seconds | INT | NOT NULL | 4.2 |
| clone_voice_id | VARCHAR(100) | NULL | 4.2 |
| status | ENUM('uploading','processing','completed','failed') | DEFAULT 'uploading' | 4.2 |
| error_message | VARCHAR(500) | NULL | 4.2 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 4.2 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 4.2 |

## Naming Conventions & Patterns

| Pattern | Convention |
|---------|------------|
| Table naming | snake_case, singular |
| Field naming | snake_case |
| Primary key | id (BIGINT AUTO_INCREMENT) |
| Foreign key | {table}_id |
| Timestamps | created_at, updated_at |

## Schema Evolution Timeline

| Story | Changes | Tables Affected |
|-------|---------|-----------------|
| 1.1 | Initial schema creation | users, tasks, videos, video_frames, scripts |
| 4.1 | ALTER tasks: ADD voice_type, voice_sample_id; ADD INDEX idx_tasks_voice_type | tasks |
| 4.2 | CREATE voice_samples; ALTER tasks: ADD FK voice_sample_id → voice_samples.id ON DELETE SET NULL | voice_samples, tasks |

**Total Tables**: 6
**Total Fields**: 59
