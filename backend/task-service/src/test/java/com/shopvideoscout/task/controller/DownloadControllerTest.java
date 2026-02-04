package com.shopvideoscout.task.controller;

import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.task.dto.AssetPackResponse;
import com.shopvideoscout.task.dto.DownloadUrlResponse;
import com.shopvideoscout.task.service.AssetPackService;
import com.shopvideoscout.task.service.DownloadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DownloadController (Story 5.2).
 */
@ExtendWith(MockitoExtension.class)
class DownloadControllerTest {

    @Mock
    private DownloadService downloadService;

    @Mock
    private AssetPackService assetPackService;

    @InjectMocks
    private DownloadController downloadController;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = new User("100", "password", Collections.emptyList());
    }

    @Nested
    @DisplayName("getVideoDownloadUrl")
    class GetVideoDownloadUrlTests {

        @Test
        @DisplayName("should return download URL response")
        void shouldReturnDownloadUrl() {
            DownloadUrlResponse mockResponse = DownloadUrlResponse.builder()
                    .downloadUrl("https://oss.example.com/video.mp4?token=xxx")
                    .filename("海底捞-探店视频-20260203.mp4")
                    .expiresAt("2026-02-03T17:00:00Z")
                    .fileSize(47841280L)
                    .build();

            when(downloadService.generateVideoDownloadUrl(1L, 100L)).thenReturn(mockResponse);

            var result = downloadController.getVideoDownloadUrl(1L, userDetails);

            assertThat(result.getCode()).isEqualTo(200);
            assertThat(result.getData().getDownloadUrl()).contains("oss.example.com");
            assertThat(result.getData().getFilename()).contains("探店视频");
        }

        @Test
        @DisplayName("should propagate TASK_NOT_FOUND exception")
        void shouldPropagateTaskNotFound() {
            when(downloadService.generateVideoDownloadUrl(1L, 100L))
                    .thenThrow(new BusinessException(ResultCode.TASK_NOT_FOUND));

            assertThatThrownBy(() -> downloadController.getVideoDownloadUrl(1L, userDetails))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ResultCode.TASK_NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("should propagate OUTPUT_NOT_READY exception")
        void shouldPropagateOutputNotReady() {
            when(downloadService.generateVideoDownloadUrl(1L, 100L))
                    .thenThrow(new BusinessException(ResultCode.OUTPUT_NOT_READY));

            assertThatThrownBy(() -> downloadController.getVideoDownloadUrl(1L, userDetails))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ResultCode.OUTPUT_NOT_READY.getCode());
        }
    }

    @Nested
    @DisplayName("getAssetsPackDownloadUrl")
    class GetAssetsPackDownloadUrlTests {

        @Test
        @DisplayName("should return asset pack response")
        void shouldReturnAssetPack() {
            AssetPackResponse mockResponse = AssetPackResponse.builder()
                    .downloadUrl("https://oss.example.com/pack.zip?token=xxx")
                    .filename("海底捞-素材包-20260203.zip")
                    .fileCount(5)
                    .totalSize(125829120L)
                    .expiresAt("2026-02-03T17:00:00Z")
                    .build();

            when(assetPackService.generateAssetPack(1L, 100L)).thenReturn(mockResponse);

            var result = downloadController.getAssetsPackDownloadUrl(1L, userDetails);

            assertThat(result.getCode()).isEqualTo(200);
            assertThat(result.getData().getDownloadUrl()).contains("pack.zip");
            assertThat(result.getData().getFileCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("should propagate NO_ASSETS_TO_PACK exception")
        void shouldPropagateNoAssets() {
            when(assetPackService.generateAssetPack(1L, 100L))
                    .thenThrow(new BusinessException(ResultCode.NO_ASSETS_TO_PACK));

            assertThatThrownBy(() -> downloadController.getAssetsPackDownloadUrl(1L, userDetails))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ResultCode.NO_ASSETS_TO_PACK.getCode());
        }
    }
}
