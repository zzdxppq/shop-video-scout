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

    /**
     * Whether subtitles are enabled (Story 4.3).
     */
    private Boolean subtitleEnabled;

    /**
     * Subtitle style template (Story 4.3).
     * Values: simple_white, vibrant_yellow, xiaohongshu, douyin_hot, neon
     */
    private String subtitleStyle;

    private String callbackUrl;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Paragraph implements Serializable {
        private int index;
        private String text;
        /**
         * Shot ID mapping to videos.id for video segment cutting (Story 4.3).
         */
        private Long shotId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoiceConfig implements Serializable {
        private String type;
        private String voiceId;
        /**
         * Voice sample ID for clone voice (Story 4.2).
         * When set, TtsSynthesisService resolves clone_voice_id via DB lookup.
         */
        private Long voiceSampleId;
        /**
         * User ID for ownership verification when using clone voice (SEC-002).
         */
        private Long userId;
    }
}
