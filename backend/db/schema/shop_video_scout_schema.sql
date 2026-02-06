-- ============================================================================
-- Shop Video Scout - Complete Database Schema
-- Generated: 2026-02-06
-- Database: MySQL 8.0+
-- Charset: utf8mb4
-- ============================================================================

-- Create database
CREATE DATABASE IF NOT EXISTS `shop_video_scout`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `shop_video_scout`;

-- ============================================================================
-- Table: users
-- Purpose: Store user account information
-- ============================================================================
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号（唯一登录标识）',
    `nickname` VARCHAR(100) DEFAULT NULL COMMENT '用户昵称',
    `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `membership_type` ENUM('free', 'basic', 'pro') DEFAULT 'free' COMMENT '会员类型',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_membership_type` (`membership_type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================================
-- Table: voice_samples
-- Purpose: Store user voice samples for voice cloning (Seed-ICL)
-- ============================================================================
CREATE TABLE IF NOT EXISTS `voice_samples` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '语音样本ID',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `name` VARCHAR(100) DEFAULT NULL COMMENT '语音样本名称',
    `oss_key` VARCHAR(500) NOT NULL COMMENT 'OSS存储路径',
    `duration_seconds` INT NOT NULL COMMENT '音频时长（秒）',
    `clone_voice_id` VARCHAR(100) DEFAULT NULL COMMENT 'Seed-ICL克隆语音ID',
    `status` ENUM('uploading', 'processing', 'completed', 'failed') DEFAULT 'uploading' COMMENT '处理状态',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    KEY `idx_voice_samples_user_id` (`user_id`),
    KEY `idx_voice_samples_status` (`status`),
    CONSTRAINT `fk_voice_sample_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户语音样本表';

-- ============================================================================
-- Table: tasks
-- Purpose: Store video production task information
-- ============================================================================
CREATE TABLE IF NOT EXISTS `tasks` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `shop_name` VARCHAR(200) NOT NULL COMMENT '店铺名称',
    `shop_type` ENUM('food', 'beauty', 'entertainment', 'other') NOT NULL COMMENT '店铺类型',
    `promotion_text` TEXT DEFAULT NULL COMMENT '推广文案',
    `video_style` ENUM('recommend', 'review', 'vlog') NOT NULL COMMENT '视频风格',
    `status` ENUM('created', 'uploading', 'analyzing', 'script_ready', 'script_edited', 'voice_set', 'composing', 'completed', 'failed') DEFAULT 'created' COMMENT '任务状态',
    `voice_type` VARCHAR(50) DEFAULT 'xiaomei' COMMENT '标准TTS语音类型',
    `voice_sample_id` BIGINT DEFAULT NULL COMMENT '自定义克隆语音样本ID',
    `subtitle_enabled` BOOLEAN DEFAULT TRUE COMMENT '是否启用字幕',
    `subtitle_style` VARCHAR(50) DEFAULT 'simple_white' COMMENT '字幕样式模板',
    `output_oss_key` VARCHAR(500) DEFAULT NULL COMMENT '输出视频OSS路径',
    `output_duration_seconds` INT DEFAULT NULL COMMENT '输出视频时长（秒）',
    `output_file_size` BIGINT DEFAULT NULL COMMENT '输出文件大小（字节）',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `script_regenerate_count` INT DEFAULT 0 COMMENT '脚本重新生成次数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    KEY `idx_tasks_user_id` (`user_id`),
    KEY `idx_tasks_status` (`status`),
    KEY `idx_tasks_created_at` (`created_at`),
    KEY `idx_tasks_voice_type` (`voice_type`),
    KEY `idx_tasks_output_oss_key` (`output_oss_key`),
    CONSTRAINT `fk_task_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_tasks_voice_sample` FOREIGN KEY (`voice_sample_id`) REFERENCES `voice_samples` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频制作任务表';

-- ============================================================================
-- Table: videos
-- Purpose: Store uploaded video information and analysis results
-- ============================================================================
CREATE TABLE IF NOT EXISTS `videos` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '视频ID',
    `task_id` BIGINT NOT NULL COMMENT '所属任务ID',
    `original_filename` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `oss_key` VARCHAR(500) NOT NULL COMMENT 'OSS存储路径',
    `thumbnail_oss_key` VARCHAR(500) DEFAULT NULL COMMENT '缩略图OSS路径',
    `duration_seconds` INT DEFAULT NULL COMMENT '视频时长（秒）',
    `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `width` INT DEFAULT NULL COMMENT '视频宽度（像素）',
    `height` INT DEFAULT NULL COMMENT '视频高度（像素）',
    `status` ENUM('uploading', 'uploaded', 'analyzing', 'analyzed', 'failed') DEFAULT 'uploading' COMMENT '处理状态',
    `category` ENUM('food', 'person', 'environment', 'other') DEFAULT NULL COMMENT 'AI检测主类别',
    `tags` JSON DEFAULT NULL COMMENT 'AI检测标签数组（最多5个）',
    `description` VARCHAR(500) DEFAULT NULL COMMENT 'AI生成描述',
    `quality_score` INT DEFAULT NULL COMMENT '质量评分（0-100）',
    `is_recommended` BOOLEAN DEFAULT FALSE COMMENT '是否推荐镜头',
    `sort_order` INT DEFAULT NULL COMMENT '排序顺序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    KEY `idx_videos_task_id` (`task_id`),
    KEY `idx_videos_status` (`status`),
    KEY `idx_videos_category` (`category`),
    CONSTRAINT `fk_video_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上传视频表';

-- ============================================================================
-- Table: video_frames
-- Purpose: Store extracted video frame analysis results
-- ============================================================================
CREATE TABLE IF NOT EXISTS `video_frames` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '帧ID',
    `video_id` BIGINT NOT NULL COMMENT '所属视频ID',
    `frame_number` INT NOT NULL COMMENT '帧序号',
    `timestamp_ms` INT NOT NULL COMMENT '帧时间戳（毫秒）',
    `frame_url` VARCHAR(500) NOT NULL COMMENT '帧图片OSS URL',
    `category` ENUM('food', 'person', 'environment', 'other') DEFAULT NULL COMMENT 'AI检测类别',
    `tags` JSON DEFAULT NULL COMMENT 'AI检测标签（最多5个）',
    `quality_score` INT DEFAULT 0 COMMENT '质量评分（0-100）',
    `is_recommended` BOOLEAN DEFAULT FALSE COMMENT '是否推荐镜头',
    `description` VARCHAR(500) DEFAULT NULL COMMENT 'AI生成描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分析时间',

    PRIMARY KEY (`id`),
    KEY `idx_frames_video_id` (`video_id`),
    KEY `idx_frames_category` (`category`),
    KEY `idx_frames_is_recommended` (`is_recommended`),
    KEY `idx_frames_quality_score` (`quality_score`),
    CONSTRAINT `fk_frame_video` FOREIGN KEY (`video_id`) REFERENCES `videos` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频帧分析表';

-- ============================================================================
-- Table: scripts
-- Purpose: Store AI-generated and user-edited scripts
-- ============================================================================
CREATE TABLE IF NOT EXISTS `scripts` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '脚本ID',
    `task_id` BIGINT NOT NULL COMMENT '所属任务ID',
    `content` JSON NOT NULL COMMENT '脚本内容（paragraphs数组）',
    `version` INT DEFAULT 1 COMMENT '脚本版本号',
    `is_user_edited` BOOLEAN DEFAULT FALSE COMMENT '是否用户编辑过',
    `total_duration_seconds` INT DEFAULT NULL COMMENT '总时长（秒）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scripts_task_id` (`task_id`),
    KEY `idx_scripts_version` (`version`),
    CONSTRAINT `fk_script_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI脚本表';

-- ============================================================================
-- Table: publish_assists
-- Purpose: Store AI-generated topics and titles for publishing
-- ============================================================================
CREATE TABLE IF NOT EXISTS `publish_assists` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '发布辅助ID',
    `task_id` BIGINT NOT NULL COMMENT '所属任务ID',
    `topics` JSON DEFAULT NULL COMMENT '推荐话题JSON数组',
    `titles` JSON DEFAULT NULL COMMENT '推荐标题JSON数组',
    `regenerate_count` INT DEFAULT 0 COMMENT '重新生成次数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_publish_assists_task_id` (`task_id`),
    CONSTRAINT `fk_publish_assists_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发布辅助表';

-- ============================================================================
-- Sample Data (Optional - for development/testing)
-- ============================================================================

-- Insert test user
-- INSERT INTO `users` (`phone`, `nickname`, `membership_type`) VALUES
--     ('13800138000', '测试用户', 'pro');

-- ============================================================================
-- Views (Optional - for common queries)
-- ============================================================================

-- View: Task summary with user info
CREATE OR REPLACE VIEW `v_task_summary` AS
SELECT
    t.id AS task_id,
    t.shop_name,
    t.shop_type,
    t.status,
    t.video_style,
    t.output_oss_key,
    t.created_at,
    t.updated_at,
    u.id AS user_id,
    u.phone,
    u.nickname,
    (SELECT COUNT(*) FROM videos v WHERE v.task_id = t.id) AS video_count,
    (SELECT COUNT(*) FROM scripts s WHERE s.task_id = t.id) AS has_script
FROM tasks t
JOIN users u ON t.user_id = u.id;

-- View: Video analysis summary
CREATE OR REPLACE VIEW `v_video_analysis` AS
SELECT
    v.id AS video_id,
    v.task_id,
    v.original_filename,
    v.status,
    v.category,
    v.quality_score,
    v.is_recommended,
    v.duration_seconds,
    (SELECT COUNT(*) FROM video_frames vf WHERE vf.video_id = v.id) AS frame_count,
    (SELECT COUNT(*) FROM video_frames vf WHERE vf.video_id = v.id AND vf.is_recommended = TRUE) AS recommended_frame_count
FROM videos v;

-- ============================================================================
-- Stored Procedures (Optional - for common operations)
-- ============================================================================

-- Procedure: Get task history with pagination
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS `sp_get_task_history`(
    IN p_user_id BIGINT,
    IN p_page INT,
    IN p_size INT
)
BEGIN
    DECLARE v_offset INT;
    SET v_offset = (p_page - 1) * p_size;

    SELECT
        t.id,
        t.shop_name,
        t.shop_type,
        t.status,
        (SELECT v.thumbnail_oss_key
         FROM videos v
         WHERE v.task_id = t.id
         ORDER BY v.created_at ASC
         LIMIT 1) AS thumbnail_url,
        t.created_at
    FROM tasks t
    WHERE t.user_id = p_user_id
    ORDER BY t.created_at DESC
    LIMIT p_size OFFSET v_offset;
END //
DELIMITER ;

-- Procedure: Count user tasks
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS `sp_count_user_tasks`(
    IN p_user_id BIGINT,
    OUT p_total BIGINT
)
BEGIN
    SELECT COUNT(*) INTO p_total FROM tasks WHERE user_id = p_user_id;
END //
DELIMITER ;

-- ============================================================================
-- Triggers (Optional - for audit/consistency)
-- ============================================================================

-- Trigger: Update task status when all videos are analyzed
DELIMITER //
CREATE TRIGGER IF NOT EXISTS `trg_video_analyzed`
AFTER UPDATE ON `videos`
FOR EACH ROW
BEGIN
    DECLARE v_total_videos INT;
    DECLARE v_analyzed_videos INT;

    IF NEW.status = 'analyzed' AND OLD.status != 'analyzed' THEN
        SELECT COUNT(*), SUM(CASE WHEN status = 'analyzed' THEN 1 ELSE 0 END)
        INTO v_total_videos, v_analyzed_videos
        FROM videos
        WHERE task_id = NEW.task_id;

        IF v_total_videos = v_analyzed_videos THEN
            UPDATE tasks SET status = 'script_ready' WHERE id = NEW.task_id AND status = 'analyzing';
        END IF;
    END IF;
END //
DELIMITER ;

-- ============================================================================
-- End of Schema
-- ============================================================================
