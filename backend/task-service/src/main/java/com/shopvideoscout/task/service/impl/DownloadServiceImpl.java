package com.shopvideoscout.task.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ResponseHeaderOverrides;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.task.config.OssConfig;
import com.shopvideoscout.task.constant.TaskConstants;
import com.shopvideoscout.task.dto.DownloadUrlResponse;
import com.shopvideoscout.task.entity.Task;
import com.shopvideoscout.task.mapper.TaskMapper;
import com.shopvideoscout.task.service.DownloadService;
import com.shopvideoscout.task.util.FilenameUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Implementation of DownloadService (Story 5.2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadServiceImpl implements DownloadService {

    private static final int DOWNLOAD_URL_EXPIRATION_SECONDS = 3600;

    private final TaskMapper taskMapper;
    private final OSS ossClient;
    private final OssConfig ossConfig;

    @Override
    public DownloadUrlResponse generateVideoDownloadUrl(Long taskId, Long userId) {
        // Load and validate task
        Task task = loadAndValidateTask(taskId, userId);

        // Generate filename
        String filename = FilenameUtils.generateVideoFilename(task.getShopName());

        // Generate signed URL with Content-Disposition
        String downloadUrl = generateSignedUrl(task.getOutputOssKey(), filename);

        // Calculate expiration time
        Instant expiresAt = Instant.now().plus(DOWNLOAD_URL_EXPIRATION_SECONDS, ChronoUnit.SECONDS);

        log.info("Generated video download URL for task {}, expires at {}", taskId, expiresAt);

        return DownloadUrlResponse.builder()
                .downloadUrl(downloadUrl)
                .filename(filename)
                .expiresAt(expiresAt.toString())
                .fileSize(task.getOutputFileSize())
                .build();
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

        if (task.getOutputOssKey() == null || task.getOutputOssKey().isBlank()) {
            throw new BusinessException(ResultCode.OUTPUT_NOT_READY, "视频文件不存在");
        }

        return task;
    }

    /**
     * Generate signed URL with Content-Disposition header for forced download.
     */
    private String generateSignedUrl(String ossKey, String filename) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + DOWNLOAD_URL_EXPIRATION_SECONDS * 1000L);

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    ossConfig.getBucketName(), ossKey);
            request.setExpiration(expiration);
            request.setMethod(com.aliyun.oss.HttpMethod.GET);

            // Set Content-Disposition to force download with filename
            // Use RFC 5987 encoding for non-ASCII characters
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
