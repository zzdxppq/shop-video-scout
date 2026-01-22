package com.shopvideoscout.task.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for presigned upload URL.
 */
@Data
@Builder
public class VideoUploadUrlResponse {

    /**
     * OSS presigned upload URL.
     */
    private String uploadUrl;

    /**
     * OSS object key for the file.
     */
    private String ossKey;

    /**
     * URL expiration time in seconds.
     */
    private Integer expiresIn;
}
