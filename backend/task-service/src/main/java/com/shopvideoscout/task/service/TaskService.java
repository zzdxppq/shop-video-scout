package com.shopvideoscout.task.service;

import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.task.constant.TaskConstants;
import com.shopvideoscout.task.dto.CreateTaskRequest;
import com.shopvideoscout.task.dto.PagedResponse;
import com.shopvideoscout.task.dto.SubtitleSettingsRequest;
import com.shopvideoscout.task.dto.SubtitleSettingsResponse;
import com.shopvideoscout.task.dto.TaskResponse;
import com.shopvideoscout.task.dto.TaskSummaryResponse;
import com.shopvideoscout.task.entity.Task;
import com.shopvideoscout.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Task service handling task creation and management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final OssCleanupService ossCleanupService;

    /** Statuses that indicate task is in progress and cannot be deleted */
    private static final List<String> IN_PROGRESS_STATUSES = Arrays.asList(
        TaskConstants.TaskStatus.ANALYZING,
        TaskConstants.TaskStatus.COMPOSING
    );

    /**
     * Create a new task.
     * BR-2.1: Task is associated with current logged-in user.
     * BR-2.2: Max 5 in-progress tasks per user.
     *
     * @param userId current user ID
     * @param request task creation request
     * @return created task response
     * @throws BusinessException if user has too many in-progress tasks (429)
     */
    @Transactional
    public TaskResponse createTask(Long userId, CreateTaskRequest request) {
        log.debug("Creating task for user: {}, shop: {}", userId, request.getShopName());

        // BR-2.2: Check in-progress task limit
        int inProgressCount = taskMapper.countInProgressTasks(userId, TaskConstants.IN_PROGRESS_STATUSES);
        if (inProgressCount >= TaskConstants.MAX_IN_PROGRESS_TASKS) {
            log.warn("User {} has {} in-progress tasks, limit exceeded", userId, inProgressCount);
            throw new BusinessException(ResultCode.RATE_LIMIT_EXCEEDED, "您有太多进行中的任务，请完成后再创建");
        }

        // Create task entity
        Task task = Task.createNew(
            userId,
            request.getShopName(),
            request.getShopType(),
            request.getPromotionText(),
            request.getVideoStyle()
        );

        // Insert to database
        taskMapper.insert(task);
        log.info("Task created: id={}, userId={}, shopName={}", task.getId(), userId, request.getShopName());

        return TaskResponse.fromEntity(task);
    }

    /**
     * Get in-progress tasks for a user.
     *
     * @param userId user ID
     * @return list of in-progress tasks
     */
    public List<TaskResponse> getInProgressTasks(Long userId) {
        List<Task> tasks = taskMapper.findInProgressTasks(userId, TaskConstants.IN_PROGRESS_STATUSES);
        return tasks.stream()
            .map(TaskResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get task by ID.
     *
     * @param taskId task ID
     * @return task response
     * @throws BusinessException if task not found
     */
    public TaskResponse getTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        return TaskResponse.fromEntity(task);
    }

    /**
     * Get task by ID and verify ownership.
     *
     * @param taskId task ID
     * @param userId user ID
     * @return task response
     * @throws BusinessException if task not found or not owned by user
     */
    public TaskResponse getTaskForUser(Long taskId, Long userId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问此任务");
        }
        return TaskResponse.fromEntity(task);
    }

    /**
     * Update subtitle settings for a task.
     * Story 4.5: 字幕设置页面
     *
     * BR-1.2: 开关状态实时保存到任务配置中
     * BR-2.3: 样式选择实时保存到 tasks.subtitle_style 字段
     *
     * @param taskId task ID
     * @param userId user ID for ownership verification
     * @param request subtitle settings to update
     * @return updated subtitle settings
     * @throws BusinessException if task not found or not owned by user
     */
    @Transactional
    public SubtitleSettingsResponse updateSubtitleSettings(Long taskId, Long userId, SubtitleSettingsRequest request) {
        log.debug("Updating subtitle settings for task: {}, user: {}", taskId, userId);

        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND, "任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问此任务");
        }

        // Update subtitle settings
        task.setSubtitleEnabled(request.getSubtitleEnabled());
        task.setSubtitleStyle(request.getSubtitleStyle());

        taskMapper.updateById(task);
        log.info("Subtitle settings updated for task: {}, enabled={}, style={}",
            taskId, request.getSubtitleEnabled(), request.getSubtitleStyle());

        return SubtitleSettingsResponse.of(task.getSubtitleEnabled(), task.getSubtitleStyle());
    }

    /**
     * Get paginated task history for a user.
     * Story 5.5: 历史任务管理
     *
     * @param userId user ID
     * @param page page number (1-indexed)
     * @param size page size (max 50)
     * @return paginated task summary list
     */
    public PagedResponse<TaskSummaryResponse> getTaskHistory(Long userId, int page, int size) {
        log.debug("Getting task history for user: {}, page: {}, size: {}", userId, page, size);

        // Validate and normalize params
        page = Math.max(1, page);
        size = Math.min(50, Math.max(1, size));

        int offset = (page - 1) * size;

        // Get total count
        long total = taskMapper.countTasksByUser(userId);

        // Get page of tasks
        List<Task> tasks = taskMapper.findTasksByUserWithPagination(userId, offset, size);

        // Convert to summaries
        List<TaskSummaryResponse> summaries = tasks.stream()
            .map(task -> {
                // Generate thumbnail URL for completed tasks
                String thumbnailUrl = null;
                if (TaskConstants.TaskStatus.COMPLETED.equals(task.getStatus()) && task.getOutputOssKey() != null) {
                    // Thumbnail is stored at output/{taskId}/thumb.jpg
                    thumbnailUrl = ossCleanupService != null
                        ? String.format("output/%d/thumb.jpg", task.getId())
                        : null;
                }
                return TaskSummaryResponse.fromEntity(task, thumbnailUrl);
            })
            .collect(Collectors.toList());

        return PagedResponse.of(summaries, total, page, size);
    }

    /**
     * Delete a task and its associated files.
     * Story 5.5: 历史任务管理
     *
     * BR-2.1: Hard delete (no recovery)
     * BR-2.2: Delete OSS files asynchronously
     * BR-2.3: CASCADE handles related tables
     * BR-2.4: Validate user ownership
     * BR-2.5: Cannot delete tasks in progress (analyzing/composing)
     *
     * @param taskId task ID
     * @param userId user ID (for ownership validation)
     * @throws BusinessException if task not found, not owned by user, or in progress
     */
    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        log.debug("Deleting task: {}, user: {}", taskId, userId);

        // Find task
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND, "任务不存在");
        }

        // Validate ownership (BR-2.4)
        if (!task.getUserId().equals(userId)) {
            log.warn("User {} attempted to delete task {} owned by user {}",
                userId, taskId, task.getUserId());
            throw new BusinessException(ResultCode.TASK_DELETE_FORBIDDEN, "无权限删除此任务");
        }

        // Validate status (BR-2.5)
        if (IN_PROGRESS_STATUSES.contains(task.getStatus())) {
            log.warn("Attempted to delete task {} with status {}", taskId, task.getStatus());
            throw new BusinessException(ResultCode.TASK_IN_PROGRESS, "任务正在处理中，无法删除");
        }

        // Delete from database (CASCADE handles related tables - BR-2.3)
        int deleted = taskMapper.deleteById(taskId);
        if (deleted == 0) {
            throw new BusinessException(ResultCode.TASK_DELETE_FAILED, "删除任务失败");
        }

        log.info("Task {} deleted from database by user {}", taskId, userId);

        // Cleanup OSS files asynchronously (BR-2.2)
        ossCleanupService.cleanupTaskFiles(taskId, userId);
    }
}
