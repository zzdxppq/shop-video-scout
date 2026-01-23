package com.shopvideoscout.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * VideoFrame entity mapping to 'video_frames' table.
 * Stores extracted keyframes and their AI analysis results.
 */
@Data
@TableName(value = "video_frames", autoResultMap = true)
public class VideoFrame {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Associated video ID.
     */
    private Long videoId;

    /**
     * Frame number in sequence.
     */
    private Integer frameNumber;

    /**
     * Timestamp in milliseconds from video start.
     */
    private Integer timestampMs;

    /**
     * OSS URL of the frame image.
     */
    private String frameUrl;

    /**
     * AI-detected category: food, person, environment, other.
     */
    private String category;

    /**
     * AI-generated tags (JSON array).
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /**
     * AI quality score (0-100).
     */
    private Integer qualityScore;

    /**
     * Whether this frame is recommended.
     */
    private Boolean isRecommended;

    /**
     * AI-generated description.
     */
    private String description;

    /**
     * Creation timestamp.
     */
    private LocalDateTime createdAt;

    /**
     * Create a new video frame.
     */
    public static VideoFrame create(Long videoId, int frameNumber, int timestampMs, String frameUrl) {
        VideoFrame frame = new VideoFrame();
        frame.setVideoId(videoId);
        frame.setFrameNumber(frameNumber);
        frame.setTimestampMs(timestampMs);
        frame.setFrameUrl(frameUrl);
        frame.setIsRecommended(false);
        frame.setCreatedAt(LocalDateTime.now());
        return frame;
    }
}
