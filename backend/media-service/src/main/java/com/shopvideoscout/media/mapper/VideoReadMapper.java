package com.shopvideoscout.media.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Read-only mapper for videos table (Story 4.3).
 * Used by VideoSegmentCuttingService to retrieve video metadata for cutting.
 */
@Mapper
public interface VideoReadMapper {

    /**
     * Get video by ID.
     *
     * @param id video ID
     * @return video info or null
     */
    @Select("SELECT id, task_id, oss_key, thumbnail_oss_key, duration_seconds, category, is_recommended " +
            "FROM videos WHERE id = #{id} AND deleted_at IS NULL")
    VideoInfo findById(@Param("id") Long id);

    /**
     * Get recommended video for a task.
     *
     * @param taskId task ID
     * @return recommended video info or null
     */
    @Select("SELECT id, task_id, oss_key, thumbnail_oss_key, duration_seconds, category, is_recommended " +
            "FROM videos WHERE task_id = #{taskId} AND is_recommended = true AND deleted_at IS NULL LIMIT 1")
    VideoInfo findRecommendedByTaskId(@Param("taskId") Long taskId);

    /**
     * Get all videos for a task.
     *
     * @param taskId task ID
     * @return list of video info
     */
    @Select("SELECT id, task_id, oss_key, thumbnail_oss_key, duration_seconds, category, is_recommended " +
            "FROM videos WHERE task_id = #{taskId} AND deleted_at IS NULL ORDER BY sort_order")
    List<VideoInfo> findByTaskId(@Param("taskId") Long taskId);

    /**
     * Video info projection.
     */
    interface VideoInfo {
        Long getId();
        Long getTaskId();
        String getOssKey();
        String getThumbnailOssKey();
        Integer getDurationSeconds();
        String getCategory();
        Boolean getIsRecommended();
    }
}
