package com.shopvideoscout.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Script content structure stored as JSON in scripts.content column.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptContent {

    /**
     * Script paragraphs with shot mappings.
     */
    private List<ScriptParagraph> paragraphs;

    /**
     * Total estimated duration in seconds.
     */
    @JsonProperty("total_duration")
    private Integer totalDuration;

    /**
     * Validate script content meets business rules.
     * BR-1.1: 5-7 paragraphs
     * BR-1.2: Each paragraph has shot_id
     * BR-1.3: Total duration ~60s (50-70s acceptable)
     */
    public boolean isValid() {
        if (paragraphs == null || paragraphs.isEmpty()) {
            return false;
        }

        // BR-1.1: 5-7 paragraphs
        if (paragraphs.size() < 5 || paragraphs.size() > 7) {
            return false;
        }

        // BR-1.2: Each paragraph has shot_id
        for (ScriptParagraph p : paragraphs) {
            if (p.getShotId() == null) {
                return false;
            }
        }

        // BR-1.3: Duration ~60s (allow 50-70s range)
        if (totalDuration == null || totalDuration < 50 || totalDuration > 70) {
            return false;
        }

        return true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScriptParagraph {

        /**
         * Paragraph ID (para_1, para_2, etc.).
         */
        private String id;

        /**
         * Section name (开场, 环境展示, 重点内容, 优惠信息, 结尾互动).
         */
        private String section;

        /**
         * Referenced video/shot ID.
         */
        @JsonProperty("shot_id")
        private Long shotId;

        /**
         * Script text for this paragraph.
         */
        private String text;

        /**
         * Estimated duration in seconds.
         */
        @JsonProperty("estimated_duration")
        private Integer estimatedDuration;
    }
}
