package com.shopvideoscout.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for creating a voice sample record after upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVoiceSampleRequest {

    @NotBlank(message = "样本名称不能为空")
    @Size(max = 100, message = "样本名称不能超过100个字符")
    private String name;

    @NotBlank(message = "OSS路径不能为空")
    @Size(max = 500, message = "OSS路径不能超过500个字符")
    private String ossKey;

    @NotNull(message = "音频时长不能为空")
    @Min(value = 5, message = "音频时长不能少于5秒")
    @Max(value = 120, message = "音频时长不能超过120秒")
    private Integer durationSeconds;
}
