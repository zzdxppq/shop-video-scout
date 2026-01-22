package com.shopvideoscout.task.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.shopvideoscout.task.config.OssConfig;
import com.shopvideoscout.task.constant.VideoConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

/**
 * Service for Aliyun OSS operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {

    private final OSS ossClient;
    private final OssConfig ossConfig;

    /**
     * Generate presigned upload URL for video file.
     *
     * @param userId    user ID
     * @param taskId    task ID
     * @param extension file extension (mp4 or mov)
     * @return presigned URL result containing upload URL and OSS key
     */
    public PresignedUrlResult generatePresignedUploadUrl(Long userId, Long taskId, String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String ossKey = String.format("videos/%d/%d/%s.%s", userId, taskId, uuid, extension.toLowerCase());

        Date expiration = new Date(System.currentTimeMillis()
                + VideoConstants.PRESIGNED_URL_EXPIRATION_MINUTES * 60 * 1000L);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                ossConfig.getBucketName(), ossKey);
        request.setExpiration(expiration);
        request.setMethod(com.aliyun.oss.HttpMethod.PUT);

        // Set content type
        String contentType = "mov".equalsIgnoreCase(extension) ? "video/quicktime" : "video/mp4";
        request.setContentType(contentType);

        URL presignedUrl = ossClient.generatePresignedUrl(request);

        log.debug("Generated presigned URL for user {} task {}: {}", userId, taskId, ossKey);

        return new PresignedUrlResult(presignedUrl.toString(), ossKey,
                VideoConstants.PRESIGNED_URL_EXPIRATION_MINUTES * 60);
    }

    /**
     * Generate presigned download URL for an object.
     *
     * @param ossKey object key
     * @param expirationMinutes URL expiration time in minutes
     * @return presigned download URL
     */
    public String generatePresignedDownloadUrl(String ossKey, int expirationMinutes) {
        Date expiration = new Date(System.currentTimeMillis() + expirationMinutes * 60 * 1000L);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                ossConfig.getBucketName(), ossKey);
        request.setExpiration(expiration);
        request.setMethod(com.aliyun.oss.HttpMethod.GET);

        URL presignedUrl = ossClient.generatePresignedUrl(request);
        return presignedUrl.toString();
    }

    /**
     * Check if object exists in OSS.
     *
     * @param ossKey object key
     * @return true if exists
     */
    public boolean objectExists(String ossKey) {
        return ossClient.doesObjectExist(ossConfig.getBucketName(), ossKey);
    }

    /**
     * Get object metadata.
     *
     * @param ossKey object key
     * @return object metadata
     */
    public ObjectMetadata getObjectMetadata(String ossKey) {
        return ossClient.getObjectMetadata(ossConfig.getBucketName(), ossKey);
    }

    /**
     * Get object input stream.
     *
     * @param ossKey object key
     * @return input stream
     */
    public InputStream getObject(String ossKey) {
        OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), ossKey);
        return ossObject.getObjectContent();
    }

    /**
     * Delete object from OSS.
     *
     * @param ossKey object key
     */
    public void deleteObject(String ossKey) {
        ossClient.deleteObject(ossConfig.getBucketName(), ossKey);
        log.debug("Deleted OSS object: {}", ossKey);
    }

    /**
     * Get CDN base URL.
     */
    public String getCdnBaseUrl() {
        return ossConfig.getCdnBaseUrl();
    }

    /**
     * Result holder for presigned URL generation.
     */
    public record PresignedUrlResult(String uploadUrl, String ossKey, int expiresIn) {}
}
