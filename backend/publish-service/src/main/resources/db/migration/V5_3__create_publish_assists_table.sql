-- V5_3__create_publish_assists_table.sql
-- Story 5.3: 发布辅助服务
-- Creates publish_assists table for storing AI-generated topics and titles

CREATE TABLE IF NOT EXISTS publish_assists (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL UNIQUE COMMENT 'Associated task ID',
    topics JSON COMMENT 'Recommended topics (JSON array)',
    titles JSON COMMENT 'Recommended titles (JSON array)',
    regenerate_count INT DEFAULT 0 COMMENT 'Number of regeneration attempts',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_publish_assists_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Publish assist data for tasks';

CREATE INDEX idx_publish_assists_task_id ON publish_assists(task_id);
