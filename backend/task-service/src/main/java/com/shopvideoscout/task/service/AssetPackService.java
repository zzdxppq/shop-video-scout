package com.shopvideoscout.task.service;

import com.shopvideoscout.task.dto.AssetPackResponse;

/**
 * Service for generating assets pack download (Story 5.2).
 */
public interface AssetPackService {

    /**
     * Generate or retrieve cached assets pack download URL.
     * The pack contains all recommended shots as a ZIP file.
     *
     * @param taskId task ID
     * @param userId user ID (for ownership validation)
     * @return asset pack response with download URL and metadata
     */
    AssetPackResponse generateAssetPack(Long taskId, Long userId);
}
