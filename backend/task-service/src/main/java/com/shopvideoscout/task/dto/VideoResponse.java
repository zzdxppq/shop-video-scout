package com.shopvideoscout.task.dto;

import com.shopvideoscout.task.entity.Video;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for video information.
 */
@Data
@Builder
public class VideoResponse {

    private Long id;
    private Long taskId;
    private String originalFilename;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String status;
    private String category;
    private List<String> tags;
    private String description;
    private Integer qualityScore;
    private Boolean isRecommended;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    /**
     * Create response from entity.
     */
    public static VideoResponse fromEntity(Video video, String cdnBaseUrl) {
        String thumbnailUrl = video.getThumbnailOssKey() != null
                ? cdnBaseUrl + "/" + video.getThumbnailOssKey()
                : null;

        return VideoResponse.builder()
                .id(video.getId())
                .taskId(video.getTaskId())
                .originalFilename(video.getOriginalFilename())
                .thumbnailUrl(thumbnailUrl)
                .durationSeconds(video.getDurationSeconds())
                .fileSize(video.getFileSize())
                .width(video.getWidth())
                .height(video.getHeight())
                .status(video.getStatus())
                .category(video.getCategory())
                .tags(video.getTags())
                .description(video.getDescription())
                .qualityScore(video.getQualityScore())
                .isRecommended(video.getIsRecommended())
                .sortOrder(video.getSortOrder())
                .createdAt(video.getCreatedAt())
                .build();
    }
}
