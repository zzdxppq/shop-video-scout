package com.shopvideoscout.task.constant;

/**
 * Video service constants.
 */
public final class VideoConstants {

    private VideoConstants() {}

    /**
     * Supported video file extensions (BR-1.2).
     */
    public static final String[] SUPPORTED_EXTENSIONS = {"mp4", "mov"};

    /**
     * Maximum file size in bytes: 100MB (BR-1.3).
     */
    public static final long MAX_FILE_SIZE = 100L * 1024 * 1024;

    /**
     * Maximum video duration in seconds: 3 minutes (BR-2.3).
     */
    public static final int MAX_DURATION_SECONDS = 180;

    /**
     * Maximum videos per task (BR-2.4).
     */
    public static final int MAX_VIDEOS_PER_TASK = 20;

    /**
     * Presigned URL expiration in minutes (AC1).
     */
    public static final int PRESIGNED_URL_EXPIRATION_MINUTES = 15;

    /**
     * Thumbnail dimensions (BR-2.2).
     */
    public static final int THUMBNAIL_WIDTH = 320;
    public static final int THUMBNAIL_HEIGHT = 180;

    /**
     * Frame extraction interval in seconds (BR-2.1).
     */
    public static final int FRAME_EXTRACTION_INTERVAL_SECONDS = 2;

    /**
     * Video status enum values.
     */
    public static final class VideoStatus {
        public static final String UPLOADING = "uploading";
        public static final String UPLOADED = "uploaded";
        public static final String ANALYZING = "analyzing";
        public static final String ANALYZED = "analyzed";
        public static final String FAILED = "failed";
    }

    /**
     * Video category enum values.
     */
    public static final class Category {
        public static final String FOOD = "food";
        public static final String PERSON = "person";
        public static final String ENVIRONMENT = "environment";
        public static final String OTHER = "other";
    }
}
