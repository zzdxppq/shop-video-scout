package com.shopvideoscout.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for confirming video upload completion.
 */
@Data
public class ConfirmVideoUploadRequest {

    /**
     * OSS object key from upload URL response (required).
     */
    @NotBlank(message = "OSS key不能为空")
    private String ossKey;

    /**
     * Original filename (required).
     */
    @NotBlank(message = "原始文件名不能为空")
    private String originalFilename;
}
