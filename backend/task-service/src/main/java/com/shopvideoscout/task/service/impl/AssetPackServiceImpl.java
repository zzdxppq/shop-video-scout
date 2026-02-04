package com.shopvideoscout.task.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.ResponseHeaderOverrides;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.task.config.OssConfig;
import com.shopvideoscout.task.constant.TaskConstants;
import com.shopvideoscout.task.dto.AssetPackResponse;
import com.shopvideoscout.task.entity.Task;
import com.shopvideoscout.task.entity.Video;
import com.shopvideoscout.task.mapper.TaskMapper;
import com.shopvideoscout.task.mapper.VideoMapper;
import com.shopvideoscout.task.service.AssetPackService;
import com.shopvideoscout.task.util.FilenameUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Implementation of AssetPackService (Story 5.2).
 *
 * Creates ZIP file from recommended shots and caches in Redis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetPackServiceImpl implements AssetPackService {

    private static final int DOWNLOAD_URL_EXPIRATION_SECONDS = 3600;
    private static final int MAX_VIDEOS_IN_PACK = 10;
    private static final String CACHE_KEY_PREFIX = "assets-pack:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final TaskMapper taskMapper;
    private final VideoMapper videoMapper;
    private final OSS ossClient;
    private final OssConfig ossConfig;
    private final StringRedisTemplate redisTemplate;

    @Override
    public AssetPackResponse generateAssetPack(Long taskId, Long userId) {
        // Load and validate task
        Task task = loadAndValidateTask(taskId, userId);

        // Get recommended videos
        List<Video> recommendedVideos = getRecommendedVideos(taskId);

        if (recommendedVideos.isEmpty()) {
            throw new BusinessException(ResultCode.NO_ASSETS_TO_PACK);
        }

        // Generate videos hash for cache key
        String videosHash = generateVideosHash(recommendedVideos);
        String cacheKey = CACHE_KEY_PREFIX + taskId + ":" + videosHash;

        // Check cache
        String cachedOssKey = redisTemplate.opsForValue().get(cacheKey);
        if (cachedOssKey != null && ossClient.doesObjectExist(ossConfig.getBucketName(), cachedOssKey)) {
            log.info("Cache hit for assets pack, task {}", taskId);
            return buildResponse(task, cachedOssKey, recommendedVideos);
        }

        // Create ZIP and upload to OSS
        String zipOssKey = createAndUploadZip(taskId, task.getShopName(), recommendedVideos);

        // Cache the OSS key
        redisTemplate.opsForValue().set(cacheKey, zipOssKey, CACHE_TTL);

        log.info("Created new assets pack for task {}, {} videos", taskId, recommendedVideos.size());

        return buildResponse(task, zipOssKey, recommendedVideos);
    }

    /**
     * Load task and validate ownership and status.
     */
    private Task loadAndValidateTask(Long taskId, Long userId) {
        Task task = taskMapper.selectById(taskId);

        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }

        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问此任务");
        }

        if (!TaskConstants.TaskStatus.COMPLETED.equals(task.getStatus())) {
            throw new BusinessException(ResultCode.OUTPUT_NOT_READY,
                    "视频尚未完成合成，当前状态: " + task.getStatus());
        }

        return task;
    }

    /**
     * Get recommended videos for task, limited to MAX_VIDEOS_IN_PACK.
     */
    private List<Video> getRecommendedVideos(Long taskId) {
        return videoMapper.findByTaskId(taskId).stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsRecommended()))
                .sorted(Comparator.comparingInt(v -> v.getSortOrder() != null ? v.getSortOrder() : 999))
                .limit(MAX_VIDEOS_IN_PACK)
                .collect(Collectors.toList());
    }

    /**
     * Generate hash of video IDs for cache key.
     */
    private String generateVideosHash(List<Video> videos) {
        try {
            String ids = videos.stream()
                    .map(v -> v.getId().toString())
                    .sorted()
                    .collect(Collectors.joining(","));

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(ids.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 8);
        } catch (Exception e) {
            log.warn("Failed to generate videos hash: {}", e.getMessage());
            return String.valueOf(System.currentTimeMillis());
        }
    }

    /**
     * Create ZIP file and upload to OSS.
     */
    private String createAndUploadZip(Long taskId, String shopName, List<Video> videos) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                int sequence = 1;
                for (Video video : videos) {
                    if (video.getOssKey() == null) {
                        log.warn("Video {} has no OSS key, skipping", video.getId());
                        continue;
                    }

                    try {
                        // Download video from OSS
                        OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), video.getOssKey());
                        try (InputStream is = ossObject.getObjectContent()) {
                            // Add to ZIP
                            String entryName = FilenameUtils.generateShotFilename(sequence, video.getCategory());
                            zos.putNextEntry(new ZipEntry(entryName));

                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = is.read(buffer)) > 0) {
                                zos.write(buffer, 0, len);
                            }

                            zos.closeEntry();
                            sequence++;
                        }
                    } catch (Exception e) {
                        log.warn("Failed to add video {} to ZIP: {}", video.getId(), e.getMessage());
                        // Continue with other videos
                    }
                }
            }

            if (baos.size() == 0) {
                throw new BusinessException(ResultCode.ASSET_PACK_FAILED);
            }

            // Upload ZIP to OSS
            String zipOssKey = String.format("assets-pack/%d/pack-%d.zip", taskId, System.currentTimeMillis());

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(baos.size());
            metadata.setContentType("application/zip");

            ossClient.putObject(new PutObjectRequest(
                    ossConfig.getBucketName(),
                    zipOssKey,
                    new ByteArrayInputStream(baos.toByteArray()),
                    metadata));

            return zipOssKey;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create assets pack for task {}: {}", taskId, e.getMessage());
            throw new BusinessException(ResultCode.ASSET_PACK_FAILED);
        }
    }

    /**
     * Build response with signed URL.
     */
    private AssetPackResponse buildResponse(Task task, String zipOssKey, List<Video> videos) {
        String filename = FilenameUtils.generateAssetPackFilename(task.getShopName());
        String downloadUrl = generateSignedUrl(zipOssKey, filename);
        Instant expiresAt = Instant.now().plus(DOWNLOAD_URL_EXPIRATION_SECONDS, ChronoUnit.SECONDS);

        // Calculate total size
        long totalSize = videos.stream()
                .mapToLong(v -> v.getFileSize() != null ? v.getFileSize() : 0)
                .sum();

        return AssetPackResponse.builder()
                .downloadUrl(downloadUrl)
                .filename(filename)
                .fileCount(videos.size())
                .totalSize(totalSize)
                .expiresAt(expiresAt.toString())
                .build();
    }

    /**
     * Generate signed URL with Content-Disposition header.
     */
    private String generateSignedUrl(String ossKey, String filename) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + DOWNLOAD_URL_EXPIRATION_SECONDS * 1000L);

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    ossConfig.getBucketName(), ossKey);
            request.setExpiration(expiration);
            request.setMethod(com.aliyun.oss.HttpMethod.GET);

            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            ResponseHeaderOverrides overrides = new ResponseHeaderOverrides();
            overrides.setContentDisposition(
                    String.format("attachment; filename=\"%s\"; filename*=UTF-8''%s",
                            filename.replaceAll("[^a-zA-Z0-9._-]", "_"),
                            encodedFilename));
            request.setResponseHeaders(overrides);

            URL presignedUrl = ossClient.generatePresignedUrl(request);
            return presignedUrl.toString();

        } catch (Exception e) {
            log.error("Failed to generate signed URL for {}: {}", ossKey, e.getMessage());
            throw new BusinessException(ResultCode.OSS_ERROR);
        }
    }
}
