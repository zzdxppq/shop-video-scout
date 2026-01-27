package com.shopvideoscout.task.controller;

import com.shopvideoscout.common.result.R;
import com.shopvideoscout.task.constant.TaskConstants;
import com.shopvideoscout.task.entity.Task;
import com.shopvideoscout.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Internal callback controller for media-service to notify compose completion.
 * Not exposed through Gateway - internal service-to-service communication only.
 */
@Slf4j
@RestController
@RequestMapping("/internal/tasks")
@RequiredArgsConstructor
public class ComposeCallbackController {

    private final TaskMapper taskMapper;

    /**
     * Receive compose completion callback from media-service.
     *
     * @param taskId task ID
     * @param body   callback payload with status and result details
     */
    @PostMapping("/{taskId}/compose-complete")
    public R<Void> composeComplete(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        log.info("Compose callback for task {}: status={}", taskId, status);

        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("Compose callback for non-existent task: {}", taskId);
            return R.ok();
        }

        if ("completed".equals(status)) {
            task.setStatus(TaskConstants.TaskStatus.COMPLETED);
            if (body.get("totalDurationSeconds") != null) {
                task.setOutputDurationSeconds(
                        ((Number) body.get("totalDurationSeconds")).intValue());
            }
            taskMapper.updateById(task);
            log.info("Task {} compose completed, status → completed", taskId);
        } else if ("failed".equals(status)) {
            task.setStatus(TaskConstants.TaskStatus.FAILED);
            task.setErrorMessage((String) body.get("errorMessage"));
            taskMapper.updateById(task);
            log.error("Task {} compose failed: {}", taskId, body.get("errorMessage"));
        }

        return R.ok();
    }
}
