package com.shopvideoscout.common.doubao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Response body from Doubao API chat completions.
 * Story 5.3: 发布辅助服务
 */
@Data
public class DoubaoResponse {

    /**
     * Response ID.
     */
    private String id;

    /**
     * Object type.
     */
    private String object;

    /**
     * Creation timestamp.
     */
    private Long created;

    /**
     * Model used.
     */
    private String model;

    /**
     * Completion choices.
     */
    private List<Choice> choices;

    /**
     * Token usage statistics.
     */
    private Usage usage;

    /**
     * Completion choice.
     */
    @Data
    public static class Choice {
        /**
         * Choice index.
         */
        private Integer index;

        /**
         * Generated message.
         */
        private Message message;

        /**
         * Finish reason.
         */
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * Message in response.
     */
    @Data
    public static class Message {
        /**
         * Role: assistant.
         */
        private String role;

        /**
         * Generated content.
         */
        private String content;
    }

    /**
     * Token usage statistics.
     */
    @Data
    public static class Usage {
        /**
         * Tokens in prompt.
         */
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        /**
         * Tokens in completion.
         */
        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        /**
         * Total tokens.
         */
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    /**
     * Extract content from first choice.
     *
     * @return content string or null
     */
    public String getContent() {
        if (choices != null && !choices.isEmpty()) {
            Choice choice = choices.get(0);
            if (choice.getMessage() != null) {
                return choice.getMessage().getContent();
            }
        }
        return null;
    }
}
