package com.shopvideoscout.task.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request DTO for updating subtitle settings.
 * Story 4.5: 字幕设置页面
 */
@Data
public class SubtitleSettingsRequest {

    /**
     * Whether subtitles are enabled.
     * BR-1.1: 字幕开关默认为开启状态
     */
    @NotNull(message = "字幕开关状态不能为空")
    private Boolean subtitleEnabled;

    /**
     * Subtitle style template.
     * BR-2.1: 5种预设样式
     */
    @NotNull(message = "字幕样式不能为空")
    @Pattern(
        regexp = "simple_white|vibrant_yellow|xiaohongshu|douyin_hot|neon",
        message = "请选择有效的字幕样式"
    )
    private String subtitleStyle;
}
