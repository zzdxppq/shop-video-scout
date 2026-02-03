package com.shopvideoscout.task.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for subtitle settings.
 * Story 4.5: 字幕设置页面
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleSettingsResponse {

    /**
     * Whether subtitles are enabled.
     */
    private Boolean subtitleEnabled;

    /**
     * Subtitle style template.
     */
    private String subtitleStyle;

    /**
     * Create response from entity fields.
     */
    public static SubtitleSettingsResponse of(Boolean enabled, String style) {
        return new SubtitleSettingsResponse(enabled, style);
    }
}
