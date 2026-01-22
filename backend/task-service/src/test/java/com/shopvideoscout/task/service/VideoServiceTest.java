package com.shopvideoscout.task.service;

import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.task.constant.VideoConstants;
import com.shopvideoscout.task.dto.*;
import com.shopvideoscout.task.entity.Task;
import com.shopvideoscout.task.entity.Video;
import com.shopvideoscout.task.mapper.TaskMapper;
import com.shopvideoscout.task.mapper.VideoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VideoService.
 * Tests cover scenarios from QA test design document.
 */
@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock
    private VideoMapper videoMapper;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private OssService ossService;

    @InjectMocks
    private VideoService videoService;

    private Task mockTask;

    @BeforeEach
    void setUp() {
        mockTask = new Task();
        mockTask.setId(1L);
        mockTask.setUserId(100L);
    }

    @Nested
    @DisplayName("AC1: Get Upload URL - File Validation")
    class GetUploadUrlTests {

        @Test
        @DisplayName("2.1-UNIT-001: Valid MP4 filename should return presigned URL")
        void validMp4Filename_ShouldReturnPresignedUrl() {
            // Given
            VideoUploadUrlRequest request = new VideoUploadUrlRequest();
            request.setFilename("test.mp4");
            request.setFileSize(50L * 1024 * 1024); // 50MB

            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(ossService.generatePresignedUploadUrl(100L, 1L, "mp4"))
                    .thenReturn(new OssService.PresignedUrlResult(
                            "https://oss.example.com/upload?signature=xxx",
                            "videos/100/1/uuid.mp4",
                            900));

            // When
            VideoUploadUrlResponse response = videoService.getUploadUrl(1L, 100L, request);

            // Then
            assertNotNull(response);
            assertNotNull(response.getUploadUrl());
            assertNotNull(response.getOssKey());
            assertEquals(900, response.getExpiresIn());
        }

        @Test
        @DisplayName("2.1-UNIT-002: Valid MOV filename should return presigned URL")
        void validMovFilename_ShouldReturnPresignedUrl() {
            // Given
            VideoUploadUrlRequest request = new VideoUploadUrlRequest();
            request.setFilename("video.mov");
            request.setFileSize(50L * 1024 * 1024); // 50MB

            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(ossService.generatePresignedUploadUrl(100L, 1L, "mov"))
                    .thenReturn(new OssService.PresignedUrlResult(
                            "https://oss.example.com/upload?signature=xxx",
                            "videos/100/1/uuid.mov",
                            900));

            // When
            VideoUploadUrlResponse response = videoService.getUploadUrl(1L, 100L, request);

            // Then
            assertNotNull(response);
            assertTrue(response.getOssKey().endsWith(".mov"));
        }

        @Test
        @DisplayName("2.1-UNIT-003: Invalid extension (avi) should return 400 error")
        void invalidExtension_ShouldThrowException() {
            // Given
            VideoUploadUrlRequest request = new VideoUploadUrlRequest();
            request.setFilename("test.avi");
            request.setFileSize(50L * 1024 * 1024);

            when(taskMapper.selectById(1L)).thenReturn(mockTask);

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> videoService.getUploadUrl(1L, 100L, request));
            assertEquals(ResultCode.INVALID_FILE_FORMAT.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("MP4"));
        }

        @Test
        @DisplayName("2.1-UNIT-004: Size at limit (100MB exactly) should return presigned URL")
        void sizeAtLimit_ShouldReturnPresignedUrl() {
            // Given
            VideoUploadUrlRequest request = new VideoUploadUrlRequest();
            request.setFilename("test.mp4");
            request.setFileSize(VideoConstants.MAX_FILE_SIZE); // 100MB exactly

            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(ossService.generatePresignedUploadUrl(anyLong(), anyLong(), anyString()))
                    .thenReturn(new OssService.PresignedUrlResult("url", "key", 900));

            // When
            VideoUploadUrlResponse response = videoService.getUploadUrl(1L, 100L, request);

            // Then
            assertNotNull(response);
        }

        @Test
        @DisplayName("2.1-UNIT-005: Size over limit (100MB + 1 byte) should return 400 error")
        void sizeOverLimit_ShouldThrowException() {
            // Given
            VideoUploadUrlRequest request = new VideoUploadUrlRequest();
            request.setFilename("test.mp4");
            request.setFileSize(VideoConstants.MAX_FILE_SIZE + 1); // 100MB + 1 byte

            when(taskMapper.selectById(1L)).thenReturn(mockTask);

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> videoService.getUploadUrl(1L, 100L, request));
            assertEquals(ResultCode.FILE_SIZE_EXCEEDED.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("100MB"));
        }

        @Test
        @DisplayName("2.1-UNIT-006: Task not found should return 404")
        void taskNotFound_ShouldThrowException() {
            // Given
            VideoUploadUrlRequest request = new VideoUploadUrlRequest();
            request.setFilename("test.mp4");
            request.setFileSize(50L * 1024 * 1024);

            when(taskMapper.selectById(1L)).thenReturn(null);

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> videoService.getUploadUrl(1L, 100L, request));
            assertEquals(ResultCode.TASK_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("2.1-UNIT-007: User not owning task should return 403")
        void userNotOwningTask_ShouldThrowException() {
            // Given
            VideoUploadUrlRequest request = new VideoUploadUrlRequest();
            request.setFilename("test.mp4");
            request.setFileSize(50L * 1024 * 1024);

            when(taskMapper.selectById(1L)).thenReturn(mockTask);

            // When/Then - different user ID
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> videoService.getUploadUrl(1L, 999L, request));
            assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("2.1-BLIND-001: Uppercase extension should be accepted")
        void uppercaseExtension_ShouldBeAccepted() {
            // Given
            VideoUploadUrlRequest request = new VideoUploadUrlRequest();
            request.setFilename("test.MP4");
            request.setFileSize(50L * 1024 * 1024);

            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(ossService.generatePresignedUploadUrl(anyLong(), anyLong(), anyString()))
                    .thenReturn(new OssService.PresignedUrlResult("url", "key", 900));

            // When
            VideoUploadUrlResponse response = videoService.getUploadUrl(1L, 100L, request);

            // Then
            assertNotNull(response);
        }

        @Test
        @DisplayName("2.1-BLIND-002: Mixed case extension should be accepted")
        void mixedCaseExtension_ShouldBeAccepted() {
            // Given
            VideoUploadUrlRequest request = new VideoUploadUrlRequest();
            request.setFilename("test.Mp4");
            request.setFileSize(50L * 1024 * 1024);

            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(ossService.generatePresignedUploadUrl(anyLong(), anyLong(), anyString()))
                    .thenReturn(new OssService.PresignedUrlResult("url", "key", 900));

            // When
            VideoUploadUrlResponse response = videoService.getUploadUrl(1L, 100L, request);

            // Then
            assertNotNull(response);
        }

        @Test
        @DisplayName("2.1-BLIND-003: Filename without extension should be rejected")
        void filenameWithoutExtension_ShouldThrowException() {
            // Given
            VideoUploadUrlRequest request = new VideoUploadUrlRequest();
            request.setFilename("testfile");
            request.setFileSize(50L * 1024 * 1024);

            when(taskMapper.selectById(1L)).thenReturn(mockTask);

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> videoService.getUploadUrl(1L, 100L, request));
            assertEquals(ResultCode.INVALID_FILE_FORMAT.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("AC2: Confirm Upload - Video Record Creation")
    class ConfirmUploadTests {

        @Test
        @DisplayName("2.1-INT-001: Valid upload confirmation should create video record")
        void validUploadConfirmation_ShouldCreateRecord() {
            // Given
            ConfirmVideoUploadRequest request = new ConfirmVideoUploadRequest();
            request.setOssKey("videos/100/1/uuid.mp4");
            request.setOriginalFilename("myvideo.mp4");

            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(videoMapper.countByTaskId(1L)).thenReturn(0);
            when(ossService.objectExists("videos/100/1/uuid.mp4")).thenReturn(true);
            when(ossService.getCdnBaseUrl()).thenReturn("https://cdn.example.com");

            // When
            VideoResponse response = videoService.confirmUpload(1L, 100L, request);

            // Then
            assertNotNull(response);
            assertEquals("myvideo.mp4", response.getOriginalFilename());
            verify(videoMapper).insert(any(Video.class));
        }

        @Test
        @DisplayName("2.1-UNIT-008: Video count at limit (20) should accept")
        void videoCountAtLimit_ShouldAccept() {
            // Given
            ConfirmVideoUploadRequest request = new ConfirmVideoUploadRequest();
            request.setOssKey("videos/100/1/uuid.mp4");
            request.setOriginalFilename("myvideo.mp4");

            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(videoMapper.countByTaskId(1L)).thenReturn(19); // 19 existing, adding 20th
            when(ossService.objectExists(anyString())).thenReturn(true);
            when(ossService.getCdnBaseUrl()).thenReturn("https://cdn.example.com");

            // When
            VideoResponse response = videoService.confirmUpload(1L, 100L, request);

            // Then
            assertNotNull(response);
            verify(videoMapper).insert(any(Video.class));
        }

        @Test
        @DisplayName("2.1-UNIT-009: Video count over limit (21st) should return 400")
        void videoCountOverLimit_ShouldThrowException() {
            // Given
            ConfirmVideoUploadRequest request = new ConfirmVideoUploadRequest();
            request.setOssKey("videos/100/1/uuid.mp4");
            request.setOriginalFilename("myvideo.mp4");

            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(videoMapper.countByTaskId(1L)).thenReturn(20); // Already at limit

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> videoService.confirmUpload(1L, 100L, request));
            assertEquals(ResultCode.VIDEO_COUNT_EXCEEDED.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("20"));
        }

        @Test
        @DisplayName("2.1-INT-002: File not found in OSS should return 404")
        void fileNotFoundInOss_ShouldThrowException() {
            // Given
            ConfirmVideoUploadRequest request = new ConfirmVideoUploadRequest();
            request.setOssKey("videos/100/1/nonexistent.mp4");
            request.setOriginalFilename("myvideo.mp4");

            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(videoMapper.countByTaskId(1L)).thenReturn(0);
            when(ossService.objectExists("videos/100/1/nonexistent.mp4")).thenReturn(false);

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> videoService.confirmUpload(1L, 100L, request));
            assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("Video CRUD Operations")
    class VideoCrudTests {

        @Test
        @DisplayName("2.1-INT-003: Get videos should return list")
        void getVideos_ShouldReturnList() {
            // Given
            Video video1 = new Video();
            video1.setId(1L);
            video1.setTaskId(1L);
            video1.setOriginalFilename("video1.mp4");

            Video video2 = new Video();
            video2.setId(2L);
            video2.setTaskId(1L);
            video2.setOriginalFilename("video2.mp4");

            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(videoMapper.findByTaskId(1L)).thenReturn(Arrays.asList(video1, video2));
            when(ossService.getCdnBaseUrl()).thenReturn("https://cdn.example.com");

            // When
            List<VideoResponse> videos = videoService.getVideos(1L, 100L);

            // Then
            assertEquals(2, videos.size());
            assertEquals("video1.mp4", videos.get(0).getOriginalFilename());
        }

        @Test
        @DisplayName("2.1-INT-004: Delete video should remove from DB and OSS")
        void deleteVideo_ShouldRemoveFromDbAndOss() {
            // Given
            Video video = new Video();
            video.setId(1L);
            video.setTaskId(1L);
            video.setOssKey("videos/100/1/uuid.mp4");
            video.setThumbnailOssKey("thumbnails/100/1/uuid.jpg");

            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(videoMapper.findByIdAndTaskId(1L, 1L)).thenReturn(video);

            // When
            videoService.deleteVideo(1L, 1L, 100L);

            // Then
            verify(ossService).deleteObject("videos/100/1/uuid.mp4");
            verify(ossService).deleteObject("thumbnails/100/1/uuid.jpg");
            verify(videoMapper).deleteById(1L);
        }

        @Test
        @DisplayName("2.1-INT-005: Delete non-existent video should return 404")
        void deleteNonExistentVideo_ShouldThrowException() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(videoMapper.findByIdAndTaskId(999L, 1L)).thenReturn(null);

            // When/Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> videoService.deleteVideo(1L, 999L, 100L));
            assertEquals(ResultCode.VIDEO_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("2.1-BLIND-004: Delete video with null OSS key should not fail")
        void deleteVideoWithNullOssKey_ShouldNotFail() {
            // Given
            Video video = new Video();
            video.setId(1L);
            video.setTaskId(1L);
            video.setOssKey(null);
            video.setThumbnailOssKey(null);

            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(videoMapper.findByIdAndTaskId(1L, 1L)).thenReturn(video);

            // When/Then - should not throw
            assertDoesNotThrow(() -> videoService.deleteVideo(1L, 1L, 100L));
            verify(ossService, never()).deleteObject(anyString());
            verify(videoMapper).deleteById(1L);
        }
    }
}
