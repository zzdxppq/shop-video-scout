package com.shopvideoscout.user.dto;

import com.shopvideoscout.user.entity.VoiceSample;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for voice sample.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceSampleResponse {

    private Long id;
    private String name;
    private String status;
    private String cloneVoiceId;
    private Integer durationSeconds;
    private String errorMessage;
    private LocalDateTime createdAt;

    public static VoiceSampleResponse fromEntity(VoiceSample sample) {
        return VoiceSampleResponse.builder()
                .id(sample.getId())
                .name(sample.getName())
                .status(sample.getStatus())
                .cloneVoiceId(sample.getCloneVoiceId())
                .durationSeconds(sample.getDurationSeconds())
                .errorMessage(sample.getErrorMessage())
                .createdAt(sample.getCreatedAt())
                .build();
    }
}
