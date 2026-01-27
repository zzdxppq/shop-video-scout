package com.shopvideoscout.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response containing presigned OSS upload URL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceUploadUrlResponse {

    private String uploadUrl;

    private String ossKey;

    private int expiresIn;
}
