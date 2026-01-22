package com.shopvideoscout.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.task.dto.*;
import com.shopvideoscout.task.service.VideoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for VideoController.
 * Tests cover HTTP request/response handling and validation.
 */
@WebMvcTest(VideoController.class)
class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VideoService videoService;

    private static final String BASE_URL = "/api/v1/tasks/1/videos";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Nested
    @DisplayName("POST /upload-url - Get Presigned Upload URL")
    class GetUploadUrlTests {

        @Test
        @DisplayName("2.1-INT-008: Valid request should return 200 with presigned URL")
        void validRequest_ShouldReturnPresignedUrl() throws Exception {
            // Given
            VideoUploadUrlRequest request = new VideoUploadUrlRequest();
            request.setFilename("test.mp4");
            request.setFileSize(50L * 1024 * 1024);

            VideoUploadUrlResponse response = VideoUploadUrlResponse.builder()
                    .uploadUrl("https://oss.example.com/upload?signature=xxx")
                    .ossKey("videos/100/1/uuid.mp4")
                    .expiresIn(900)
                    .build();

            when(videoService.getUploadUrl(eq(1L), eq(100L), any(VideoUploadUrlRequest.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(post(BASE_URL + "/upload-url")
                            .header(USER_ID_HEADER, "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.uploadUrl").value("https://oss.example.com/upload?signature=xxx"))
                    .andExpect(jsonPath("$.data.ossKey").value("videos/100/1/uuid.mp4"))
                    .andExpect(jsonPath("$.data.expiresIn").value(900));
        }

        @Test
        @DisplayName("2.1-INT-009: Missing X-User-Id header should return 400")
        void missingUserIdHeader_ShouldReturn400() throws Exception {
            // Given
            VideoUploadUrlRequest request = new VideoUploadUrlRequest();
            request.setFilename("test.mp4");
            request.setFileSize(50L * 1024 * 1024);

            // When/Then
            mockMvc.perform(post(BASE_URL + "/upload-url")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("2.1-INT-010: Invalid file format should return 400")
        void invalidFileFormat_ShouldReturn400() throws Exception {
            // Given
            VideoUploadUrlRequest request = new VideoUploadUrlRequest();
            request.setFilename("test.avi");
            request.setFileSize(50L * 1024 * 1024);

            when(videoService.getUploadUrl(anyLong(), anyLong(), any(VideoUploadUrlRequest.class)))
                    .thenThrow(new BusinessException(ResultCode.INVALID_FILE_FORMAT, "仅支持MP4和MOV格式视频"));

            // When/Then
            mockMvc.perform(post(BASE_URL + "/upload-url")
                            .header(USER_ID_HEADER, "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ResultCode.INVALID_FILE_FORMAT.getCode()));
        }

        @Test
        @DisplayName("2.1-INT-011: File size exceeded should return 400")
        void fileSizeExceeded_ShouldReturn400() throws Exception {
            // Given
            VideoUploadUrlRequest request = new VideoUploadUrlRequest();
            request.setFilename("test.mp4");
            request.setFileSize(200L * 1024 * 1024); // 200MB

            when(videoService.getUploadUrl(anyLong(), anyLong(), any(VideoUploadUrlRequest.class)))
                    .thenThrow(new BusinessException(ResultCode.FILE_SIZE_EXCEEDED, "单个视频不能超过100MB"));

            // When/Then
            mockMvc.perform(post(BASE_URL + "/upload-url")
                            .header(USER_ID_HEADER, "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ResultCode.FILE_SIZE_EXCEEDED.getCode()));
        }

        @Test
        @DisplayName("2.1-VALID-001: Missing filename should return validation error")
        void missingFilename_ShouldReturnValidationError() throws Exception {
            // Given
            VideoUploadUrlRequest request = new VideoUploadUrlRequest();
            request.setFilename(null);
            request.setFileSize(50L * 1024 * 1024);

            // When/Then
            mockMvc.perform(post(BASE_URL + "/upload-url")
                            .header(USER_ID_HEADER, "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("2.1-VALID-002: Missing file size should return validation error")
        void missingFileSize_ShouldReturnValidationError() throws Exception {
            // Given
            VideoUploadUrlRequest request = new VideoUploadUrlRequest();
            request.setFilename("test.mp4");
            request.setFileSize(null);

            // When/Then
            mockMvc.perform(post(BASE_URL + "/upload-url")
                            .header(USER_ID_HEADER, "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST / - Confirm Video Upload")
    class ConfirmUploadTests {

        @Test
        @DisplayName("2.1-INT-012: Valid confirmation should return 201")
        void validConfirmation_ShouldReturn201() throws Exception {
            // Given
            ConfirmVideoUploadRequest request = new ConfirmVideoUploadRequest();
            request.setOssKey("videos/100/1/uuid.mp4");
            request.setOriginalFilename("myvideo.mp4");

            VideoResponse response = VideoResponse.builder()
                    .id(1L)
                    .taskId(1L)
                    .originalFilename("myvideo.mp4")
                    .status("uploaded")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(videoService.confirmUpload(eq(1L), eq(100L), any(ConfirmVideoUploadRequest.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(post(BASE_URL)
                            .header(USER_ID_HEADER, "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.originalFilename").value("myvideo.mp4"));
        }

        @Test
        @DisplayName("2.1-INT-013: Video count exceeded should return 400")
        void videoCountExceeded_ShouldReturn400() throws Exception {
            // Given
            ConfirmVideoUploadRequest request = new ConfirmVideoUploadRequest();
            request.setOssKey("videos/100/1/uuid.mp4");
            request.setOriginalFilename("myvideo.mp4");

            when(videoService.confirmUpload(anyLong(), anyLong(), any(ConfirmVideoUploadRequest.class)))
                    .thenThrow(new BusinessException(ResultCode.VIDEO_COUNT_EXCEEDED, "每个任务最多上传20个视频"));

            // When/Then
            mockMvc.perform(post(BASE_URL)
                            .header(USER_ID_HEADER, "100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ResultCode.VIDEO_COUNT_EXCEEDED.getCode()));
        }
    }

    @Nested
    @DisplayName("GET / - List Videos")
    class GetVideosTests {

        @Test
        @DisplayName("2.1-INT-014: Get videos should return list")
        void getVideos_ShouldReturnList() throws Exception {
            // Given
            List<VideoResponse> videos = Arrays.asList(
                    VideoResponse.builder()
                            .id(1L)
                            .taskId(1L)
                            .originalFilename("video1.mp4")
                            .status("uploaded")
                            .build(),
                    VideoResponse.builder()
                            .id(2L)
                            .taskId(1L)
                            .originalFilename("video2.mp4")
                            .status("analyzed")
                            .build()
            );

            when(videoService.getVideos(1L, 100L)).thenReturn(videos);

            // When/Then
            mockMvc.perform(get(BASE_URL)
                            .header(USER_ID_HEADER, "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].originalFilename").value("video1.mp4"));
        }

        @Test
        @DisplayName("2.1-INT-015: Unauthorized access should return 403")
        void unauthorizedAccess_ShouldReturn403() throws Exception {
            // Given
            when(videoService.getVideos(1L, 999L))
                    .thenThrow(new BusinessException(ResultCode.FORBIDDEN, "无权访问此任务"));

            // When/Then
            mockMvc.perform(get(BASE_URL)
                            .header(USER_ID_HEADER, "999"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ResultCode.FORBIDDEN.getCode()));
        }
    }

    @Nested
    @DisplayName("DELETE /{videoId} - Delete Video")
    class DeleteVideoTests {

        @Test
        @DisplayName("2.1-INT-016: Delete video should return 200")
        void deleteVideo_ShouldReturn200() throws Exception {
            // Given
            doNothing().when(videoService).deleteVideo(1L, 1L, 100L);

            // When/Then
            mockMvc.perform(delete(BASE_URL + "/1")
                            .header(USER_ID_HEADER, "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("2.1-INT-017: Delete non-existent video should return 404")
        void deleteNonExistentVideo_ShouldReturn404() throws Exception {
            // Given
            doThrow(new BusinessException(ResultCode.VIDEO_NOT_FOUND, "视频不存在"))
                    .when(videoService).deleteVideo(1L, 999L, 100L);

            // When/Then
            mockMvc.perform(delete(BASE_URL + "/999")
                            .header(USER_ID_HEADER, "100"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ResultCode.VIDEO_NOT_FOUND.getCode()));
        }
    }
}
