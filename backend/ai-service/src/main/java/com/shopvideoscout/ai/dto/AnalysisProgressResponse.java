package com.shopvideoscout.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for analysis progress.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisProgressResponse {

    /**
     * Task ID.
     */
    @JsonProperty("task_id")
    private Long taskId;

    /**
     * Current analysis status: pending, analyzing, completed, failed.
     */
    private String status;

    /**
     * Total number of frames to analyze.
     */
    @JsonProperty("total_frames")
    private Integer totalFrames;

    /**
     * Number of frames analyzed so far.
     */
    @JsonProperty("analyzed_frames")
    private Integer analyzedFrames;

    /**
     * Progress percentage (0-100).
     */
    @JsonProperty("progress_percent")
    private Integer progressPercent;

    /**
     * Error message if status is failed.
     */
    @JsonProperty("error_message")
    private String errorMessage;

    /**
     * Create a pending response (analysis not yet started).
     */
    public static AnalysisProgressResponse pending(Long taskId) {
        return AnalysisProgressResponse.builder()
                .taskId(taskId)
                .status("pending")
                .totalFrames(0)
                .analyzedFrames(0)
                .progressPercent(0)
                .build();
    }

    /**
     * Create an in-progress response.
     */
    public static AnalysisProgressResponse inProgress(Long taskId, int totalFrames, int analyzedFrames) {
        int percent = totalFrames > 0 ? (analyzedFrames * 100 / totalFrames) : 0;
        return AnalysisProgressResponse.builder()
                .taskId(taskId)
                .status("analyzing")
                .totalFrames(totalFrames)
                .analyzedFrames(analyzedFrames)
                .progressPercent(percent)
                .build();
    }

    /**
     * Create a completed response.
     */
    public static AnalysisProgressResponse completed(Long taskId, int totalFrames) {
        return AnalysisProgressResponse.builder()
                .taskId(taskId)
                .status("completed")
                .totalFrames(totalFrames)
                .analyzedFrames(totalFrames)
                .progressPercent(100)
                .build();
    }

    /**
     * Create a failed response.
     */
    public static AnalysisProgressResponse failed(Long taskId, String errorMessage) {
        return AnalysisProgressResponse.builder()
                .taskId(taskId)
                .status("failed")
                .errorMessage(errorMessage)
                .build();
    }
}
