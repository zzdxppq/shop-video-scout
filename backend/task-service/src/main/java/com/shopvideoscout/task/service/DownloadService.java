package com.shopvideoscout.task.service;

import com.shopvideoscout.task.dto.DownloadUrlResponse;

/**
 * Service for generating video download URLs (Story 5.2).
 */
public interface DownloadService {

    /**
     * Generate signed download URL for finished video.
     *
     * @param taskId task ID
     * @param userId user ID (for ownership validation)
     * @return download URL response with signed URL and metadata
     */
    DownloadUrlResponse generateVideoDownloadUrl(Long taskId, Long userId);
}
