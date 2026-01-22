package com.shopvideoscout.task.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.shopvideoscout.mybatis.entity.BaseEntity;
import com.shopvideoscout.task.constant.VideoConstants;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Video entity mapping to 'videos' table.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "videos", autoResultMap = true)
public class Video extends BaseEntity {

    /**
     * Associated task ID.
     */
    private Long taskId;

    /**
     * Original filename from upload.
     */
    private String originalFilename;

    /**
     * OSS storage key.
     */
    private String ossKey;

    /**
     * Thumbnail OSS key.
     */
    private String thumbnailOssKey;

    /**
     * Video duration in seconds.
     */
    private Integer durationSeconds;

    /**
     * File size in bytes.
     */
    private Long fileSize;

    /**
     * Video width in pixels.
     */
    private Integer width;

    /**
     * Video height in pixels.
     */
    private Integer height;

    /**
     * Processing status: uploading, uploaded, analyzing, analyzed, failed.
     */
    private String status;

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
     * AI-generated description.
     */
    private String description;

    /**
     * AI quality score (0-100).
     */
    private Integer qualityScore;

    /**
     * Whether marked as recommended.
     */
    private Boolean isRecommended;

    /**
     * Display order.
     */
    private Integer sortOrder;

    /**
     * Create a new video with uploading status.
     */
    public static Video createNew(Long taskId, String ossKey, String originalFilename, Long fileSize) {
        Video video = new Video();
        video.setTaskId(taskId);
        video.setOssKey(ossKey);
        video.setOriginalFilename(originalFilename);
        video.setFileSize(fileSize);
        video.setStatus(VideoConstants.VideoStatus.UPLOADING);
        video.setIsRecommended(false);
        return video;
    }
}
