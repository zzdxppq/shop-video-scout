-- Story 4.3: Video Composition - Add subtitle and output fields to tasks table
-- These fields support subtitle configuration and output video metadata

ALTER TABLE tasks ADD COLUMN subtitle_enabled BOOLEAN DEFAULT TRUE AFTER voice_sample_id;
ALTER TABLE tasks ADD COLUMN subtitle_style VARCHAR(50) DEFAULT 'simple_white' AFTER subtitle_enabled;
ALTER TABLE tasks ADD COLUMN output_oss_key VARCHAR(500) NULL AFTER subtitle_style;
ALTER TABLE tasks ADD COLUMN output_duration_seconds INT NULL AFTER output_oss_key;
ALTER TABLE tasks ADD COLUMN output_file_size BIGINT NULL AFTER output_duration_seconds;
ALTER TABLE tasks ADD COLUMN error_message VARCHAR(500) NULL AFTER output_file_size;

-- Add index for output queries
CREATE INDEX idx_tasks_output_oss_key ON tasks(output_oss_key);
