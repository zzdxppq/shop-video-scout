package com.shopvideoscout.task.dto;

import com.shopvideoscout.task.entity.Task;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for task information.
 */
@Data
public class TaskResponse {

    private Long id;
    private String shopName;
    private String shopType;
    private String promotionText;
    private String videoStyle;
    private String status;
    private LocalDateTime createdAt;

    /**
     * Create response from entity.
     */
    public static TaskResponse fromEntity(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setShopName(task.getShopName());
        response.setShopType(task.getShopType());
        response.setPromotionText(task.getPromotionText());
        response.setVideoStyle(task.getVideoStyle());
        response.setStatus(task.getStatus());
        response.setCreatedAt(task.getCreatedAt());
        return response;
    }
}
