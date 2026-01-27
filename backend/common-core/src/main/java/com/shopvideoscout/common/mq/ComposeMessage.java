package com.shopvideoscout.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * MQ message for triggering TTS compose in media-service.
 * Published by task-service, consumed by media-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComposeMessage implements Serializable {

    private Long taskId;

    private List<Paragraph> paragraphs;

    private VoiceConfig voiceConfig;

    private String callbackUrl;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Paragraph implements Serializable {
        private int index;
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoiceConfig implements Serializable {
        private String type;
        private String voiceId;
    }
}
