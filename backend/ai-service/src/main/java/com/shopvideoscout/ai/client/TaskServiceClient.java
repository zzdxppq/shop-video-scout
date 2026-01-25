package com.shopvideoscout.ai.client;

import com.shopvideoscout.common.result.R;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client for task-service communication.
 * Used to get task info and update task status.
 */
@FeignClient(name = "task-service", path = "/api/v1/tasks")
public interface TaskServiceClient {

    /**
     * Get task details including shop info and regenerate count.
     */
    @GetMapping("/{taskId}/internal")
    R<TaskInfo> getTaskInfo(@PathVariable("taskId") Long taskId);

    /**
     * Increment script regenerate count for a task.
     */
    @PostMapping("/{taskId}/increment-regenerate-count")
    R<Void> incrementRegenerateCount(@PathVariable("taskId") Long taskId);

    /**
     * Update task status.
     */
    @PutMapping("/{taskId}/status")
    R<Void> updateTaskStatus(@PathVariable("taskId") Long taskId, @RequestParam("status") String status);

    /**
     * Task info DTO returned from task-service.
     */
    @Data
    class TaskInfo {
        private Long id;
        private String shopName;
        private String shopType;
        private String promotionText;
        private String videoStyle;
        private String status;
        private Integer scriptRegenerateCount;
    }
}
