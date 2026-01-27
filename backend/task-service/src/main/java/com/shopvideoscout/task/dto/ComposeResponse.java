package com.shopvideoscout.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned when compose is triggered.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComposeResponse {

    private String status;
    private Long taskId;
}
