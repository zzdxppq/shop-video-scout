package com.shopvideoscout.task.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.task.config.OssConfig;
import com.shopvideoscout.task.constant.TaskConstants;
import com.shopvideoscout.task.dto.DownloadUrlResponse;
import com.shopvideoscout.task.entity.Task;
import com.shopvideoscout.task.mapper.TaskMapper;
import com.shopvideoscout.task.service.impl.DownloadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DownloadService (Story 5.2).
 */
@ExtendWith(MockitoExtension.class)
class DownloadServiceTest {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private OSS ossClient;

    @Mock
    private OssConfig ossConfig;

    @InjectMocks
    private DownloadServiceImpl downloadService;

    @Captor
    private ArgumentCaptor<GeneratePresignedUrlRequest> urlRequestCaptor;

    private Task task;

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(1L);
        task.setUserId(100L);
        task.setShopName("海底捞望京店");
        task.setStatus(TaskConstants.TaskStatus.COMPLETED);
        task.setOutputOssKey("outputs/1/video.mp4");
        task.setOutputFileSize(47841280L);
    }

    @Nested
    @DisplayName("generateVideoDownloadUrl")
    class GenerateVideoDownloadUrlTests {

        @Test
        @DisplayName("should generate signed URL successfully")
        void shouldGenerateSignedUrl() throws Exception {
            when(taskMapper.selectById(1L)).thenReturn(task);
            when(ossConfig.getBucketName()).thenReturn("test-bucket");
            when(ossClient.generatePresignedUrl(any())).thenReturn(
                    new URL("https://oss.example.com/signed?token=xxx"));

            DownloadUrlResponse response = downloadService.generateVideoDownloadUrl(1L, 100L);

            assertThat(response.getDownloadUrl()).isEqualTo("https://oss.example.com/signed?token=xxx");
            assertThat(response.getFilename()).matches("海底捞望京店-探店视频-\\d{8}\\.mp4");
            assertThat(response.getFileSize()).isEqualTo(47841280L);
            assertThat(response.getExpiresAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw TASK_NOT_FOUND when task does not exist")
        void shouldThrowWhenTaskNotFound() {
            when(taskMapper.selectById(1L)).thenReturn(null);

            assertThatThrownBy(() -> downloadService.generateVideoDownloadUrl(1L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ResultCode.TASK_NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("should throw FORBIDDEN when user is not owner")
        void shouldThrowWhenNotOwner() {
            when(taskMapper.selectById(1L)).thenReturn(task);

            assertThatThrownBy(() -> downloadService.generateVideoDownloadUrl(1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ResultCode.FORBIDDEN.getCode());
        }

        @Test
        @DisplayName("should throw OUTPUT_NOT_READY when task not completed")
        void shouldThrowWhenTaskNotCompleted() {
            task.setStatus(TaskConstants.TaskStatus.COMPOSING);
            when(taskMapper.selectById(1L)).thenReturn(task);

            assertThatThrownBy(() -> downloadService.generateVideoDownloadUrl(1L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ResultCode.OUTPUT_NOT_READY.getCode());
        }

        @Test
        @DisplayName("should throw OUTPUT_NOT_READY when output key is null")
        void shouldThrowWhenOutputKeyNull() {
            task.setOutputOssKey(null);
            when(taskMapper.selectById(1L)).thenReturn(task);

            assertThatThrownBy(() -> downloadService.generateVideoDownloadUrl(1L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ResultCode.OUTPUT_NOT_READY.getCode());
        }

        @Test
        @DisplayName("should throw OUTPUT_NOT_READY when output key is blank")
        void shouldThrowWhenOutputKeyBlank() {
            task.setOutputOssKey("   ");
            when(taskMapper.selectById(1L)).thenReturn(task);

            assertThatThrownBy(() -> downloadService.generateVideoDownloadUrl(1L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ResultCode.OUTPUT_NOT_READY.getCode());
        }

        @Test
        @DisplayName("should set Content-Disposition header in URL request")
        void shouldSetContentDisposition() throws Exception {
            when(taskMapper.selectById(1L)).thenReturn(task);
            when(ossConfig.getBucketName()).thenReturn("test-bucket");
            when(ossClient.generatePresignedUrl(any())).thenReturn(
                    new URL("https://oss.example.com/signed"));

            downloadService.generateVideoDownloadUrl(1L, 100L);

            verify(ossClient).generatePresignedUrl(urlRequestCaptor.capture());
            GeneratePresignedUrlRequest request = urlRequestCaptor.getValue();

            assertThat(request.getResponseHeaders().getContentDisposition())
                    .contains("attachment")
                    .contains("filename");
        }

        @Test
        @DisplayName("should sanitize shop name in filename")
        void shouldSanitizeShopName() throws Exception {
            task.setShopName("海底捞/望京:店");
            when(taskMapper.selectById(1L)).thenReturn(task);
            when(ossConfig.getBucketName()).thenReturn("test-bucket");
            when(ossClient.generatePresignedUrl(any())).thenReturn(
                    new URL("https://oss.example.com/signed"));

            DownloadUrlResponse response = downloadService.generateVideoDownloadUrl(1L, 100L);

            assertThat(response.getFilename()).doesNotContain("/").doesNotContain(":");
        }

        @Test
        @DisplayName("should throw OSS_ERROR when OSS fails")
        void shouldThrowWhenOssFails() {
            when(taskMapper.selectById(1L)).thenReturn(task);
            when(ossConfig.getBucketName()).thenReturn("test-bucket");
            when(ossClient.generatePresignedUrl(any())).thenThrow(new RuntimeException("OSS error"));

            assertThatThrownBy(() -> downloadService.generateVideoDownloadUrl(1L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ResultCode.OSS_ERROR.getCode());
        }
    }
}
