package com.shopvideoscout.ai.controller;

import com.shopvideoscout.ai.client.TaskServiceClient;
import com.shopvideoscout.ai.dto.ScriptResponse;
import com.shopvideoscout.ai.entity.Script;
import com.shopvideoscout.ai.service.ScriptGenerationService;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.R;
import com.shopvideoscout.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for script generation and retrieval.
 * Story 3.1: AI脚本生成服务
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptGenerationService scriptGenerationService;
    private final TaskServiceClient taskServiceClient;

    /**
     * Get script for a task.
     * GET /api/v1/tasks/{taskId}/script
     *
     * @param taskId Task ID
     * @return Script content with paragraphs and metadata
     */
    @GetMapping("/{taskId}/script")
    public R<ScriptResponse> getScript(@PathVariable Long taskId) {
        log.debug("Get script for task {}", taskId);

        // Check task exists and get regenerate count
        R<TaskServiceClient.TaskInfo> taskResult = taskServiceClient.getTaskInfo(taskId);
        if (!taskResult.isSuccess() || taskResult.getData() == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        TaskServiceClient.TaskInfo task = taskResult.getData();

        Script script = scriptGenerationService.getByTaskId(taskId);
        if (script == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "脚本尚未生成");
        }

        int regenerateCount = task.getScriptRegenerateCount() != null ? task.getScriptRegenerateCount() : 0;
        int remaining = scriptGenerationService.getRemainingRegenerations(regenerateCount);
        return R.ok(ScriptResponse.fromEntity(script, remaining));
    }

    /**
     * Regenerate script for a task.
     * POST /api/v1/tasks/{taskId}/regenerate-script
     *
     * @param taskId Task ID
     * @return New script content after regeneration
     */
    @PostMapping("/{taskId}/regenerate-script")
    public R<ScriptResponse> regenerateScript(@PathVariable Long taskId) {
        log.info("Regenerate script for task {}", taskId);

        // Get task info including shop details and regenerate count
        R<TaskServiceClient.TaskInfo> taskResult = taskServiceClient.getTaskInfo(taskId);
        if (!taskResult.isSuccess() || taskResult.getData() == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        TaskServiceClient.TaskInfo task = taskResult.getData();

        int regenerateCount = task.getScriptRegenerateCount() != null ? task.getScriptRegenerateCount() : 0;

        // Check regeneration limit before calling service
        if (!scriptGenerationService.canRegenerate(regenerateCount)) {
            throw new BusinessException(429, "重新生成次数已达上限，请手动编辑");
        }

        // Regenerate script
        Script script = scriptGenerationService.regenerateScript(
                taskId,
                task.getShopName(),
                task.getShopType(),
                task.getPromotionText(),
                task.getVideoStyle(),
                regenerateCount
        );

        // Update task regenerate count
        taskServiceClient.incrementRegenerateCount(taskId);

        int remaining = scriptGenerationService.getRemainingRegenerations(regenerateCount + 1);
        return R.ok(ScriptResponse.fromEntity(script, remaining));
    }
}
