package com.shopvideoscout.ai.service;

import com.shopvideoscout.ai.client.QwenVlClient;
import com.shopvideoscout.ai.dto.AnalysisProgressResponse;
import com.shopvideoscout.ai.dto.AnalyzeTaskResponse;
import com.shopvideoscout.ai.dto.FrameAnalysisResult;
import com.shopvideoscout.ai.entity.VideoFrame;
import com.shopvideoscout.ai.mapper.VideoFrameMapper;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for orchestrating frame analysis.
 * Coordinates between frame extraction, AI analysis, and recommendation marking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FrameAnalysisService {

    private final VideoFrameMapper videoFrameMapper;
    private final QwenVlClient qwenVlClient;
    private final FrameRecommendationService recommendationService;
    private final StringRedisTemplate redisTemplate;

    private static final String ANALYSIS_STATUS_KEY = "analysis:status:task:";
    private static final String STATUS_ANALYZING = "analyzing";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";
    private static final long STATUS_EXPIRE_HOURS = 24;

    /**
     * Trigger analysis for a task.
     * Returns immediately and processes asynchronously.
     *
     * @param taskId Task ID to analyze
     * @return Analysis response with job status
     */
    public AnalyzeTaskResponse triggerAnalysis(Long taskId) {
        // Check if already analyzing
        String currentStatus = redisTemplate.opsForValue().get(ANALYSIS_STATUS_KEY + taskId);
        if (STATUS_ANALYZING.equals(currentStatus)) {
            log.info("Task {} already analyzing", taskId);
            return AnalyzeTaskResponse.alreadyAnalyzing(taskId);
        }

        // Get frames to analyze
        List<VideoFrame> frames = videoFrameMapper.findByTaskId(taskId);
        if (frames.isEmpty()) {
            log.info("No frames to analyze for task {}", taskId);
            return AnalyzeTaskResponse.noFrames(taskId);
        }

        // Check if already completed (all frames have category)
        long unanalyzed = frames.stream()
                .filter(f -> f.getCategory() == null)
                .count();
        if (unanalyzed == 0) {
            log.info("Task {} already fully analyzed", taskId);
            return AnalyzeTaskResponse.completed(taskId);
        }

        // Mark as analyzing and start async process
        redisTemplate.opsForValue().set(
                ANALYSIS_STATUS_KEY + taskId,
                STATUS_ANALYZING,
                STATUS_EXPIRE_HOURS,
                TimeUnit.HOURS
        );

        // Start async analysis
        processAnalysisAsync(taskId, frames);

        log.info("Triggered analysis for task {} with {} frames", taskId, frames.size());
        return AnalyzeTaskResponse.queued(taskId, frames.size());
    }

    /**
     * Get analysis progress for a task.
     *
     * @param taskId Task ID
     * @return Progress response
     */
    public AnalysisProgressResponse getProgress(Long taskId) {
        String status = redisTemplate.opsForValue().get(ANALYSIS_STATUS_KEY + taskId);

        int totalFrames = videoFrameMapper.countByTaskId(taskId);
        if (totalFrames == 0) {
            return AnalysisProgressResponse.pending(taskId);
        }

        int analyzedFrames = videoFrameMapper.countAnalyzedByTaskId(taskId);

        if (STATUS_COMPLETED.equals(status) || analyzedFrames == totalFrames) {
            return AnalysisProgressResponse.completed(taskId, totalFrames);
        }

        if (STATUS_FAILED.equals(status)) {
            return AnalysisProgressResponse.failed(taskId, "分析过程中发生错误");
        }

        return AnalysisProgressResponse.inProgress(taskId, totalFrames, analyzedFrames);
    }

    /**
     * Process analysis asynchronously.
     * Analyzes frames in batches with parallel processing.
     */
    @Async
    public void processAnalysisAsync(Long taskId, List<VideoFrame> frames) {
        log.info("Starting async analysis for task {} with {} frames", taskId, frames.size());

        try {
            // Filter unanalyzed frames
            List<VideoFrame> unanalyzedFrames = frames.stream()
                    .filter(f -> f.getCategory() == null)
                    .toList();

            // Analyze frames (can be parallelized)
            List<FrameAnalysisResult> results = analyzeFramesBatch(unanalyzedFrames);

            // Update database with results
            updateFrameAnalysisResults(results);

            // Calculate and mark recommendations
            markRecommendations(taskId, frames);

            // Mark completed
            redisTemplate.opsForValue().set(
                    ANALYSIS_STATUS_KEY + taskId,
                    STATUS_COMPLETED,
                    STATUS_EXPIRE_HOURS,
                    TimeUnit.HOURS
            );

            log.info("Completed analysis for task {}", taskId);

        } catch (Exception e) {
            log.error("Analysis failed for task {}: {}", taskId, e.getMessage(), e);
            redisTemplate.opsForValue().set(
                    ANALYSIS_STATUS_KEY + taskId,
                    STATUS_FAILED,
                    STATUS_EXPIRE_HOURS,
                    TimeUnit.HOURS
            );
        }
    }

    /**
     * Analyze frames in batch with parallel processing.
     */
    private List<FrameAnalysisResult> analyzeFramesBatch(List<VideoFrame> frames) {
        log.debug("Analyzing batch of {} frames", frames.size());

        // Process frames in parallel using CompletableFuture
        List<CompletableFuture<FrameAnalysisResult>> futures = frames.stream()
                .map(frame -> CompletableFuture.supplyAsync(() ->
                        qwenVlClient.analyzeFrame(frame.getId(), frame.getFrameUrl())))
                .toList();

        // Wait for all to complete and collect results
        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    /**
     * Update database with analysis results.
     */
    @Transactional
    public void updateFrameAnalysisResults(List<FrameAnalysisResult> results) {
        for (FrameAnalysisResult result : results) {
            if (result.isSuccess() && result.getFrameId() != null) {
                videoFrameMapper.updateAnalysisResult(
                        result.getFrameId(),
                        result.getCategory(),
                        result.getTags(),
                        result.getQualityScore(),
                        result.getDescription()
                );
                log.debug("Updated frame {} with category={}, score={}",
                        result.getFrameId(), result.getCategory(), result.getQualityScore());
            } else {
                log.warn("Skipping failed analysis result for frame {}: {}",
                        result.getFrameId(), result.getErrorMessage());
            }
        }
    }

    /**
     * Mark recommended frames for a task.
     */
    @Transactional
    public void markRecommendations(Long taskId, List<VideoFrame> allFrames) {
        // Reset all recommendations first
        videoFrameMapper.resetRecommendationsByTaskId(taskId);

        // Reload frames with analysis results
        List<VideoFrame> analyzedFrames = videoFrameMapper.findByTaskId(taskId);

        // Convert to analysis results for recommendation service
        List<FrameAnalysisResult> results = analyzedFrames.stream()
                .filter(f -> f.getCategory() != null)
                .map(this::frameToAnalysisResult)
                .toList();

        // Get recommended frame IDs
        List<Long> recommendedIds = recommendationService.markRecommendedFrames(results);

        // Update database
        for (Long frameId : recommendedIds) {
            videoFrameMapper.updateRecommended(frameId, true);
        }

        log.info("Marked {} frames as recommended for task {}", recommendedIds.size(), taskId);
    }

    private FrameAnalysisResult frameToAnalysisResult(VideoFrame frame) {
        return FrameAnalysisResult.builder()
                .frameId(frame.getId())
                .frameUrl(frame.getFrameUrl())
                .category(frame.getCategory())
                .tags(frame.getTags())
                .qualityScore(frame.getQualityScore())
                .description(frame.getDescription())
                .success(true)
                .build();
    }
}
