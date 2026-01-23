package com.shopvideoscout.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for triggering analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeTaskResponse {

    /**
     * Task ID.
     */
    @JsonProperty("task_id")
    private Long taskId;

    /**
     * Analysis job status: queued, already_analyzing, completed.
     */
    private String status;

    /**
     * Message describing the action taken.
     */
    private String message;

    /**
     * Total frames to be analyzed.
     */
    @JsonProperty("total_frames")
    private Integer totalFrames;

    public static AnalyzeTaskResponse queued(Long taskId, int totalFrames) {
        return AnalyzeTaskResponse.builder()
                .taskId(taskId)
                .status("queued")
                .message("分析任务已加入队列")
                .totalFrames(totalFrames)
                .build();
    }

    public static AnalyzeTaskResponse alreadyAnalyzing(Long taskId) {
        return AnalyzeTaskResponse.builder()
                .taskId(taskId)
                .status("already_analyzing")
                .message("任务正在分析中")
                .build();
    }

    public static AnalyzeTaskResponse completed(Long taskId) {
        return AnalyzeTaskResponse.builder()
                .taskId(taskId)
                .status("completed")
                .message("任务分析已完成")
                .build();
    }

    public static AnalyzeTaskResponse noFrames(Long taskId) {
        return AnalyzeTaskResponse.builder()
                .taskId(taskId)
                .status("no_frames")
                .message("没有可分析的帧")
                .totalFrames(0)
                .build();
    }
}
