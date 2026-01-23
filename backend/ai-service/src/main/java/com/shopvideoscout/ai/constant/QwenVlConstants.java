package com.shopvideoscout.ai.constant;

/**
 * Constants for Qwen-VL API integration.
 */
public final class QwenVlConstants {

    private QwenVlConstants() {}

    /**
     * Maximum retry attempts on timeout (BR: 3x retry on 504).
     */
    public static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * Maximum tags per frame (BR-1.2).
     */
    public static final int MAX_TAGS = 5;

    /**
     * Quality score range (BR-1.3).
     */
    public static final int MIN_QUALITY_SCORE = 0;
    public static final int MAX_QUALITY_SCORE = 100;

    /**
     * Request timeout in seconds.
     */
    public static final int REQUEST_TIMEOUT_SECONDS = 30;

    /**
     * Frame categories (BR-1.1).
     */
    public static final class Category {
        public static final String FOOD = "food";
        public static final String PERSON = "person";
        public static final String ENVIRONMENT = "environment";
        public static final String OTHER = "other";

        public static boolean isValid(String category) {
            return FOOD.equals(category) ||
                   PERSON.equals(category) ||
                   ENVIRONMENT.equals(category) ||
                   OTHER.equals(category);
        }
    }
}
