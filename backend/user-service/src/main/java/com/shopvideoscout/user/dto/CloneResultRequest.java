package com.shopvideoscout.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal callback request from media-service after voice clone processing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloneResultRequest {

    private String cloneVoiceId;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "uploading|processing|completed|failed", message = "无效的状态值")
    private String status;

    private String errorMessage;
}
