package com.shopvideoscout.task.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.shopvideoscout.task.config.OssConfig;
import com.shopvideoscout.task.constant.VideoConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OssService.
 * Tests cover OSS presigned URL generation scenarios.
 */
@ExtendWith(MockitoExtension.class)
class OssServiceTest {

    @Mock
    private OSS ossClient;

    @Mock
    private OssConfig ossConfig;

    @InjectMocks
    private OssService ossService;

    @BeforeEach
    void setUp() {
        when(ossConfig.getBucketName()).thenReturn("test-bucket");
        when(ossConfig.getCdnBaseUrl()).thenReturn("https://cdn.example.com");
    }

    @Nested
    @DisplayName("Presigned Upload URL Generation")
    class PresignedUploadUrlTests {

        @Test
        @DisplayName("2.1-INT-006: Generate presigned URL should return valid URL with correct path")
        void generatePresignedUrl_ShouldReturnValidUrl() throws MalformedURLException {
            // Given
            Long userId = 100L;
            Long taskId = 1L;
            String extension = "mp4";

            URL mockUrl = new URL("https://oss.example.com/videos/100/1/uuid.mp4?signature=xxx");
            when(ossClient.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                    .thenReturn(mockUrl);

            // When
            OssService.PresignedUrlResult result = ossService.generatePresignedUploadUrl(userId, taskId, extension);

            // Then
            assertNotNull(result);
            assertNotNull(result.uploadUrl());
            assertNotNull(result.ossKey());
            assertTrue(result.ossKey().startsWith("videos/100/1/"));
            assertTrue(result.ossKey().endsWith(".mp4"));
            assertEquals(VideoConstants.PRESIGNED_URL_EXPIRATION_MINUTES * 60, result.expiresIn());
        }

        @Test
        @DisplayName("2.1-INT-007: Presigned URL should have 15 minute expiry")
        void presignedUrl_ShouldHave15MinuteExpiry() throws MalformedURLException {
            // Given
            URL mockUrl = new URL("https://oss.example.com/upload");
            when(ossClient.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                    .thenReturn(mockUrl);

            ArgumentCaptor<GeneratePresignedUrlRequest> requestCaptor =
                    ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);

            // When
            OssService.PresignedUrlResult result = ossService.generatePresignedUploadUrl(100L, 1L, "mp4");

            // Then
            verify(ossClient).generatePresignedUrl(requestCaptor.capture());
            GeneratePresignedUrlRequest capturedRequest = requestCaptor.getValue();

            // Verify expiration is approximately 15 minutes from now
            Date expiration = capturedRequest.getExpiration();
            long expectedExpiry = System.currentTimeMillis() + (15 * 60 * 1000);
            long actualExpiry = expiration.getTime();

            // Allow 5 second tolerance for test execution time
            assertTrue(Math.abs(actualExpiry - expectedExpiry) < 5000,
                    "Expiration should be approximately 15 minutes from now");
        }

        @Test
        @DisplayName("2.1-UNIT-010: OSS key should follow path format BR-1.1")
        void ossKey_ShouldFollowPathFormat() throws MalformedURLException {
            // Given
            URL mockUrl = new URL("https://oss.example.com/upload");
            when(ossClient.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                    .thenReturn(mockUrl);

            // When
            OssService.PresignedUrlResult result = ossService.generatePresignedUploadUrl(100L, 1L, "mp4");

            // Then
            String ossKey = result.ossKey();
            // Format: videos/{user_id}/{task_id}/{uuid}.{ext}
            assertTrue(ossKey.matches("videos/100/1/[a-f0-9]{32}\\.mp4"),
                    "OSS key should match format: videos/{user_id}/{task_id}/{uuid}.{ext}");
        }

        @Test
        @DisplayName("2.1-UNIT-011: MOV files should have correct content type")
        void movFiles_ShouldHaveCorrectContentType() throws MalformedURLException {
            // Given
            URL mockUrl = new URL("https://oss.example.com/upload");
            when(ossClient.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                    .thenReturn(mockUrl);

            ArgumentCaptor<GeneratePresignedUrlRequest> requestCaptor =
                    ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);

            // When
            ossService.generatePresignedUploadUrl(100L, 1L, "mov");

            // Then
            verify(ossClient).generatePresignedUrl(requestCaptor.capture());
            assertEquals("video/quicktime", requestCaptor.getValue().getContentType());
        }

        @Test
        @DisplayName("2.1-UNIT-012: MP4 files should have correct content type")
        void mp4Files_ShouldHaveCorrectContentType() throws MalformedURLException {
            // Given
            URL mockUrl = new URL("https://oss.example.com/upload");
            when(ossClient.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                    .thenReturn(mockUrl);

            ArgumentCaptor<GeneratePresignedUrlRequest> requestCaptor =
                    ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);

            // When
            ossService.generatePresignedUploadUrl(100L, 1L, "mp4");

            // Then
            verify(ossClient).generatePresignedUrl(requestCaptor.capture());
            assertEquals("video/mp4", requestCaptor.getValue().getContentType());
        }

        @Test
        @DisplayName("2.1-BLIND-005: Extension case should be normalized to lowercase")
        void extensionCase_ShouldBeNormalizedToLowercase() throws MalformedURLException {
            // Given
            URL mockUrl = new URL("https://oss.example.com/upload");
            when(ossClient.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                    .thenReturn(mockUrl);

            // When
            OssService.PresignedUrlResult result = ossService.generatePresignedUploadUrl(100L, 1L, "MP4");

            // Then
            assertTrue(result.ossKey().endsWith(".mp4"),
                    "Extension should be normalized to lowercase");
        }
    }

    @Nested
    @DisplayName("Object Existence Check")
    class ObjectExistsTests {

        @Test
        @DisplayName("2.1-UNIT-013: Existing object should return true")
        void existingObject_ShouldReturnTrue() {
            // Given
            when(ossClient.doesObjectExist("test-bucket", "videos/100/1/uuid.mp4"))
                    .thenReturn(true);

            // When
            boolean exists = ossService.objectExists("videos/100/1/uuid.mp4");

            // Then
            assertTrue(exists);
        }

        @Test
        @DisplayName("2.1-UNIT-014: Non-existing object should return false")
        void nonExistingObject_ShouldReturnFalse() {
            // Given
            when(ossClient.doesObjectExist("test-bucket", "videos/100/1/nonexistent.mp4"))
                    .thenReturn(false);

            // When
            boolean exists = ossService.objectExists("videos/100/1/nonexistent.mp4");

            // Then
            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("Object Deletion")
    class DeleteObjectTests {

        @Test
        @DisplayName("2.1-UNIT-015: Delete object should call OSS client")
        void deleteObject_ShouldCallOssClient() {
            // Given
            String ossKey = "videos/100/1/uuid.mp4";

            // When
            ossService.deleteObject(ossKey);

            // Then
            verify(ossClient).deleteObject("test-bucket", ossKey);
        }
    }

    @Nested
    @DisplayName("CDN Base URL")
    class CdnBaseUrlTests {

        @Test
        @DisplayName("2.1-UNIT-016: Get CDN base URL should return configured value")
        void getCdnBaseUrl_ShouldReturnConfiguredValue() {
            // When
            String cdnUrl = ossService.getCdnBaseUrl();

            // Then
            assertEquals("https://cdn.example.com", cdnUrl);
        }
    }
}
