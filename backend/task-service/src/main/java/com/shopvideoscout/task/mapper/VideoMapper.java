package com.shopvideoscout.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopvideoscout.task.entity.Video;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for Video entity.
 */
@Mapper
public interface VideoMapper extends BaseMapper<Video> {

    /**
     * Count videos for a task.
     */
    @Select("SELECT COUNT(*) FROM videos WHERE task_id = #{taskId}")
    int countByTaskId(@Param("taskId") Long taskId);

    /**
     * Find all videos for a task ordered by sort_order.
     */
    @Select("SELECT * FROM videos WHERE task_id = #{taskId} ORDER BY sort_order ASC, id ASC")
    List<Video> findByTaskId(@Param("taskId") Long taskId);

    /**
     * Find video by ID and task ID (for ownership validation).
     */
    @Select("SELECT * FROM videos WHERE id = #{videoId} AND task_id = #{taskId}")
    Video findByIdAndTaskId(@Param("videoId") Long videoId, @Param("taskId") Long taskId);
}
