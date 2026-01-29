package com.shopvideoscout.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Information about a video shot used in the output (Story 4.3).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShotUsedInfo {

    /**
     * Video/shot ID.
     */
    private Long id;

    /**
     * Thumbnail URL.
     */
    private String thumbnailUrl;

    /**
     * Video category (food, person, environment, other).
     */
    private String category;

    /**
     * Duration used in seconds.
     */
    private Integer durationSeconds;
}
