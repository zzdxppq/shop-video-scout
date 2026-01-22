package com.shopvideoscout.task.constant;

/**
 * Task service constants.
 */
public final class TaskConstants {

    private TaskConstants() {}

    /**
     * Maximum number of in-progress tasks per user (BR-2.2).
     */
    public static final int MAX_IN_PROGRESS_TASKS = 5;

    /**
     * Maximum shop name length (BR-1.1).
     */
    public static final int MAX_SHOP_NAME_LENGTH = 200;

    /**
     * Maximum promotion text length (BR-1.2).
     */
    public static final int MAX_PROMOTION_TEXT_LENGTH = 500;

    /**
     * Shop types enum values.
     */
    public static final class ShopType {
        public static final String FOOD = "food";
        public static final String BEAUTY = "beauty";
        public static final String ENTERTAINMENT = "entertainment";
        public static final String OTHER = "other";
    }

    /**
     * Video style enum values.
     */
    public static final class VideoStyle {
        public static final String RECOMMEND = "recommend";
        public static final String REVIEW = "review";
        public static final String VLOG = "vlog";
    }

    /**
     * Task status enum values.
     */
    public static final class TaskStatus {
        public static final String CREATED = "created";
        public static final String UPLOADING = "uploading";
        public static final String ANALYZING = "analyzing";
        public static final String SCRIPT_READY = "script_ready";
        public static final String SCRIPT_EDITED = "script_edited";
        public static final String VOICE_SET = "voice_set";
        public static final String COMPOSING = "composing";
        public static final String COMPLETED = "completed";
        public static final String FAILED = "failed";
    }

    /**
     * In-progress task statuses for limit check.
     */
    public static final String[] IN_PROGRESS_STATUSES = {
        TaskStatus.CREATED,
        TaskStatus.UPLOADING,
        TaskStatus.ANALYZING,
        TaskStatus.SCRIPT_READY,
        TaskStatus.SCRIPT_EDITED,
        TaskStatus.VOICE_SET,
        TaskStatus.COMPOSING
    };
}
