package com.shopvideoscout.task.service;

import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.common.util.RedisUtils;
import com.shopvideoscout.task.dto.ComposeProgressResponse;
import com.shopvideoscout.task.entity.Task;
import com.shopvideoscout.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for querying compose progress from Redis.
 * Progress data is written by media-service, read here by task-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComposeProgressService {

    private final RedisUtils redisUtils;
    private final TaskMapper taskMapper;

    private static final String PROGRESS_KEY_PREFIX = "task:progress:";

    /**
     * Get compose progress for a task.
     *
     * @param taskId task ID
     * @param userId user ID for ownership check
     * @return progress response
     */
    public ComposeProgressResponse getProgress(Long taskId, Long userId) {
        // Validate task ownership
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问此任务");
        }

        String key = PROGRESS_KEY_PREFIX + taskId;

        Map<Object, Object> progressData;
        try {
            progressData = redisUtils.hGetAll(key);
        } catch (Exception e) {
            log.error("Failed to read progress from Redis for task {}: {}", taskId, e.getMessage());
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE,
                    "进度查询服务暂时不可用");
        }

        if (progressData == null || progressData.isEmpty()) {
            // No progress data - return default based on task status
            return buildDefaultProgress(task);
        }

        return buildProgressResponse(progressData);
    }

    private ComposeProgressResponse buildProgressResponse(Map<Object, Object> data) {
        int completed = parseIntSafe(data.get("completed_paragraphs"));
        int total = parseIntSafe(data.get("total_paragraphs"));
        long estimatedRemaining = parseLongSafe(data.get("estimated_remaining_seconds"));
        String status = getStringSafe(data.get("status"));
        String currentStep = getStringSafe(data.get("current_step"));

        int progressPercent = total > 0 ? (int) ((completed * 100.0) / total) : 0;

        return ComposeProgressResponse.builder()
                .status(status)
                .phase("tts_synthesis")
                .progress(progressPercent)
                .details(ComposeProgressResponse.ProgressDetails.builder()
                        .completedParagraphs(completed)
                        .totalParagraphs(total)
                        .currentStep(currentStep)
                        .estimatedRemainingSeconds(estimatedRemaining)
                        .build())
                .build();
    }

    private ComposeProgressResponse buildDefaultProgress(Task task) {
        return ComposeProgressResponse.builder()
                .status(task.getStatus())
                .phase("tts_synthesis")
                .progress(0)
                .details(ComposeProgressResponse.ProgressDetails.builder()
                        .completedParagraphs(0)
                        .totalParagraphs(0)
                        .currentStep("等待开始")
                        .estimatedRemainingSeconds(0)
                        .build())
                .build();
    }

    private int parseIntSafe(Object value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long parseLongSafe(Object value) {
        if (value == null) return 0;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getStringSafe(Object value) {
        return value != null ? value.toString() : "";
    }
}
