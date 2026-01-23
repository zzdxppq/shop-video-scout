package com.shopvideoscout.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for Qwen-VL API call.
 * Follows DashScope multimodal conversation format.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QwenVlRequest {

    private String model;

    private Input input;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Input {
        private List<Message> messages;
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
        /**
         * Content type: "image" or "text"
         */
        private String type;

        /**
         * Image URL (when type is "image")
         */
        private String image;

        /**
         * Text content (when type is "text")
         */
        private String text;
    }

    /**
     * Create a frame analysis request.
     */
    public static QwenVlRequest createFrameAnalysisRequest(String imageUrl, String prompt) {
        return QwenVlRequest.builder()
                .model("qwen-vl-max")
                .input(Input.builder()
                        .messages(List.of(
                                Message.builder()
                                        .role("user")
                                        .content(List.of(
                                                Content.builder().type("image").image(imageUrl).build(),
                                                Content.builder().type("text").text(prompt).build()
                                        ))
                                        .build()
                        ))
                        .build())
                .build();
    }
}
