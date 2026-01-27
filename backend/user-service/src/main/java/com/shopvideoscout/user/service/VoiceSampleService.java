package com.shopvideoscout.user.service;

import com.shopvideoscout.user.dto.*;
import com.shopvideoscout.user.entity.VoiceSample;

import java.util.List;

/**
 * Service interface for voice sample operations.
 */
public interface VoiceSampleService {

    /**
     * Generate a presigned OSS upload URL for voice sample.
     */
    VoiceUploadUrlResponse generateUploadUrl(Long userId, VoiceUploadUrlRequest request);

    /**
     * Create a voice sample record and trigger async cloning.
     */
    VoiceSampleResponse createVoiceSample(Long userId, CreateVoiceSampleRequest request);

    /**
     * List all voice samples for a user.
     */
    List<VoiceSampleResponse> listByUserId(Long userId);

    /**
     * Get a single voice sample by ID (with ownership check).
     */
    VoiceSampleResponse getById(Long userId, Long sampleId);

    /**
     * Delete a voice sample (with ownership check).
     */
    void deleteById(Long userId, Long sampleId);

    /**
     * Update voice sample status and clone_voice_id (internal callback from media-service).
     */
    void updateCloneResult(Long sampleId, String cloneVoiceId, String status, String errorMessage);

    /**
     * Get preview info for a voice sample (requires clone to be completed).
     */
    VoicePreviewResponse getPreview(Long userId, Long sampleId);
}
