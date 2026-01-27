-- Story 4.2: 声音克隆功能 - Add FK constraint for tasks.voice_sample_id
-- Description: Links tasks.voice_sample_id (added in Story 4.1) to voice_samples table
-- ON DELETE SET NULL: deleting a voice sample clears the reference but doesn't break tasks

ALTER TABLE tasks ADD CONSTRAINT fk_tasks_voice_sample
    FOREIGN KEY (voice_sample_id) REFERENCES voice_samples(id) ON DELETE SET NULL;
