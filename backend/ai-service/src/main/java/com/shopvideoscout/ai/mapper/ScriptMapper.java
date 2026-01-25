package com.shopvideoscout.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopvideoscout.ai.entity.Script;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * MyBatis-Plus mapper for scripts table.
 */
@Mapper
public interface ScriptMapper extends BaseMapper<Script> {

    /**
     * Find script by task ID.
     */
    @Select("SELECT * FROM scripts WHERE task_id = #{taskId}")
    Script findByTaskId(@Param("taskId") Long taskId);

    /**
     * Check if script exists for task.
     */
    @Select("SELECT COUNT(*) > 0 FROM scripts WHERE task_id = #{taskId}")
    boolean existsByTaskId(@Param("taskId") Long taskId);
}
