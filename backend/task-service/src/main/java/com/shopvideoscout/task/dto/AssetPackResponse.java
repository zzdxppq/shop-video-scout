package com.shopvideoscout.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for assets pack download URL (Story 5.2).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetPackResponse {

    /**
     * Signed download URL for the ZIP file.
     */
    private String downloadUrl;

    /**
     * Suggested filename for download.
     */
    private String filename;

    /**
     * Number of video files in the pack.
     */
    private Integer fileCount;

    /**
     * Total size of all files in bytes.
     */
    private Long totalSize;

    /**
     * URL expiration timestamp (ISO 8601).
     */
    private String expiresAt;
}
