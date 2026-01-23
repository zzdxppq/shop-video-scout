package com.shopvideoscout.ai.controller;

import com.shopvideoscout.ai.dto.AnalysisProgressResponse;
import com.shopvideoscout.ai.dto.AnalyzeTaskResponse;
import com.shopvideoscout.ai.service.FrameAnalysisService;
import com.shopvideoscout.common.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for AI analysis endpoints.
 *
 * Endpoints:
 * - POST /api/v1/tasks/{id}/analyze - Trigger AI analysis for task
 * - GET /api/v1/tasks/{id}/analysis-progress - Get analysis progress
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tasks/{taskId}")
@RequiredArgsConstructor
public class AnalysisController {

    private final FrameAnalysisService frameAnalysisService;

    /**
     * Trigger AI analysis for a task.
     * POST /api/v1/tasks/{taskId}/analyze
     *
     * Analyzes all video frames for the task using Qwen-VL:
     * - Classifies each frame (food/person/environment/other)
     * - Generates description tags (max 5)
     * - Calculates quality score (0-100)
     * - Marks top 2 frames per category as recommended
     *
     * @param taskId Task ID
     * @param userId User ID from Gateway header
     * @return Analysis job status
     */
    @PostMapping("/analyze")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public R<AnalyzeTaskResponse> triggerAnalysis(
            @PathVariable Long taskId,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("Triggering analysis for task {} by user {}", taskId, userId);

        // TODO: Validate task ownership (requires TaskMapper or Feign call to task-service)
        // For now, the Gateway validates JWT and provides userId

        AnalyzeTaskResponse response = frameAnalysisService.triggerAnalysis(taskId);
        return R.ok(response);
    }

    /**
     * Get analysis progress for a task.
     * GET /api/v1/tasks/{taskId}/analysis-progress
     *
     * Returns current analysis status and progress percentage.
     *
     * @param taskId Task ID
     * @param userId User ID from Gateway header
     * @return Analysis progress
     */
    @GetMapping("/analysis-progress")
    public R<AnalysisProgressResponse> getAnalysisProgress(
            @PathVariable Long taskId,
            @RequestHeader("X-User-Id") Long userId) {
        log.debug("Getting analysis progress for task {} by user {}", taskId, userId);

        // TODO: Validate task ownership

        AnalysisProgressResponse response = frameAnalysisService.getProgress(taskId);
        return R.ok(response);
    }
}
