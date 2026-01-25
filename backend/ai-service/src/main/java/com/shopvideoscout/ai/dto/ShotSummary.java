package com.shopvideoscout.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Summary of an analyzed video shot/frame for script generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShotSummary {

    /**
     * Video/frame ID.
     */
    private Long shotId;

    /**
     * Content category (food, person, environment, other).
     */
    private String category;

    /**
     * Descriptive tags.
     */
    private List<String> tags;

    /**
     * Quality score (0-100).
     */
    private Integer qualityScore;

    /**
     * Whether this shot is recommended for use.
     */
    private boolean recommended;
}
