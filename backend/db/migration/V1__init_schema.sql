-- Shop Video Scout Database Initialization
-- Version: 1.0.0
-- Created: 2026-01-22
-- Description: Initial database schema for core tables

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS shop_video_scout
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE shop_video_scout;

-- =====================================================
-- Users Table
-- Stores user account information
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    phone VARCHAR(20) NOT NULL UNIQUE COMMENT 'Phone number (unique identifier)',
    nickname VARCHAR(100) COMMENT 'User display name',
    avatar_url VARCHAR(500) COMMENT 'Avatar image URL',
    membership_type ENUM('free', 'basic', 'pro') DEFAULT 'free' COMMENT 'Membership tier',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Account creation time',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',

    INDEX idx_phone (phone),
    INDEX idx_membership_type (membership_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User accounts';

-- =====================================================
-- Tasks Table
-- Stores video production task information
-- =====================================================
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    user_id BIGINT NOT NULL COMMENT 'Owner user ID',
    shop_name VARCHAR(200) NOT NULL COMMENT 'Shop/store name',
    shop_type ENUM('food', 'beauty', 'entertainment', 'other') NOT NULL COMMENT 'Shop category',
    promotion_text TEXT COMMENT 'Promotional text/description',
    video_style ENUM('recommend', 'review', 'vlog') NOT NULL COMMENT 'Video style preference',
    status ENUM('created', 'uploading', 'analyzing', 'script_ready', 'script_edited', 'voice_set', 'composing', 'completed', 'failed') DEFAULT 'created' COMMENT 'Task status',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Task creation time',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',

    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Video production tasks';

-- =====================================================
-- Videos Table
-- Stores uploaded video information
-- =====================================================
CREATE TABLE IF NOT EXISTS videos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    task_id BIGINT NOT NULL COMMENT 'Parent task ID',
    file_key VARCHAR(500) NOT NULL COMMENT 'OSS file key/path',
    file_name VARCHAR(200) NOT NULL COMMENT 'Original file name',
    file_size BIGINT NOT NULL COMMENT 'File size in bytes',
    duration INT COMMENT 'Video duration in seconds',
    width INT COMMENT 'Video width in pixels',
    height INT COMMENT 'Video height in pixels',
    thumbnail_url VARCHAR(500) COMMENT 'Thumbnail image URL',
    status ENUM('uploading', 'processing', 'analyzed', 'failed') DEFAULT 'uploading' COMMENT 'Video status',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Upload time',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',

    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    CONSTRAINT fk_video_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Uploaded videos';

-- =====================================================
-- Video Frames Table
-- Stores extracted video frame analysis results
-- =====================================================
CREATE TABLE IF NOT EXISTS video_frames (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    video_id BIGINT NOT NULL COMMENT 'Parent video ID',
    frame_number INT NOT NULL COMMENT 'Frame sequence number',
    timestamp_ms INT NOT NULL COMMENT 'Frame timestamp in milliseconds',
    frame_url VARCHAR(500) NOT NULL COMMENT 'Frame image URL',
    category ENUM('food', 'person', 'environment', 'other') COMMENT 'AI-detected category',
    tags JSON COMMENT 'AI-detected tags (max 5)',
    quality_score INT DEFAULT 0 COMMENT 'Quality score (0-100)',
    is_recommended BOOLEAN DEFAULT FALSE COMMENT 'Marked as recommended shot',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Analysis time',

    INDEX idx_video_id (video_id),
    INDEX idx_category (category),
    INDEX idx_is_recommended (is_recommended),
    INDEX idx_quality_score (quality_score),
    CONSTRAINT fk_frame_video FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Video frame analysis results';

-- =====================================================
-- Scripts Table
-- Stores AI-generated and user-edited scripts
-- =====================================================
CREATE TABLE IF NOT EXISTS scripts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary key',
    task_id BIGINT NOT NULL COMMENT 'Parent task ID',
    content TEXT NOT NULL COMMENT 'Script content',
    is_ai_generated BOOLEAN DEFAULT TRUE COMMENT 'AI generated or user edited',
    version INT DEFAULT 1 COMMENT 'Script version number',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',

    INDEX idx_task_id (task_id),
    INDEX idx_version (version),
    CONSTRAINT fk_script_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Video scripts';
