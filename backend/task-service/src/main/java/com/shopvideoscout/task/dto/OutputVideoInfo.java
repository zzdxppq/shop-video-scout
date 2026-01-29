package com.shopvideoscout.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Video output details for OutputResponse (Story 4.3).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutputVideoInfo {

    /**
     * CDN URL for the output video.
     */
    private String url;

    /**
     * Video duration in seconds.
     */
    private Integer durationSeconds;

    /**
     * File size in bytes.
     */
    private Long fileSize;

    /**
     * Video width (always 1080 for portrait).
     */
    private Integer width;

    /**
     * Video height (always 1920 for portrait).
     */
    private Integer height;

    /**
     * Video format (always mp4).
     */
    private String format;
}
