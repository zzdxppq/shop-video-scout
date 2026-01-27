package com.shopvideoscout.media.service;

import com.shopvideoscout.common.util.RedisUtils;
import com.shopvideoscout.media.config.ComposeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Tracks compose progress in Redis.
 * Written by media-service, read by task-service.
 * Redis key: task:progress:{taskId} (Hash, TTL 1h)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComposeProgressTracker {

    private final RedisUtils redisUtils;
    private final ComposeProperties composeProperties;

    private static final String PROGRESS_KEY_PREFIX = "task:progress:";

    /**
     * Initialize progress tracking for a compose task.
     */
    public void initProgress(Long taskId, int totalParagraphs) {
        String key = progressKey(taskId);
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", "synthesizing");
        fields.put("completed_paragraphs", "0");
        fields.put("total_paragraphs", String.valueOf(totalParagraphs));
        fields.put("estimated_remaining_seconds", "0");
        fields.put("current_step", "TTS合成");
        fields.put("error_message", "");

        redisUtils.hSetAll(key, fields);
        redisUtils.expire(key, composeProperties.getProgressTtlSeconds(), TimeUnit.SECONDS);
        log.debug("Initialized progress for task {}: totalParagraphs={}", taskId, totalParagraphs);
    }

    /**
     * Update progress after a paragraph completes TTS.
     */
    public void updateParagraphComplete(Long taskId, int completedCount, int totalCount,
                                         double avgDurationPerParagraph) {
        String key = progressKey(taskId);
        int remaining = totalCount - completedCount;
        long estimatedSeconds = Math.round(remaining * avgDurationPerParagraph);

        redisUtils.hSet(key, "completed_paragraphs", String.valueOf(completedCount));
        redisUtils.hSet(key, "estimated_remaining_seconds", String.valueOf(estimatedSeconds));
        log.debug("Progress for task {}: {}/{}, estimated remaining: {}s",
                taskId, completedCount, totalCount, estimatedSeconds);
    }

    /**
     * Mark compose as completed.
     */
    public void markComplete(Long taskId) {
        String key = progressKey(taskId);
        redisUtils.hSet(key, "status", "completed");
        redisUtils.hSet(key, "current_step", "完成");
        redisUtils.hSet(key, "estimated_remaining_seconds", "0");
        log.info("Compose completed for task {}", taskId);
    }

    /**
     * Mark compose as failed.
     */
    public void markFailed(Long taskId, String errorMessage) {
        String key = progressKey(taskId);
        redisUtils.hSet(key, "status", "failed");
        redisUtils.hSet(key, "current_step", "失败");
        redisUtils.hSet(key, "error_message", errorMessage);
        log.error("Compose failed for task {}: {}", taskId, errorMessage);
    }

    private String progressKey(Long taskId) {
        return PROGRESS_KEY_PREFIX + taskId;
    }
}
