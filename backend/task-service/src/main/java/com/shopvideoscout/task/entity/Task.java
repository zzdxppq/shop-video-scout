package com.shopvideoscout.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopvideoscout.mybatis.entity.BaseEntity;
import com.shopvideoscout.task.constant.TaskConstants;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Task entity mapping to 'tasks' table.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tasks")
public class Task extends BaseEntity {

    /**
     * Owner user ID.
     */
    private Long userId;

    /**
     * Shop name (required, max 200 chars).
     */
    private String shopName;

    /**
     * Shop type: food, beauty, entertainment, other.
     */
    private String shopType;

    /**
     * Promotion/discount description (optional, max 500 chars).
     */
    private String promotionText;

    /**
     * Video style: recommend, review, vlog.
     */
    private String videoStyle;

    /**
     * Task status.
     */
    private String status;

    /**
     * Selected voice type for TTS.
     */
    private String voiceType;

    /**
     * User's custom voice sample ID (optional).
     */
    private Long voiceSampleId;

    /**
     * Whether subtitles are enabled.
     */
    private Boolean subtitleEnabled;

    /**
     * Subtitle style template.
     */
    private String subtitleStyle;

    /**
     * Output video OSS key.
     */
    private String outputOssKey;

    /**
     * Output video duration in seconds.
     */
    private Integer outputDurationSeconds;

    /**
     * Output file size in bytes.
     */
    private Long outputFileSize;

    /**
     * Error message if task failed.
     */
    private String errorMessage;

    /**
     * Number of script regeneration attempts.
     */
    private Integer scriptRegenerateCount;

    /**
     * Create a new task with default values.
     */
    public static Task createNew(Long userId, String shopName, String shopType,
                                  String promotionText, String videoStyle) {
        Task task = new Task();
        task.setUserId(userId);
        task.setShopName(shopName);
        task.setShopType(shopType);
        task.setPromotionText(promotionText);
        task.setVideoStyle(videoStyle);
        task.setStatus(TaskConstants.TaskStatus.UPLOADING);
        task.setVoiceType("xiaomei");
        task.setSubtitleEnabled(true);
        task.setSubtitleStyle("simple_white");
        task.setScriptRegenerateCount(0);
        return task;
    }
}
