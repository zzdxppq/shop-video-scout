package com.shopvideoscout.task.constant;

import java.util.Set;

/**
 * Voice type constants for TTS.
 */
public final class VoiceConstants {

    private VoiceConstants() {}

    /**
     * Valid standard voice types (火山引擎 Seed-TTS presets).
     * BR-1.1: 活泼女声、阳光男声、知性女声
     */
    public static final Set<String> VALID_VOICE_TYPES = Set.of(
            "xiaomei",       // 活泼女声
            "yangguang",     // 阳光男声
            "zhixing"        // 知性女声
    );

    public static final String DEFAULT_VOICE_TYPE = "xiaomei";

    /**
     * Maximum text length per TTS request (BR-1.2 error handling).
     */
    public static final int MAX_TEXT_LENGTH = 5000;
}
