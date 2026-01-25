package com.shopvideoscout.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shopvideoscout.ai.entity.Script;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * API response DTO for script data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptResponse {

    private Long id;

    @JsonProperty("task_id")
    private Long taskId;

    private Integer version;

    @JsonProperty("is_user_edited")
    private Boolean isUserEdited;

    private List<ScriptContent.ScriptParagraph> paragraphs;

    @JsonProperty("total_duration")
    private Integer totalDuration;

    /**
     * Remaining regeneration attempts (5 - task.scriptRegenerateCount).
     */
    @JsonProperty("regenerate_remaining")
    private Integer regenerateRemaining;

    /**
     * Create response from entity.
     */
    public static ScriptResponse fromEntity(Script script, int regenerateRemaining) {
        ScriptContent content = script.getContent();
        return ScriptResponse.builder()
                .id(script.getId())
                .taskId(script.getTaskId())
                .version(script.getVersion())
                .isUserEdited(script.getIsUserEdited())
                .paragraphs(content != null ? content.getParagraphs() : null)
                .totalDuration(content != null ? content.getTotalDuration() : null)
                .regenerateRemaining(regenerateRemaining)
                .build();
    }
}
