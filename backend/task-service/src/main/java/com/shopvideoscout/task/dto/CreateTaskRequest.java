package com.shopvideoscout.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating a new task.
 */
@Data
public class CreateTaskRequest {

    /**
     * Shop name (required, 1-200 characters).
     */
    @NotBlank(message = "请输入店铺名称")
    @Size(max = 200, message = "店铺名称不能超过200字")
    private String shopName;

    /**
     * Shop type (required): food, beauty, entertainment, other.
     */
    @NotBlank(message = "请选择店铺类型")
    @Pattern(regexp = "^(food|beauty|entertainment|other)$", message = "请选择正确的店铺类型")
    private String shopType;

    /**
     * Promotion text (optional, max 500 characters).
     */
    @Size(max = 500, message = "优惠描述不能超过500字")
    private String promotionText;

    /**
     * Video style (required): recommend, review, vlog.
     */
    @NotBlank(message = "请选择视频风格")
    @Pattern(regexp = "^(recommend|review|vlog)$", message = "请选择正确的视频风格")
    private String videoStyle;
}
