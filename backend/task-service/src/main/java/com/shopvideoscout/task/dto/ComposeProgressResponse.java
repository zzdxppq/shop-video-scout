package com.shopvideoscout.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for compose progress query.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComposeProgressResponse {

    private String status;
    private String phase;
    private int progress;
    private ProgressDetails details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressDetails {
        private int completedParagraphs;
        private int totalParagraphs;
        private String currentStep;
        private long estimatedRemainingSeconds;
    }
}
