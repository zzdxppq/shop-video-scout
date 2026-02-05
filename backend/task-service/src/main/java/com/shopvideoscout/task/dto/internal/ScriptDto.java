package com.shopvideoscout.task.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO for internal script API.
 * Story 5.3: 发布辅助服务
 */
@Data
@Builder
public class ScriptDto {

    @JsonProperty("task_id")
    private Long taskId;

    private List<Paragraph> paragraphs;

    @Data
    @Builder
    public static class Paragraph {
        private String id;
        private String text;
    }
}
