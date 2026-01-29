package com.shopvideoscout.media.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Read-only mapper for scripts table (Story 4.3).
 * Used to retrieve script content JSON containing paragraphs with shot_id mappings.
 */
@Mapper
public interface ScriptReadMapper {

    /**
     * Get script content (JSON) by task ID.
     * The content contains paragraphs array with id, section, shot_id, text, estimated_duration.
     *
     * @param taskId task ID
     * @return script content JSON or null
     */
    @Select("SELECT content FROM scripts WHERE task_id = #{taskId} ORDER BY version DESC LIMIT 1")
    String findContentByTaskId(@Param("taskId") Long taskId);

    /**
     * Check if script exists for task.
     *
     * @param taskId task ID
     * @return true if script exists
     */
    @Select("SELECT COUNT(*) > 0 FROM scripts WHERE task_id = #{taskId}")
    boolean existsByTaskId(@Param("taskId") Long taskId);
}
