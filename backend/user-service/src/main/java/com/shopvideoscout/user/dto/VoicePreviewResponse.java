package com.shopvideoscout.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for voice sample preview.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoicePreviewResponse {

    private Long sampleId;
    private String previewText;
    private String status;
    private String cloneVoiceId;
}
