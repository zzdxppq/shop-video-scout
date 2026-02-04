package com.shopvideoscout.task.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.task.config.OssConfig;
import com.shopvideoscout.task.constant.TaskConstants;
import com.shopvideoscout.task.dto.AssetPackResponse;
import com.shopvideoscout.task.entity.Task;
import com.shopvideoscout.task.entity.Video;
import com.shopvideoscout.task.mapper.TaskMapper;
import com.shopvideoscout.task.mapper.VideoMapper;
import com.shopvideoscout.task.service.impl.AssetPackServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AssetPackService (Story 5.2).
 */
@ExtendWith(MockitoExtension.class)
class AssetPackServiceTest {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private VideoMapper videoMapper;

    @Mock
    private OSS ossClient;

    @Mock
    private OssConfig ossConfig;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AssetPackServiceImpl assetPackService;

    private Task task;
    private Video video1;
    private Video video2;

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(1L);
        task.setUserId(100L);
        task.setShopName("海底捞望京店");
        task.setStatus(TaskConstants.TaskStatus.COMPLETED);

        video1 = new Video();
        video1.setId(101L);
        video1.setTaskId(1L);
        video1.setOssKey("videos/1/v1.mp4");
        video1.setCategory("美食");
        video1.setIsRecommended(true);
        video1.setSortOrder(1);
        video1.setFileSize(1000000L);

        video2 = new Video();
        video2.setId(102L);
        video2.setTaskId(1L);
        video2.setOssKey("videos/1/v2.mp4");
        video2.setCategory("环境");
        video2.setIsRecommended(true);
        video2.setSortOrder(2);
        video2.setFileSize(2000000L);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("generateAssetPack")
    class GenerateAssetPackTests {

        @Test
        @DisplayName("should throw TASK_NOT_FOUND when task does not exist")
        void shouldThrowWhenTaskNotFound() {
            when(taskMapper.selectById(1L)).thenReturn(null);

            assertThatThrownBy(() -> assetPackService.generateAssetPack(1L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ResultCode.TASK_NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("should throw FORBIDDEN when user is not owner")
        void shouldThrowWhenNotOwner() {
            when(taskMapper.selectById(1L)).thenReturn(task);

            assertThatThrownBy(() -> assetPackService.generateAssetPack(1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ResultCode.FORBIDDEN.getCode());
        }

        @Test
        @DisplayName("should throw OUTPUT_NOT_READY when task not completed")
        void shouldThrowWhenTaskNotCompleted() {
            task.setStatus(TaskConstants.TaskStatus.COMPOSING);
            when(taskMapper.selectById(1L)).thenReturn(task);

            assertThatThrownBy(() -> assetPackService.generateAssetPack(1L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ResultCode.OUTPUT_NOT_READY.getCode());
        }

        @Test
        @DisplayName("should throw NO_ASSETS_TO_PACK when no recommended videos")
        void shouldThrowWhenNoRecommendedVideos() {
            when(taskMapper.selectById(1L)).thenReturn(task);
            when(videoMapper.findByTaskId(1L)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> assetPackService.generateAssetPack(1L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ResultCode.NO_ASSETS_TO_PACK.getCode());
        }

        @Test
        @DisplayName("should use cached ZIP when available")
        void shouldUseCacheWhenAvailable() throws Exception {
            when(taskMapper.selectById(1L)).thenReturn(task);
            when(videoMapper.findByTaskId(1L)).thenReturn(List.of(video1, video2));
            when(valueOperations.get(anyString())).thenReturn("assets-pack/1/cached.zip");
            when(ossConfig.getBucketName()).thenReturn("test-bucket");
            when(ossClient.doesObjectExist(eq("test-bucket"), eq("assets-pack/1/cached.zip"))).thenReturn(true);
            when(ossClient.generatePresignedUrl(any())).thenReturn(
                    new URL("https://oss.example.com/cached.zip?token=xxx"));

            AssetPackResponse response = assetPackService.generateAssetPack(1L, 100L);

            assertThat(response.getDownloadUrl()).contains("cached.zip");
            verify(ossClient, never()).putObject(any());
        }

        @Test
        @DisplayName("should create new ZIP when cache miss")
        void shouldCreateZipOnCacheMiss() throws Exception {
            when(taskMapper.selectById(1L)).thenReturn(task);
            when(videoMapper.findByTaskId(1L)).thenReturn(List.of(video1));
            when(valueOperations.get(anyString())).thenReturn(null);
            when(ossConfig.getBucketName()).thenReturn("test-bucket");

            OSSObject ossObject = mock(OSSObject.class);
            when(ossObject.getObjectContent()).thenReturn(
                    new ByteArrayInputStream("video content".getBytes()));
            when(ossClient.getObject(eq("test-bucket"), anyString())).thenReturn(ossObject);
            when(ossClient.generatePresignedUrl(any())).thenReturn(
                    new URL("https://oss.example.com/pack.zip"));

            AssetPackResponse response = assetPackService.generateAssetPack(1L, 100L);

            assertThat(response.getDownloadUrl()).isNotNull();
            assertThat(response.getFileCount()).isEqualTo(1);
            assertThat(response.getFilename()).matches("海底捞望京店-素材包-\\d{8}\\.zip");
            verify(ossClient).putObject(any());
        }

        @Test
        @DisplayName("should return correct file count and total size")
        void shouldReturnCorrectMetadata() throws Exception {
            when(taskMapper.selectById(1L)).thenReturn(task);
            when(videoMapper.findByTaskId(1L)).thenReturn(List.of(video1, video2));
            when(valueOperations.get(anyString())).thenReturn("assets-pack/1/cached.zip");
            when(ossConfig.getBucketName()).thenReturn("test-bucket");
            when(ossClient.doesObjectExist(any(), any())).thenReturn(true);
            when(ossClient.generatePresignedUrl(any())).thenReturn(
                    new URL("https://oss.example.com/pack.zip"));

            AssetPackResponse response = assetPackService.generateAssetPack(1L, 100L);

            assertThat(response.getFileCount()).isEqualTo(2);
            assertThat(response.getTotalSize()).isEqualTo(3000000L);
        }

        @Test
        @DisplayName("should limit to 10 videos max")
        void shouldLimitTo10Videos() throws Exception {
            // Create 15 recommended videos
            List<Video> manyVideos = new java.util.ArrayList<>();
            for (int i = 1; i <= 15; i++) {
                Video v = new Video();
                v.setId((long) i);
                v.setTaskId(1L);
                v.setOssKey("videos/1/v" + i + ".mp4");
                v.setCategory("分类" + i);
                v.setIsRecommended(true);
                v.setSortOrder(i);
                v.setFileSize(100000L);
                manyVideos.add(v);
            }

            when(taskMapper.selectById(1L)).thenReturn(task);
            when(videoMapper.findByTaskId(1L)).thenReturn(manyVideos);
            when(valueOperations.get(anyString())).thenReturn("cached.zip");
            when(ossConfig.getBucketName()).thenReturn("test-bucket");
            when(ossClient.doesObjectExist(any(), any())).thenReturn(true);
            when(ossClient.generatePresignedUrl(any())).thenReturn(
                    new URL("https://oss.example.com/pack.zip"));

            AssetPackResponse response = assetPackService.generateAssetPack(1L, 100L);

            assertThat(response.getFileCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("should filter only recommended videos")
        void shouldFilterOnlyRecommended() throws Exception {
            Video notRecommended = new Video();
            notRecommended.setId(103L);
            notRecommended.setTaskId(1L);
            notRecommended.setIsRecommended(false);
            notRecommended.setFileSize(500000L);

            when(taskMapper.selectById(1L)).thenReturn(task);
            when(videoMapper.findByTaskId(1L)).thenReturn(List.of(video1, notRecommended, video2));
            when(valueOperations.get(anyString())).thenReturn("cached.zip");
            when(ossConfig.getBucketName()).thenReturn("test-bucket");
            when(ossClient.doesObjectExist(any(), any())).thenReturn(true);
            when(ossClient.generatePresignedUrl(any())).thenReturn(
                    new URL("https://oss.example.com/pack.zip"));

            AssetPackResponse response = assetPackService.generateAssetPack(1L, 100L);

            // Only video1 and video2 are recommended
            assertThat(response.getFileCount()).isEqualTo(2);
            assertThat(response.getTotalSize()).isEqualTo(3000000L); // video1 + video2
        }
    }
}
