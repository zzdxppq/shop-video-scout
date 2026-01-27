package com.shopvideoscout.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for updating task voice type.
 */
@Data
public class VoiceTypeRequest {

    @NotBlank(message = "音色类型不能为空")
    @Size(max = 50, message = "音色标识不能超过50个字符")
    private String voiceType;

    private Long voiceSampleId;
}
