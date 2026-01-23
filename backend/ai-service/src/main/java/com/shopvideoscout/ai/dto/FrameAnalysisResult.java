package com.shopvideoscout.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of AI frame analysis from Qwen-VL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrameAnalysisResult {

    /**
     * Frame ID being analyzed.
     */
    private Long frameId;

    /**
     * Frame URL that was analyzed.
     */
    private String frameUrl;

    /**
     * Detected category: food, person, environment, other.
     */
    private String category;

    /**
     * AI-generated description tags (max 5).
     */
    private List<String> tags;

    /**
     * Quality score (0-100) based on clarity, composition, lighting, stability.
     */
    private Integer qualityScore;

    /**
     * AI-generated description text.
     */
    private String description;

    /**
     * Whether analysis was successful.
     */
    private boolean success;

    /**
     * Error message if analysis failed.
     */
    private String errorMessage;

    /**
     * Create a failed result.
     */
    public static FrameAnalysisResult failed(Long frameId, String frameUrl, String errorMessage) {
        return FrameAnalysisResult.builder()
                .frameId(frameId)
                .frameUrl(frameUrl)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
