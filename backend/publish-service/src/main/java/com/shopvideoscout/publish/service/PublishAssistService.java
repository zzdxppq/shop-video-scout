package com.shopvideoscout.publish.service;

import com.shopvideoscout.publish.dto.PublishAssistResponse;

/**
 * Service for publish assist orchestration.
 * Story 5.3: 发布辅助服务 - AC1, AC2, AC3
 */
public interface PublishAssistService {

    /**
     * Get publish assist (topics + titles) for a task.
     * Uses cache → DB → AI generation fallback strategy.
     *
     * @param taskId task ID
     * @param userId current user ID (for ownership validation)
     * @return publish assist response
     */
    PublishAssistResponse getPublishAssist(Long taskId, Long userId);

    /**
     * Regenerate publish assist with higher temperature.
     * Max 3 regenerations per task.
     *
     * @param taskId task ID
     * @param userId current user ID (for ownership validation)
     * @return regenerated publish assist response
     */
    PublishAssistResponse regenerate(Long taskId, Long userId);
}
