package com.shopvideoscout.task.controller;

import com.shopvideoscout.common.result.R;
import com.shopvideoscout.task.dto.CreateTaskRequest;
import com.shopvideoscout.task.dto.TaskResponse;
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
     * Get task list for current user.
     * GET /api/v1/tasks
     *
     * @param userId injected from JWT via Gateway header
     * @return list of user's tasks
     */
    @GetMapping
    public R<List<TaskResponse>> getTasks(@RequestHeader("X-User-Id") Long userId) {
        log.debug("Get tasks request from user: {}", userId);
        List<TaskResponse> tasks = taskService.getInProgressTasks(userId);
        return R.ok(tasks);
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
}
