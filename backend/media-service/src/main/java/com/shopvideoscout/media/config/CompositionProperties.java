package com.shopvideoscout.media.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for video composition (Story 4.3).
 */
@Data
@Component
@ConfigurationProperties(prefix = "composition")
public class CompositionProperties {

    /**
     * Path to FFmpeg binary.
     */
    private String ffmpegPath = "/usr/bin/ffmpeg";

    /**
     * Path to FFprobe binary for duration detection.
     */
    private String ffprobePath = "/usr/bin/ffprobe";

    /**
     * Temporary directory for intermediate files.
     */
    private String tempDir = "/tmp/compose";

    /**
     * Output video width (portrait mode).
     */
    private int outputWidth = 1080;

    /**
     * Output video height (portrait mode).
     */
    private int outputHeight = 1920;

    /**
     * Video bitrate in bits per second.
     */
    private String videoBitrate = "4M";

    /**
     * Frame rate.
     */
    private int frameRate = 30;

    /**
     * Audio bitrate.
     */
    private String audioBitrate = "128k";

    /**
     * Encoding preset (ultrafast, superfast, veryfast, faster, fast, medium, slow, slower, veryslow).
     */
    private String encodingPreset = "medium";

    /**
     * Transition duration in seconds added to each segment.
     */
    private double transitionDuration = 0.5;

    /**
     * Maximum retry attempts for OSS upload.
     */
    private int ossUploadMaxRetries = 2;

    /**
     * Retry interval for OSS upload in milliseconds.
     */
    private long ossUploadRetryIntervalMs = 5000;

    /**
     * Maximum retry attempts for callback.
     */
    private int callbackMaxRetries = 3;

    /**
     * Retry interval for callback in milliseconds.
     */
    private long callbackRetryIntervalMs = 10000;

    /**
     * Maximum retry attempts for FFmpeg operations.
     */
    private int ffmpegMaxRetries = 1;
}
