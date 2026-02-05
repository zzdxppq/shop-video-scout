package com.shopvideoscout.publish.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for task details from internal API.
 * Story 5.3: 发布辅助服务
 */
@Data
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
