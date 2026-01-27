-- Story 4.2: 声音克隆功能 - Create voice_samples table
-- Description: Stores user voice samples for voice cloning via Seed-ICL

CREATE TABLE IF NOT EXISTS voice_samples (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(100) COMMENT '声音样本名称',
    oss_key VARCHAR(500) NOT NULL COMMENT 'OSS存储路径',
    duration_seconds INT NOT NULL COMMENT '音频时长（秒）',
    clone_voice_id VARCHAR(100) COMMENT '豆包Seed-ICL克隆音色ID',
    status ENUM('uploading', 'processing', 'completed', 'failed') DEFAULT 'uploading' COMMENT '状态',
    error_message VARCHAR(500) COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_voice_samples_user_id (user_id),
    INDEX idx_voice_samples_status (status),
    CONSTRAINT fk_voice_sample_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='声音克隆样本';
