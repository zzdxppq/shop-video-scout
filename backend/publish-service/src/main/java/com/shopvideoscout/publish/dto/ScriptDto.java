package com.shopvideoscout.publish.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO for script content from internal API.
 * Story 5.3: 发布辅助服务
 */
@Data
public class ScriptDto {

    @JsonProperty("task_id")
    private Long taskId;

    private List<Paragraph> paragraphs;

    @Data
    public static class Paragraph {
        private String id;
        private String text;
    }

    /**
     * Get concatenated script text.
     */
    public String getFullText() {
        if (paragraphs == null || paragraphs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Paragraph p : paragraphs) {
            if (p.getText() != null) {
                sb.append(p.getText()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Get summary (first 200 characters).
     */
    public String getSummary() {
        String full = getFullText();
        if (full.length() <= 200) {
            return full;
        }
        return full.substring(0, 200) + "...";
    }
}
