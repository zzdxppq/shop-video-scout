package com.shopvideoscout.task.controller;

import com.shopvideoscout.common.result.R;
import com.shopvideoscout.task.dto.*;
import com.shopvideoscout.task.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Video controller handling video upload and management operations.
 * All endpoints require JWT authentication (validated by Gateway).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tasks/{taskId}/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /**
     * Get presigned upload URL for video file.
     * POST /api/v1/tasks/{taskId}/videos/upload-url
     *
     * Validates:
     * - File format: mp4 or mov only (BR-1.2)
     * - File size: max 100MB (BR-1.3)
     *
     * @param taskId  task ID
     * @param request upload URL request containing filename and file_size
     * @param userId  injected from JWT via Gateway header
     * @return presigned upload URL with 15min expiry
     */
    @PostMapping("/upload-url")
    public R<VideoUploadUrlResponse> getUploadUrl(
            @PathVariable Long taskId,
            @Valid @RequestBody VideoUploadUrlRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.debug("Get upload URL for task {} user {}: {}", taskId, userId, request.getFilename());
        VideoUploadUrlResponse response = videoService.getUploadUrl(taskId, userId, request);
        return R.ok(response);
    }

    /**
     * Confirm video upload completion.
     * POST /api/v1/tasks/{taskId}/videos
     *
     * Creates video record after file upload to OSS.
     * Validates video count limit (BR-2.4: max 20 per task).
     *
     * @param taskId  task ID
     * @param request confirm upload request containing oss_key and original_filename
     * @param userId  injected from JWT via Gateway header
     * @return created video with 201 status
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public R<VideoResponse> confirmUpload(
            @PathVariable Long taskId,
            @Valid @RequestBody ConfirmVideoUploadRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.debug("Confirm upload for task {} user {}: {}", taskId, userId, request.getOssKey());
        VideoResponse response = videoService.confirmUpload(taskId, userId, request);
        return R.ok(response);
    }

    /**
     * Get all videos for a task.
     * GET /api/v1/tasks/{taskId}/videos
     *
     * @param taskId task ID
     * @param userId injected from JWT via Gateway header
     * @return list of videos
     */
    @GetMapping
    public R<List<VideoResponse>> getVideos(
            @PathVariable Long taskId,
            @RequestHeader("X-User-Id") Long userId) {
        log.debug("Get videos for task {} user {}", taskId, userId);
        List<VideoResponse> videos = videoService.getVideos(taskId, userId);
        return R.ok(videos);
    }

    /**
     * Delete a video.
     * DELETE /api/v1/tasks/{taskId}/videos/{videoId}
     *
     * Deletes video from both database and OSS.
     *
     * @param taskId  task ID
     * @param videoId video ID
     * @param userId  injected from JWT via Gateway header
     * @return success response
     */
    @DeleteMapping("/{videoId}")
    public R<Void> deleteVideo(
            @PathVariable Long taskId,
            @PathVariable Long videoId,
            @RequestHeader("X-User-Id") Long userId) {
        log.debug("Delete video {} from task {} user {}", videoId, taskId, userId);
        videoService.deleteVideo(taskId, videoId, userId);
        return R.ok();
    }
}
