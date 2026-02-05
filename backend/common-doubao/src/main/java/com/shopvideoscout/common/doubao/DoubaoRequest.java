package com.shopvideoscout.common.doubao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Request body for Doubao API chat completions.
 * Story 5.3: 发布辅助服务
 */
@Data
@Builder
public class DoubaoRequest {

    /**
     * Model name.
     */
    private String model;

    /**
     * Chat messages.
     */
    private List<Message> messages;

    /**
     * Temperature for randomness (0.0-2.0).
     */
    private Double temperature;

    /**
     * Maximum tokens to generate.
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * Chat message.
     */
    @Data
    @Builder
    public static class Message {
        /**
         * Role: system, user, or assistant.
         */
        private String role;

        /**
         * Message content.
         */
        private String content;
    }
}
