package com.shopvideoscout.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopvideoscout.ai.entity.VideoFrame;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * MyBatis-Plus mapper for VideoFrame entity.
 */
@Mapper
public interface VideoFrameMapper extends BaseMapper<VideoFrame> {

    /**
     * Find all frames for a video.
     */
    @Select("SELECT * FROM video_frames WHERE video_id = #{videoId} ORDER BY frame_number")
    List<VideoFrame> findByVideoId(@Param("videoId") Long videoId);

    /**
     * Count frames for a video.
     */
    @Select("SELECT COUNT(*) FROM video_frames WHERE video_id = #{videoId}")
    int countByVideoId(@Param("videoId") Long videoId);

    /**
     * Count analyzed frames for a video (frames with category set).
     */
    @Select("SELECT COUNT(*) FROM video_frames WHERE video_id = #{videoId} AND category IS NOT NULL")
    int countAnalyzedByVideoId(@Param("videoId") Long videoId);

    /**
     * Find all frames for a task (through videos).
     */
    @Select("""
        SELECT vf.* FROM video_frames vf
        INNER JOIN videos v ON vf.video_id = v.id
        WHERE v.task_id = #{taskId}
        ORDER BY vf.video_id, vf.frame_number
        """)
    List<VideoFrame> findByTaskId(@Param("taskId") Long taskId);

    /**
     * Count total frames for a task.
     */
    @Select("""
        SELECT COUNT(*) FROM video_frames vf
        INNER JOIN videos v ON vf.video_id = v.id
        WHERE v.task_id = #{taskId}
        """)
    int countByTaskId(@Param("taskId") Long taskId);

    /**
     * Count analyzed frames for a task.
     */
    @Select("""
        SELECT COUNT(*) FROM video_frames vf
        INNER JOIN videos v ON vf.video_id = v.id
        WHERE v.task_id = #{taskId} AND vf.category IS NOT NULL
        """)
    int countAnalyzedByTaskId(@Param("taskId") Long taskId);

    /**
     * Update frame analysis results.
     */
    @Update("""
        UPDATE video_frames
        SET category = #{category},
            tags = #{tags,typeHandler=com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler},
            quality_score = #{qualityScore},
            description = #{description}
        WHERE id = #{id}
        """)
    int updateAnalysisResult(@Param("id") Long id,
                             @Param("category") String category,
                             @Param("tags") List<String> tags,
                             @Param("qualityScore") Integer qualityScore,
                             @Param("description") String description);

    /**
     * Mark frames as recommended.
     */
    @Update("""
        UPDATE video_frames SET is_recommended = #{isRecommended} WHERE id = #{id}
        """)
    int updateRecommended(@Param("id") Long id, @Param("isRecommended") Boolean isRecommended);

    /**
     * Reset all recommendations for a task.
     */
    @Update("""
        UPDATE video_frames vf
        INNER JOIN videos v ON vf.video_id = v.id
        SET vf.is_recommended = FALSE
        WHERE v.task_id = #{taskId}
        """)
    int resetRecommendationsByTaskId(@Param("taskId") Long taskId);
}
