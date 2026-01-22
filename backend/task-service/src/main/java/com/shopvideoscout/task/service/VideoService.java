package com.shopvideoscout.task.service;

import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.task.constant.VideoConstants;
import com.shopvideoscout.task.dto.*;
import com.shopvideoscout.task.entity.Task;
import com.shopvideoscout.task.entity.Video;
import com.shopvideoscout.task.mapper.TaskMapper;
import com.shopvideoscout.task.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Service for video upload and management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoMapper videoMapper;
    private final TaskMapper taskMapper;
    private final OssService ossService;

    /**
     * Generate presigned upload URL for video file.
     * Validates file format and size before generating URL.
     *
     * @param taskId  task ID
     * @param userId  user ID (for path generation and ownership check)
     * @param request upload URL request
     * @return presigned URL response
     */
    public VideoUploadUrlResponse getUploadUrl(Long taskId, Long userId, VideoUploadUrlRequest request) {
        // Validate task ownership
        Task task = validateTaskOwnership(taskId, userId);

        // Validate file extension (BR-1.2)
        String extension = extractExtension(request.getFilename());
        if (!isValidExtension(extension)) {
            throw new BusinessException(ResultCode.INVALID_FILE_FORMAT, "仅支持MP4和MOV格式视频");
        }

        // Validate file size (BR-1.3)
        if (request.getFileSize() > VideoConstants.MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEEDED, "单个视频不能超过100MB");
        }

        // Generate presigned URL
        OssService.PresignedUrlResult result = ossService.generatePresignedUploadUrl(
                userId, taskId, extension);

        log.info("Generated upload URL for task {} user {}: {}", taskId, userId, result.ossKey());

        return VideoUploadUrlResponse.builder()
                .uploadUrl(result.uploadUrl())
                .ossKey(result.ossKey())
                .expiresIn(result.expiresIn())
                .build();
    }

    /**
     * Confirm video upload and create video record.
     * Validates video count limit and initiates processing.
     *
     * @param taskId  task ID
     * @param userId  user ID
     * @param request confirm upload request
     * @return video response
     */
    @Transactional
    public VideoResponse confirmUpload(Long taskId, Long userId, ConfirmVideoUploadRequest request) {
        // Validate task ownership
        Task task = validateTaskOwnership(taskId, userId);

        // Check video count limit (BR-2.4)
        int currentCount = videoMapper.countByTaskId(taskId);
        if (currentCount >= VideoConstants.MAX_VIDEOS_PER_TASK) {
            throw new BusinessException(ResultCode.VIDEO_COUNT_EXCEEDED,
                    String.format("每个任务最多上传%d个视频，当前已有%d个",
                            VideoConstants.MAX_VIDEOS_PER_TASK, currentCount));
        }

        // Verify file exists in OSS
        if (!ossService.objectExists(request.getOssKey())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件未上传成功，请重试");
        }

        // Create video record
        Video video = Video.createNew(taskId, request.getOssKey(),
                request.getOriginalFilename(), null);
        video.setSortOrder(currentCount + 1);
        video.setStatus(VideoConstants.VideoStatus.UPLOADED);

        videoMapper.insert(video);

        log.info("Video {} confirmed for task {}", video.getId(), taskId);

        // DEFERRED TO STORY 2.3 (AI Analysis Service):
        // - FFmpeg processing: keyframe extraction (BR-2.1), thumbnail generation (BR-2.2)
        // - Video metadata extraction: duration, resolution
        // - BR-2.3 Duration validation (max 3 minutes): Requires FFmpeg metadata extraction
        //   to determine video duration. Will be validated during async processing in Story 2.3.
        //   Videos exceeding 3 minutes will be marked as FAILED with VIDEO_DURATION_EXCEEDED error.

        return VideoResponse.fromEntity(video, ossService.getCdnBaseUrl());
    }

    /**
     * Get all videos for a task.
     *
     * @param taskId task ID
     * @param userId user ID
     * @return list of video responses
     */
    public List<VideoResponse> getVideos(Long taskId, Long userId) {
        // Validate task ownership
        validateTaskOwnership(taskId, userId);

        List<Video> videos = videoMapper.findByTaskId(taskId);
        String cdnBaseUrl = ossService.getCdnBaseUrl();

        return videos.stream()
                .map(v -> VideoResponse.fromEntity(v, cdnBaseUrl))
                .toList();
    }

    /**
     * Delete a video.
     *
     * @param taskId  task ID
     * @param videoId video ID
     * @param userId  user ID
     */
    @Transactional
    public void deleteVideo(Long taskId, Long videoId, Long userId) {
        // Validate task ownership
        validateTaskOwnership(taskId, userId);

        // Find video
        Video video = videoMapper.findByIdAndTaskId(videoId, taskId);
        if (video == null) {
            throw new BusinessException(ResultCode.VIDEO_NOT_FOUND, "视频不存在");
        }

        // Delete from OSS
        if (video.getOssKey() != null) {
            try {
                ossService.deleteObject(video.getOssKey());
            } catch (Exception e) {
                log.warn("Failed to delete video from OSS: {}", video.getOssKey(), e);
            }
        }

        // Delete thumbnail from OSS
        if (video.getThumbnailOssKey() != null) {
            try {
                ossService.deleteObject(video.getThumbnailOssKey());
            } catch (Exception e) {
                log.warn("Failed to delete thumbnail from OSS: {}", video.getThumbnailOssKey(), e);
            }
        }

        // Delete from database
        videoMapper.deleteById(videoId);

        log.info("Deleted video {} from task {}", videoId, taskId);
    }

    /**
     * Validate that the task exists and belongs to the user.
     */
    private Task validateTaskOwnership(Long taskId, Long userId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND, "任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问此任务");
        }
        return task;
    }

    /**
     * Extract file extension from filename.
     */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Check if extension is valid (mp4 or mov).
     */
    private boolean isValidExtension(String extension) {
        return Arrays.stream(VideoConstants.SUPPORTED_EXTENSIONS)
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));
    }
}
