package com.shopvideoscout.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopvideoscout.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Voice sample entity mapping to 'voice_samples' table.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("voice_samples")
public class VoiceSample extends BaseEntity {

    private Long userId;

    private String name;

    private String ossKey;

    private Integer durationSeconds;

    /**
     * Seed-ICL clone voice ID (populated after successful cloning).
     */
    private String cloneVoiceId;

    /**
     * Status: uploading, processing, completed, failed.
     */
    private String status;

    private String errorMessage;

    public static final String STATUS_UPLOADING = "uploading";
    public static final String STATUS_PROCESSING = "processing";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_FAILED = "failed";

    public static final int MIN_DURATION_SECONDS = 5;
    public static final int MAX_DURATION_SECONDS = 120;
    public static final int MAX_SAMPLES_PER_USER = 3;
}
