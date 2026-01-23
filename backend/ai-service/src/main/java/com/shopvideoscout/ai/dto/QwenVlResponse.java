package com.shopvideoscout.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response from Qwen-VL API.
 * Follows DashScope multimodal conversation response format.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QwenVlResponse {

    @JsonProperty("request_id")
    private String requestId;

    private Output output;

    private Usage usage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output {
        private List<Choice> choices;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        @JsonProperty("finish_reason")
        private String finishReason;

        private Message message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private List<Content> content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        @JsonProperty("input_tokens")
        private Integer inputTokens;

        @JsonProperty("output_tokens")
        private Integer outputTokens;
    }

    /**
     * Extract text content from response.
     */
    public String getTextContent() {
        if (output == null || output.getChoices() == null || output.getChoices().isEmpty()) {
            return null;
        }
        Choice choice = output.getChoices().get(0);
        if (choice.getMessage() == null || choice.getMessage().getContent() == null ||
            choice.getMessage().getContent().isEmpty()) {
            return null;
        }
        return choice.getMessage().getContent().get(0).getText();
    }
}
