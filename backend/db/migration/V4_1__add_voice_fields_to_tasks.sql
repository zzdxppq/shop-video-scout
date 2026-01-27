-- Story 4.1: TTS配音服务 - Add voice configuration fields to tasks table
-- Description: Add voice_type and voice_sample_id columns for TTS synthesis

ALTER TABLE tasks
    ADD COLUMN voice_type VARCHAR(50) DEFAULT 'xiaomei' COMMENT '标准音色标识，默认活泼女声',
    ADD COLUMN voice_sample_id BIGINT NULL COMMENT '克隆声音关联ID，可NULL';

CREATE INDEX idx_tasks_voice_type ON tasks(voice_type);
