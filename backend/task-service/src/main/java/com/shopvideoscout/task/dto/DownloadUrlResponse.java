package com.shopvideoscout.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for video download URL (Story 5.2).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadUrlResponse {

    /**
     * Signed download URL with Content-Disposition header.
     */
    private String downloadUrl;

    /**
     * Suggested filename for download.
     */
    private String filename;

    /**
     * URL expiration timestamp (ISO 8601).
     */
    private String expiresAt;

    /**
     * File size in bytes.
     */
    private Long fileSize;
}
