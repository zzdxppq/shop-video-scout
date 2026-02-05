package com.shopvideoscout.publish.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for publish assist API.
 * Story 5.3: 发布辅助服务
 */
@Data
@Builder
public class PublishAssistResponse {

    /**
     * Recommended topics (hashtags).
     */
    private List<String> topics;

    /**
     * Recommended video titles.
     */
    private List<String> titles;

    /**
     * Remaining regeneration attempts.
     */
    private Integer regenerateRemaining;
}
