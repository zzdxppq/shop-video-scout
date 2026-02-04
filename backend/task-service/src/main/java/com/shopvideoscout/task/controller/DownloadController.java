package com.shopvideoscout.task.controller;

import com.shopvideoscout.common.result.R;
import com.shopvideoscout.task.dto.AssetPackResponse;
import com.shopvideoscout.task.dto.DownloadUrlResponse;
import com.shopvideoscout.task.service.AssetPackService;
import com.shopvideoscout.task.service.DownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for download endpoints (Story 5.2).
 *
 * Provides signed URLs for:
 * - Finished video download
 * - Assets pack (recommended shots ZIP) download
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadService downloadService;
    private final AssetPackService assetPackService;

    /**
     * Get signed download URL for finished video.
     *
     * GET /api/v1/tasks/{id}/output/download
     *
     * @param id task ID
     * @param userDetails authenticated user
     * @return download URL response with signed URL and metadata
     */
    @GetMapping("/{id}/output/download")
    public R<DownloadUrlResponse> getVideoDownloadUrl(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        log.debug("User {} requesting video download for task {}", userId, id);

        DownloadUrlResponse response = downloadService.generateVideoDownloadUrl(id, userId);
        return R.ok(response);
    }

    /**
     * Get signed download URL for assets pack (recommended shots ZIP).
     *
     * GET /api/v1/tasks/{id}/assets-pack
     *
     * @param id task ID
     * @param userDetails authenticated user
     * @return asset pack response with signed URL and metadata
     */
    @GetMapping("/{id}/assets-pack")
    public R<AssetPackResponse> getAssetsPackDownloadUrl(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        log.debug("User {} requesting assets pack for task {}", userId, id);

        AssetPackResponse response = assetPackService.generateAssetPack(id, userId);
        return R.ok(response);
    }
}
