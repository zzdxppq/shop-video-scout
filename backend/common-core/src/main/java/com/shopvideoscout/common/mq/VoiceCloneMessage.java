package com.shopvideoscout.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * MQ message for triggering voice cloning in media-service.
 * Published by user-service after voice sample upload, consumed by media-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceCloneMessage implements Serializable {

    private Long voiceSampleId;

    private Long userId;

    private String ossKey;

    private Integer durationSeconds;
}
