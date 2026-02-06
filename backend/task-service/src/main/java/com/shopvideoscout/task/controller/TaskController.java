package com.shopvideoscout.task.controller;

import com.shopvideoscout.common.result.R;
import com.shopvideoscout.task.dto.CreateTaskRequest;
import com.shopvideoscout.task.dto.PagedResponse;
import com.shopvideoscout.task.dto.SubtitleSettingsRequest;
import com.shopvideoscout.task.dto.SubtitleSettingsResponse;
import com.shopvideoscout.task.dto.TaskResponse;
import com.shopvideoscout.task.dto.TaskSummaryResponse;
import com.shopvideoscout.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Task controller handling task CRUD operations.
 * All endpoints require JWT authentication (validated by Gateway).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * Create a new task.
     * POST /api/v1/tasks
     *
     * BR-2.1: Task is associated with current logged-in user.
     * BR-2.2: Max 5 in-progress tasks per user (429 if exceeded).
     *
     * @param request task creation request
     * @param userId injected from JWT via Gateway header
     * @return created task with 201 status
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public R<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.debug("Create task request from user: {}", userId);
        TaskResponse response = taskService.createTask(userId, request);
        return R.ok(response);
    }

    /**
     * Get task list for current user (in-progress tasks only).
     * GET /api/v1/tasks
     *
     * @param userId injected from JWT via Gateway header
     * @return list of user's in-progress tasks
     */
    @GetMapping
    public R<List<TaskResponse>> getTasks(@RequestHeader("X-User-Id") Long userId) {
        log.debug("Get tasks request from user: {}", userId);
        List<TaskResponse> tasks = taskService.getInProgressTasks(userId);
        return R.ok(tasks);
    }

    /**
     * Get paginated task history for current user.
     * GET /api/v1/tasks/history
     * Story 5.5: 历史任务管理
     *
     * @param page page number (1-indexed, default 1)
     * @param size page size (default 10, max 50)
     * @param userId injected from JWT via Gateway header
     * @return paginated task summary list
     */
    @GetMapping("/history")
    public R<PagedResponse<TaskSummaryResponse>> getTaskHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("X-User-Id") Long userId) {
        log.debug("Get task history request from user: {}, page: {}, size: {}", userId, page, size);
        PagedResponse<TaskSummaryResponse> response = taskService.getTaskHistory(userId, page, size);
        return R.ok(response);
    }

    /**
     * Get task by ID.
     * GET /api/v1/tasks/{id}
     *
     * @param id task ID
     * @param userId injected from JWT via Gateway header
     * @return task details
     */
    @GetMapping("/{id}")
    public R<TaskResponse> getTask(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.debug("Get task {} request from user: {}", id, userId);
        TaskResponse response = taskService.getTaskForUser(id, userId);
        return R.ok(response);
    }

    /**
     * Update subtitle settings for a task.
     * PUT /api/v1/tasks/{id}/subtitle-settings
     * Story 4.5: 字幕设置页面
     *
     * @param id task ID
     * @param request subtitle settings request
     * @param userId injected from JWT via Gateway header
     * @return updated subtitle settings
     */
    @PutMapping("/{id}/subtitle-settings")
    public R<SubtitleSettingsResponse> updateSubtitleSettings(
            @PathVariable Long id,
            @Valid @RequestBody SubtitleSettingsRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.debug("Update subtitle settings for task {} from user: {}", id, userId);
        SubtitleSettingsResponse response = taskService.updateSubtitleSettings(id, userId, request);
        return R.ok(response);
    }

    /**
     * Delete a task.
     * DELETE /api/v1/tasks/{id}
     * Story 5.5: 历史任务管理
     *
     * Deletes the task and associated OSS files.
     * Only the task owner can delete.
     * Cannot delete tasks that are currently processing (analyzing/composing).
     *
     * @param id task ID
     * @param userId injected from JWT via Gateway header
     * @return 200 OK on success
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteTask(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.debug("Delete task {} request from user: {}", id, userId);
        taskService.deleteTask(id, userId);
        return R.ok();
    }
}
