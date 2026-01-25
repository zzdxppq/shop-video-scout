package com.shopvideoscout.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for Doubao API chat completions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoubaoRequest {

    /**
     * Model endpoint ID.
     */
    private String model;

    /**
     * Chat messages (system + user).
     */
    private List<Message> messages;

    /**
     * Temperature for response diversity (0.0-1.0).
     */
    private Double temperature;

    /**
     * Maximum tokens in response.
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;

        public static Message system(String content) {
            return Message.builder().role("system").content(content).build();
        }

        public static Message user(String content) {
            return Message.builder().role("user").content(content).build();
        }
    }

    /**
     * Create a script generation request.
     */
    public static DoubaoRequest forScriptGeneration(String endpointId, String prompt, double temperature) {
        return DoubaoRequest.builder()
                .model(endpointId)
                .messages(List.of(
                        Message.system("你是一位专业的探店视频文案撰写专家，擅长根据店铺信息和视频镜头生成吸引人的口播脚本。请严格按照JSON格式输出。"),
                        Message.user(prompt)
                ))
                .temperature(temperature)
                .maxTokens(2000)
                .build();
    }
}
