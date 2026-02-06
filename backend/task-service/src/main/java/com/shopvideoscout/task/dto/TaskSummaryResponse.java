package com.shopvideoscout.task.dto;

import com.shopvideoscout.task.entity.Task;
import lombok.Builder;
import lombok.Data;

/**
 * Task summary for history list.
 * Story 5.5: 历史任务管理
 */
@Data
@Builder
public class TaskSummaryResponse {

    private Long id;
    private String shopName;
    private String shopType;
    private String status;
    private String thumbnailUrl;
    private String createdAt;

    /**
     * Create summary from Task entity.
     */
    public static TaskSummaryResponse fromEntity(Task task, String thumbnailUrl) {
        return TaskSummaryResponse.builder()
            .id(task.getId())
            .shopName(task.getShopName())
            .shopType(task.getShopType())
            .status(task.getStatus())
            .thumbnailUrl(thumbnailUrl)
            .createdAt(task.getCreatedAt() != null ? task.getCreatedAt().toString() : null)
            .build();
    }
}
