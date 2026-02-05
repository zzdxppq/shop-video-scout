package com.shopvideoscout.publish.client;

import com.shopvideoscout.publish.dto.ScriptDto;
import com.shopvideoscout.publish.dto.TaskDetailDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for task-service internal APIs.
 * Story 5.3: 发布辅助服务
 */
@FeignClient(name = "task-service", path = "/internal/tasks")
public interface TaskServiceClient {

    /**
     * Get task details.
     *
     * @param taskId task ID
     * @return task details including shop_name, shop_type, status
     */
    @GetMapping("/{id}")
    TaskDetailDto getTask(@PathVariable("id") Long taskId);

    /**
     * Get task script content.
     *
     * @param taskId task ID
     * @return script with paragraphs
     */
    @GetMapping("/{id}/script")
    ScriptDto getScript(@PathVariable("id") Long taskId);
}
