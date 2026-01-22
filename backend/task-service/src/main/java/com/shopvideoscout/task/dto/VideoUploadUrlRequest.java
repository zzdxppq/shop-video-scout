package com.shopvideoscout.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Request DTO for getting presigned upload URL.
 */
@Data
public class VideoUploadUrlRequest {

    /**
     * Original filename with extension (required).
     */
    @NotBlank(message = "文件名不能为空")
    private String filename;

    /**
     * File size in bytes (required).
     */
    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于0")
    private Long fileSize;

    /**
     * Content type (optional, defaults based on extension).
     */
    private String contentType;
}
