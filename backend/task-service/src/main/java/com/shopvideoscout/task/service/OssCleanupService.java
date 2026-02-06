package com.shopvideoscout.task.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.ListObjectsV2Request;
import com.aliyun.oss.model.ListObjectsV2Result;
import com.aliyun.oss.model.OSSObjectSummary;
import com.shopvideoscout.task.config.OssConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for cleaning up OSS files associated with tasks.
 * Story 5.5: 历史任务管理
 *
 * Handles asynchronous deletion of all OSS objects for a task.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssCleanupService {

    private final OSS ossClient;
    private final OssConfig ossConfig;

    /**
     * Delete all OSS files associated with a task.
     * This method runs asynchronously to avoid blocking the API response.
     *
     * OSS paths to clean:
     * - videos/{userId}/{taskId}/ - Uploaded source videos
     * - frames/{taskId}/ - Extracted video frames
     * - audio/{taskId}/ - TTS audio files
     * - output/{taskId}/ - Final composed video
     * - assets/{taskId}/ - Asset packs for download
     *
     * @param taskId task ID
     * @param userId user ID (for video path)
     */
    @Async
    public void cleanupTaskFiles(Long taskId, Long userId) {
        log.info("Starting OSS cleanup for task: {}, user: {}", taskId, userId);

        List<String> prefixes = List.of(
            String.format("videos/%d/%d/", userId, taskId),
            String.format("frames/%d/", taskId),
            String.format("audio/%d/", taskId),
            String.format("output/%d/", taskId),
            String.format("assets/%d/", taskId)
        );

        int totalDeleted = 0;
        int totalFailed = 0;

        for (String prefix : prefixes) {
            try {
                int deleted = deleteObjectsByPrefix(prefix);
                totalDeleted += deleted;
                log.debug("Deleted {} objects with prefix: {}", deleted, prefix);
            } catch (Exception e) {
                totalFailed++;
                log.error("Failed to delete objects with prefix: {}", prefix, e);
                // Continue with other prefixes - best effort cleanup
            }
        }

        log.info("OSS cleanup completed for task: {}. Deleted: {}, Failed prefixes: {}",
            taskId, totalDeleted, totalFailed);
    }

    /**
     * Delete all objects with a given prefix.
     *
     * @param prefix OSS key prefix
     * @return number of objects deleted
     */
    private int deleteObjectsByPrefix(String prefix) {
        String bucketName = ossConfig.getBucketName();
        List<String> keysToDelete = new ArrayList<>();

        // List all objects with prefix
        ListObjectsV2Request listRequest = new ListObjectsV2Request(bucketName)
            .withPrefix(prefix)
            .withMaxKeys(1000);

        ListObjectsV2Result result;
        do {
            result = ossClient.listObjectsV2(listRequest);
            List<String> keys = result.getObjectSummaries().stream()
                .map(OSSObjectSummary::getKey)
                .collect(Collectors.toList());
            keysToDelete.addAll(keys);

            listRequest.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        if (keysToDelete.isEmpty()) {
            return 0;
        }

        // Delete in batches of 1000 (OSS limit)
        int batchSize = 1000;
        for (int i = 0; i < keysToDelete.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, keysToDelete.size());
            List<String> batch = keysToDelete.subList(i, endIndex);

            DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucketName)
                .withKeys(batch)
                .withQuiet(true);

            ossClient.deleteObjects(deleteRequest);
        }

        return keysToDelete.size();
    }
}
