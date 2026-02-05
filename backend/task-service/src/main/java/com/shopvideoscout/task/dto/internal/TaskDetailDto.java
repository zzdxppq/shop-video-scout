package com.shopvideoscout.task.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * DTO for internal task details API.
 * Story 5.3: 发布辅助服务
 */
@Data
@Builder
public class TaskDetailDto {

    private Long id;

    @JsonProperty("shop_name")
    private String shopName;

    @JsonProperty("shop_type")
    private String shopType;

    private String status;

    @JsonProperty("user_id")
    private Long userId;
}
