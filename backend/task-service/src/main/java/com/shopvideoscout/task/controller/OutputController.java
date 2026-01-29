package com.shopvideoscout.task.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.R;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.task.config.OssConfig;
import com.shopvideoscout.task.constant.TaskConstants;
import com.shopvideoscout.task.dto.OutputResponse;
import com.shopvideoscout.task.dto.OutputVideoInfo;
import com.shopvideoscout.task.dto.ShotUsedInfo;
import com.shopvideoscout.task.entity.Task;
import com.shopvideoscout.task.mapper.ScriptMapper;
import com.shopvideoscout.task.mapper.TaskMapper;
import com.shopvideoscout.task.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controller for task output video retrieval (Story 4.3).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class OutputController {

    private final TaskMapper taskMapper;
    private final ScriptMapper scriptMapper;
    private final VideoMapper videoMapper;
    private final OssConfig ossConfig;
    private final ObjectMapper objectMapper;

    /**
     * Get task output video information.
     *
     * @param id          task ID
     * @param userDetails authenticated user
     * @return output response with video URL and metadata
     */
    @GetMapping("/{id}/output")
    public R<OutputResponse> getOutput(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Load task
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }

        // Validate ownership
        Long userId = Long.parseLong(userDetails.getUsername());
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问此任务");
        }

        // Check if output is ready
        if (!TaskConstants.TaskStatus.COMPLETED.equals(task.getStatus())) {
            throw new BusinessException(ResultCode.OUTPUT_NOT_READY,
                    "视频尚未完成，当前状态: " + task.getStatus());
        }

        if (task.getOutputOssKey() == null || task.getOutputOssKey().isBlank()) {
            throw new BusinessException(ResultCode.OUTPUT_NOT_READY,
                    "视频输出尚未就绪");
        }

        // Build video info
        OutputVideoInfo videoInfo = OutputVideoInfo.builder()
                .url(generateCdnUrl(task.getOutputOssKey()))
                .durationSeconds(task.getOutputDurationSeconds())
                .fileSize(task.getOutputFileSize())
                .width(1080)
                .height(1920)
                .format("mp4")
                .build();

        // Build shots used info
        List<ShotUsedInfo> shotsUsed = getShotsUsed(id);

        OutputResponse response = OutputResponse.builder()
                .status(task.getStatus())
                .video(videoInfo)
                .shotsUsed(shotsUsed)
                .build();

        return R.ok(response);
    }

    /**
     * Generate CDN URL from OSS key.
     */
    private String generateCdnUrl(String ossKey) {
        String cdnBase = ossConfig.getCdnBaseUrl();
        if (cdnBase != null && !cdnBase.isBlank()) {
            return cdnBase.endsWith("/") ? cdnBase + ossKey : cdnBase + "/" + ossKey;
        }
        // Fallback to direct OSS URL
        return String.format("https://%s.%s/%s",
                ossConfig.getBucketName(), ossConfig.getEndpoint(), ossKey);
    }

    /**
     * Get shots used in the composition.
     * Queries script for paragraph shot_ids, then queries videos table.
     */
    private List<ShotUsedInfo> getShotsUsed(Long taskId) {
        List<ShotUsedInfo> result = new ArrayList<>();

        try {
            // Get script content
            String scriptContent = scriptMapper.findContentByTaskId(taskId);
            if (scriptContent == null || scriptContent.isBlank()) {
                return result;
            }

            // Parse paragraphs to get shot_ids
            JsonNode root = objectMapper.readTree(scriptContent);
            JsonNode paragraphsNode = root.get("paragraphs");
            if (paragraphsNode == null || !paragraphsNode.isArray()) {
                return result;
            }

            Set<Long> shotIds = new HashSet<>();
            for (JsonNode pNode : paragraphsNode) {
                if (pNode.has("shot_id") && !pNode.get("shot_id").isNull()) {
                    shotIds.add(pNode.get("shot_id").asLong());
                }
            }

            // Query videos for each shot_id
            for (Long shotId : shotIds) {
                var video = videoMapper.selectById(shotId);
                if (video != null) {
                    String thumbnailUrl = null;
                    if (video.getThumbnailOssKey() != null) {
                        thumbnailUrl = generateCdnUrl(video.getThumbnailOssKey());
                    }

                    result.add(ShotUsedInfo.builder()
                            .id(video.getId())
                            .thumbnailUrl(thumbnailUrl)
                            .category(video.getCategory())
                            .durationSeconds(video.getDurationSeconds())
                            .build());
                }
            }

        } catch (Exception e) {
            log.warn("Failed to get shots used for task {}: {}", taskId, e.getMessage());
        }

        return result;
    }
}
