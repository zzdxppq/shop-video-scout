package com.shopvideoscout.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for GET /api/v1/tasks/{id}/output (Story 4.3).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutputResponse {

    /**
     * Task status (completed, composing, failed, etc.).
     */
    private String status;

    /**
     * Output video information.
     */
    private OutputVideoInfo video;

    /**
     * List of video shots used in the composition.
     */
    private List<ShotUsedInfo> shotsUsed;
}
