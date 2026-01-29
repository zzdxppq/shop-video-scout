package com.shopvideoscout.media.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.media.config.CompositionProperties;
import com.shopvideoscout.media.config.OssConfig;
import com.shopvideoscout.media.mapper.VideoReadMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for cutting video segments based on TTS paragraph durations (Story 4.3).
 * Uses FFmpeg to extract segments from source videos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoSegmentCuttingService {

    private final VideoReadMapper videoReadMapper;
    private final OSS ossClient;
    private final OssConfig ossConfig;
    private final CompositionProperties compositionProperties;

    /**
     * Cut video segments for all paragraphs.
     *
     * @param taskId            task ID
     * @param paragraphDurations list of paragraph durations from TTS
     * @return list of segment files
     */
    public List<SegmentResult> cutSegments(Long taskId, List<ParagraphDuration> paragraphDurations) {
        log.info("Starting video segment cutting for task {}: {} paragraphs", taskId, paragraphDurations.size());

        // Ensure temp directory exists
        File tempDir = new File(compositionProperties.getTempDir(), taskId.toString());
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "Failed to create temp directory");
        }

        List<SegmentResult> results = new ArrayList<>();
        List<File> downloadedFiles = new ArrayList<>();

        try {
            for (int i = 0; i < paragraphDurations.size(); i++) {
                ParagraphDuration pd = paragraphDurations.get(i);
                SegmentResult result = cutSingleSegment(taskId, pd, i, tempDir, downloadedFiles);
                results.add(result);
            }

            log.info("Video segment cutting completed for task {}: {} segments created", taskId, results.size());
            return results;

        } finally {
            // Clean up downloaded source files
            for (File file : downloadedFiles) {
                if (file.exists()) {
                    file.delete();
                }
            }
        }
    }

    /**
     * Cut a single video segment for a paragraph.
     */
    private SegmentResult cutSingleSegment(Long taskId, ParagraphDuration pd, int index,
                                            File tempDir, List<File> downloadedFiles) {
        Long shotId = pd.getShotId();
        if (shotId == null) {
            throw new BusinessException(ResultCode.VIDEO_CUTTING_FAILED,
                    "第" + (index + 1) + "段落缺少视频镜头映射");
        }

        // Query video by shot_id
        VideoReadMapper.VideoInfo video = videoReadMapper.findById(shotId);
        if (video == null) {
            log.error("Video not found for shot_id {}", shotId);
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND,
                    "镜头" + shotId + "对应的视频未找到");
        }

        // Calculate segment duration (TTS duration + transition)
        double segmentDuration = pd.getActualDurationSeconds() + compositionProperties.getTransitionDuration();

        // Download source video from OSS
        File sourceFile = downloadFromOss(video.getOssKey(), tempDir, "source_" + index + ".mp4");
        downloadedFiles.add(sourceFile);

        // Get actual video duration using FFprobe
        double videoDuration = getVideoDuration(sourceFile);

        // Calculate start position (center cut)
        double startPosition = calculateStartPosition(videoDuration, segmentDuration);
        boolean needsLoop = videoDuration < segmentDuration;

        // Output file
        File outputFile = new File(tempDir, "segment_" + index + ".mp4");

        // Execute FFmpeg with retry
        executeFFmpegCut(sourceFile, outputFile, startPosition, segmentDuration, needsLoop);

        return SegmentResult.builder()
                .paragraphIndex(index)
                .segmentFile(outputFile)
                .durationSeconds(segmentDuration)
                .shotId(shotId)
                .build();
    }

    /**
     * Calculate start position for center-based cutting.
     * start = (video_duration - segment_duration) / 2
     */
    double calculateStartPosition(double videoDuration, double segmentDuration) {
        if (videoDuration <= segmentDuration) {
            return 0.0;
        }
        return (videoDuration - segmentDuration) / 2.0;
    }

    /**
     * Download file from OSS to local temp directory.
     */
    private File downloadFromOss(String ossKey, File tempDir, String filename) {
        File localFile = new File(tempDir, filename);
        try (OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), ossKey);
             InputStream is = ossObject.getObjectContent();
             FileOutputStream fos = new FileOutputStream(localFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            log.debug("Downloaded {} to {}", ossKey, localFile.getAbsolutePath());
            return localFile;

        } catch (IOException e) {
            log.error("Failed to download from OSS: {}", ossKey, e);
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE,
                    "视频下载失败: " + e.getMessage());
        }
    }

    /**
     * Get video duration using FFprobe.
     */
    double getVideoDuration(File videoFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    compositionProperties.getFfprobePath(),
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    videoFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.readLine();
            }

            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new BusinessException(ResultCode.VIDEO_CUTTING_FAILED, "FFprobe超时");
            }

            if (output != null && !output.isBlank()) {
                return Double.parseDouble(output.trim());
            }
            throw new BusinessException(ResultCode.VIDEO_CUTTING_FAILED, "无法获取视频时长");

        } catch (IOException | InterruptedException e) {
            log.error("FFprobe failed for {}: {}", videoFile.getName(), e.getMessage());
            throw new BusinessException(ResultCode.VIDEO_CUTTING_FAILED,
                    "视频时长检测失败: " + e.getMessage());
        }
    }

    /**
     * Execute FFmpeg to cut video segment with retry.
     */
    private void executeFFmpegCut(File sourceFile, File outputFile, double startPosition,
                                   double duration, boolean needsLoop) {
        int maxRetries = compositionProperties.getFfmpegMaxRetries();
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                doFFmpegCut(sourceFile, outputFile, startPosition, duration, needsLoop);
                return;
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    log.warn("FFmpeg cut failed (attempt {}/{}), retrying: {}",
                            attempt + 1, maxRetries + 1, e.getMessage());
                }
            }
        }

        log.error("FFmpeg cut failed after {} attempts", maxRetries + 1);
        throw new BusinessException(ResultCode.VIDEO_CUTTING_FAILED,
                "视频裁剪失败: " + (lastException != null ? lastException.getMessage() : "未知错误"));
    }

    /**
     * Execute single FFmpeg cut operation.
     */
    private void doFFmpegCut(File sourceFile, File outputFile, double startPosition,
                              double duration, boolean needsLoop) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(compositionProperties.getFfmpegPath());
        command.add("-y"); // Overwrite output

        if (needsLoop) {
            // Video too short, use stream loop
            command.add("-stream_loop");
            command.add("-1");
        }

        command.add("-i");
        command.add(sourceFile.getAbsolutePath());

        if (!needsLoop && startPosition > 0) {
            command.add("-ss");
            command.add(String.format("%.3f", startPosition));
        }

        command.add("-t");
        command.add(String.format("%.3f", duration));
        command.add("-c");
        command.add("copy");
        command.add(outputFile.getAbsolutePath());

        log.debug("FFmpeg command: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Read output to prevent blocking
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }

        boolean completed = process.waitFor(60, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new IOException("FFmpeg timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("FFmpeg failed with exit code {}: {}", exitCode, errorOutput);
            throw new IOException("FFmpeg exit code: " + exitCode);
        }

        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new IOException("Output file not created or empty");
        }

        log.debug("Segment created: {} ({} bytes)", outputFile.getName(), outputFile.length());
    }

    /**
     * Clean up segment files after composition.
     */
    public void cleanupSegments(List<SegmentResult> segments) {
        for (SegmentResult segment : segments) {
            if (segment.getSegmentFile() != null && segment.getSegmentFile().exists()) {
                segment.getSegmentFile().delete();
            }
        }
    }

    /**
     * Input DTO for paragraph duration from TTS.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParagraphDuration {
        private int paragraphIndex;
        private Long shotId;
        private String text;
        private String audioUrl;
        private double actualDurationSeconds;
    }

    /**
     * Output DTO for cut segment.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentResult {
        private int paragraphIndex;
        private File segmentFile;
        private double durationSeconds;
        private Long shotId;
    }
}
