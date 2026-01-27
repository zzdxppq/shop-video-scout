package com.shopvideoscout.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for generating presigned OSS upload URL for voice sample.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoiceUploadUrlRequest {

    @NotBlank(message = "文件名不能为空")
    private String filename;
}
