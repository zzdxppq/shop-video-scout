package com.shopvideoscout.task.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Read-only mapper for scripts table.
 * Used to retrieve script content for compose operations.
 */
@Mapper
public interface ScriptMapper {

    /**
     * Get script content (JSON) by task ID.
     */
    @Select("SELECT content FROM scripts WHERE task_id = #{taskId} ORDER BY version DESC LIMIT 1")
    String findContentByTaskId(@Param("taskId") Long taskId);

    /**
     * Check if script exists for task.
     */
    @Select("SELECT COUNT(*) > 0 FROM scripts WHERE task_id = #{taskId}")
    boolean existsByTaskId(@Param("taskId") Long taskId);
}
