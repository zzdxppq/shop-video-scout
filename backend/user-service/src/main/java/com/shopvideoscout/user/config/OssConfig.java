package com.shopvideoscout.user.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;
import java.util.Date;
import java.util.Map;

/**
 * Aliyun OSS configuration for user-service (voice sample uploads).
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    private static final int PRESIGNED_URL_EXPIRATION_MS = 15 * 60 * 1000;

    private static final Map<String, String> AUDIO_CONTENT_TYPES = Map.of(
            "mp3", "audio/mpeg",
            "wav", "audio/wav",
            "m4a", "audio/mp4"
    );

    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    /**
     * Generate presigned upload URL for voice sample.
     */
    public String generatePresignedUploadUrl(String ossKey, String extension) {
        OSS client = ossClient();
        Date expiration = new Date(System.currentTimeMillis() + PRESIGNED_URL_EXPIRATION_MS);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, ossKey);
        request.setExpiration(expiration);
        request.setMethod(com.aliyun.oss.HttpMethod.PUT);

        String contentType = AUDIO_CONTENT_TYPES.getOrDefault(
                extension.toLowerCase(), "application/octet-stream");
        request.setContentType(contentType);

        URL presignedUrl = client.generatePresignedUrl(request);
        return presignedUrl.toString();
    }
}
