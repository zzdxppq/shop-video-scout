package com.shopvideoscout.media.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.media.config.CompositionProperties;
import com.shopvideoscout.media.config.OssConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for composing final video from segments and audio (Story 4.3).
 * Uses FFmpeg to concat segments, merge audio, and burn subtitles.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoCompositionService {

    private final OSS ossClient;
    private final OssConfig ossConfig;
    private final CompositionProperties compositionProperties;

    /**
     * Compose final video from segments, audio, and optional subtitles.
     *
     * @param taskId        task ID
     * @param segments      video segment files
     * @param audioUrls     audio file OSS URLs
     * @param subtitleFile  optional subtitle file (ASS format)
     * @return composition result with output file
     */
    public CompositionResult compose(Long taskId,
                                      List<VideoSegmentCuttingService.SegmentResult> segments,
                                      List<String> audioUrls,
                                      File subtitleFile) {
        log.info("Starting video composition for task {}: {} segments, {} audio files",
                taskId, segments.size(), audioUrls.size());

        File tempDir = new File(compositionProperties.getTempDir(), taskId.toString());
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "Failed to create temp directory");
        }

        List<File> tempFiles = new ArrayList<>();

        try {
            // Step 1: Generate segments.txt for FFmpeg concat
            File segmentsListFile = generateSegmentsList(segments, tempDir);
            tempFiles.add(segmentsListFile);

            // Step 2: Concatenate video segments
            File concatenatedVideo = new File(tempDir, "concat.mp4");
            tempFiles.add(concatenatedVideo);
            concatenateSegments(segmentsListFile, concatenatedVideo);

            // Step 3: Download and merge audio files
            File mergedAudio = downloadAndMergeAudio(audioUrls, tempDir);
            tempFiles.add(mergedAudio);

            // Step 4: Compose final video (video + audio + optional subtitles)
            File outputFile = new File(tempDir, "final.mp4");
            composeWithAudioAndSubtitles(concatenatedVideo, mergedAudio, subtitleFile, outputFile);

            // Get file size
            long fileSize = outputFile.length();
            double duration = getVideoDuration(outputFile);

            log.info("Video composition completed for task {}: size={}, duration={}s",
                    taskId, fileSize, duration);

            return CompositionResult.builder()
                    .outputFile(outputFile)
                    .durationSeconds(duration)
                    .fileSizeBytes(fileSize)
                    .build();

        } catch (Exception e) {
            log.error("Video composition failed for task {}: {}", taskId, e.getMessage());
            throw new BusinessException(ResultCode.COMPOSITION_FAILED,
                    "视频合成失败: " + e.getMessage());
        } finally {
            // Cleanup intermediate files (keep output)
            for (File file : tempFiles) {
                if (file != null && file.exists()) {
                    file.delete();
                }
            }
        }
    }

    /**
     * Generate segments.txt file for FFmpeg concat demuxer.
     * Format: file 'path/to/segment.mp4'
     */
    File generateSegmentsList(List<VideoSegmentCuttingService.SegmentResult> segments, File tempDir)
            throws IOException {
        File listFile = new File(tempDir, "segments.txt");
        try (PrintWriter writer = new PrintWriter(listFile)) {
            for (VideoSegmentCuttingService.SegmentResult segment : segments) {
                // Use absolute paths and escape quotes
                String path = segment.getSegmentFile().getAbsolutePath().replace("'", "'\\''");
                writer.println("file '" + path + "'");
            }
        }
        log.debug("Generated segments.txt with {} entries", segments.size());
        return listFile;
    }

    /**
     * Concatenate video segments using FFmpeg concat demuxer.
     */
    private void concatenateSegments(File segmentsListFile, File outputFile) throws IOException, InterruptedException {
        List<String> command = List.of(
                compositionProperties.getFfmpegPath(),
                "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", segmentsListFile.getAbsolutePath(),
                "-c", "copy",
                outputFile.getAbsolutePath()
        );

        executeFFmpeg(command, "Segment concatenation");
    }

    /**
     * Download audio files from OSS and merge into single track.
     */
    private File downloadAndMergeAudio(List<String> audioUrls, File tempDir) throws IOException, InterruptedException {
        List<File> audioFiles = new ArrayList<>();

        // Download each audio file
        for (int i = 0; i < audioUrls.size(); i++) {
            String url = audioUrls.get(i);
            String ossKey = extractOssKey(url);
            File audioFile = downloadFromOss(ossKey, tempDir, "audio_" + i + ".mp3");
            audioFiles.add(audioFile);
        }

        if (audioFiles.size() == 1) {
            return audioFiles.get(0);
        }

        // Generate audio list file
        File audioListFile = new File(tempDir, "audio_list.txt");
        try (PrintWriter writer = new PrintWriter(audioListFile)) {
            for (File audioFile : audioFiles) {
                String path = audioFile.getAbsolutePath().replace("'", "'\\''");
                writer.println("file '" + path + "'");
            }
        }

        // Merge audio files
        File mergedAudio = new File(tempDir, "merged_audio.mp3");
        List<String> command = List.of(
                compositionProperties.getFfmpegPath(),
                "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", audioListFile.getAbsolutePath(),
                "-c", "copy",
                mergedAudio.getAbsolutePath()
        );

        executeFFmpeg(command, "Audio merge");

        // Cleanup individual audio files
        for (File audioFile : audioFiles) {
            audioFile.delete();
        }
        audioListFile.delete();

        return mergedAudio;
    }

    /**
     * Compose final video with audio overlay and optional subtitle burn-in.
     */
    private void composeWithAudioAndSubtitles(File videoFile, File audioFile,
                                               File subtitleFile, File outputFile)
            throws IOException, InterruptedException {

        List<String> command = new ArrayList<>();
        command.add(compositionProperties.getFfmpegPath());
        command.add("-y");
        command.add("-i");
        command.add(videoFile.getAbsolutePath());
        command.add("-i");
        command.add(audioFile.getAbsolutePath());

        // Build filter complex
        StringBuilder filterComplex = new StringBuilder();

        // Scale and pad for 1080x1920 portrait output
        filterComplex.append("[0:v]scale=")
                .append(compositionProperties.getOutputWidth())
                .append(":")
                .append(compositionProperties.getOutputHeight())
                .append(":force_original_aspect_ratio=decrease,")
                .append("pad=")
                .append(compositionProperties.getOutputWidth())
                .append(":")
                .append(compositionProperties.getOutputHeight())
                .append(":(ow-iw)/2:(oh-ih)/2");

        // Add subtitle burn-in if provided
        if (subtitleFile != null && subtitleFile.exists()) {
            // Escape path for filter
            String subtitlePath = subtitleFile.getAbsolutePath()
                    .replace("\\", "/")
                    .replace(":", "\\:");
            filterComplex.append(",subtitles='").append(subtitlePath).append("'");
        }

        filterComplex.append("[v]");

        command.add("-filter_complex");
        command.add(filterComplex.toString());
        command.add("-map");
        command.add("[v]");
        command.add("-map");
        command.add("1:a");

        // Video encoding settings
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add(compositionProperties.getEncodingPreset());
        command.add("-b:v");
        command.add(compositionProperties.getVideoBitrate());
        command.add("-r");
        command.add(String.valueOf(compositionProperties.getFrameRate()));

        // Audio encoding settings
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add(compositionProperties.getAudioBitrate());

        // Enable fast start for web playback
        command.add("-movflags");
        command.add("+faststart");

        command.add(outputFile.getAbsolutePath());

        executeFFmpeg(command, "Final composition");
    }

    /**
     * Upload output video to OSS with retry.
     *
     * @param outputFile local output file
     * @param taskId     task ID
     * @return OSS key
     */
    public String uploadToOss(File outputFile, Long taskId) {
        String ossKey = "output/" + taskId + "/final.mp4";
        int maxRetries = compositionProperties.getOssUploadMaxRetries();
        long retryIntervalMs = compositionProperties.getOssUploadRetryIntervalMs();

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                try (InputStream is = new FileInputStream(outputFile)) {
                    ossClient.putObject(ossConfig.getBucketName(), ossKey, is);
                }
                log.info("Uploaded output video to OSS: {}", ossKey);
                return ossKey;

            } catch (Exception e) {
                if (attempt < maxRetries) {
                    log.warn("OSS upload failed (attempt {}/{}), retrying in {}ms: {}",
                            attempt + 1, maxRetries + 1, retryIntervalMs, e.getMessage());
                    try {
                        Thread.sleep(retryIntervalMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "Upload interrupted");
                    }
                } else {
                    log.error("OSS upload failed after {} attempts", maxRetries + 1);
                    throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE,
                            "视频上传失败: " + e.getMessage());
                }
            }
        }

        throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "视频上传失败");
    }

    /**
     * Execute FFmpeg command with timeout.
     */
    private void executeFFmpeg(List<String> command, String operation)
            throws IOException, InterruptedException {
        log.debug("FFmpeg {}: {}", operation, String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean completed = process.waitFor(300, TimeUnit.SECONDS); // 5 minutes timeout
        if (!completed) {
            process.destroyForcibly();
            throw new IOException(operation + " timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("{} failed with exit code {}: {}", operation, exitCode, output);
            throw new IOException(operation + " failed: exit code " + exitCode);
        }
    }

    /**
     * Get video duration using FFprobe.
     */
    private double getVideoDuration(File videoFile) throws IOException, InterruptedException {
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

        process.waitFor(30, TimeUnit.SECONDS);

        if (output != null && !output.isBlank()) {
            return Double.parseDouble(output.trim());
        }
        return 0.0;
    }

    /**
     * Download file from OSS.
     */
    private File downloadFromOss(String ossKey, File tempDir, String filename) throws IOException {
        File localFile = new File(tempDir, filename);
        try (OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), ossKey);
             InputStream is = ossObject.getObjectContent();
             FileOutputStream fos = new FileOutputStream(localFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            return localFile;
        }
    }

    /**
     * Extract OSS key from full URL.
     */
    private String extractOssKey(String url) {
        // URL format: https://{bucket}.{endpoint}/{key}
        int protocolEnd = url.indexOf("://");
        if (protocolEnd > 0) {
            int pathStart = url.indexOf("/", protocolEnd + 3);
            if (pathStart > 0) {
                return url.substring(pathStart + 1);
            }
        }
        return url;
    }

    /**
     * Clean up all temp files for a task.
     */
    public void cleanup(Long taskId) {
        File tempDir = new File(compositionProperties.getTempDir(), taskId.toString());
        if (tempDir.exists()) {
            deleteDirectory(tempDir);
            log.debug("Cleaned up temp directory for task {}", taskId);
        }
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    /**
     * Composition result.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositionResult {
        private File outputFile;
        private double durationSeconds;
        private long fileSizeBytes;
    }
}
