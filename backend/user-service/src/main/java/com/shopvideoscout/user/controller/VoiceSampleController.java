package com.shopvideoscout.user.controller;

import com.shopvideoscout.common.result.R;
import com.shopvideoscout.user.dto.*;
import com.shopvideoscout.user.service.VoiceSampleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Voice sample CRUD endpoints.
 * All /api/v1/voice/** routes are handled by user-service via gateway.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/voice")
@RequiredArgsConstructor
public class VoiceSampleController {

    private final VoiceSampleService voiceSampleService;

    /**
     * Generate presigned OSS upload URL for voice sample.
     * POST /api/v1/voice/upload-url
     */
    @PostMapping("/upload-url")
    public R<VoiceUploadUrlResponse> generateUploadUrl(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody VoiceUploadUrlRequest request) {
        log.debug("Generate voice upload URL for user: {}", userId);
        VoiceUploadUrlResponse response = voiceSampleService.generateUploadUrl(userId, request);
        return R.ok(response);
    }

    /**
     * Create voice sample record after upload.
     * POST /api/v1/voice/samples
     */
    @PostMapping("/samples")
    @ResponseStatus(HttpStatus.CREATED)
    public R<VoiceSampleResponse> createVoiceSample(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateVoiceSampleRequest request) {
        log.debug("Create voice sample for user: {}", userId);
        VoiceSampleResponse response = voiceSampleService.createVoiceSample(userId, request);
        return R.ok(response);
    }

    /**
     * List user's voice samples.
     * GET /api/v1/voice/samples
     */
    @GetMapping("/samples")
    public R<List<VoiceSampleResponse>> listSamples(
            @RequestHeader("X-User-Id") Long userId) {
        List<VoiceSampleResponse> samples = voiceSampleService.listByUserId(userId);
        return R.ok(samples);
    }

    /**
     * Get voice sample detail.
     * GET /api/v1/voice/samples/{id}
     */
    @GetMapping("/samples/{id}")
    public R<VoiceSampleResponse> getSample(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long sampleId) {
        VoiceSampleResponse response = voiceSampleService.getById(userId, sampleId);
        return R.ok(response);
    }

    /**
     * Delete voice sample.
     * DELETE /api/v1/voice/samples/{id}
     */
    @DeleteMapping("/samples/{id}")
    public R<Void> deleteSample(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long sampleId) {
        log.debug("Delete voice sample {} for user {}", sampleId, userId);
        voiceSampleService.deleteById(userId, sampleId);
        return R.ok();
    }

    /**
     * Get voice sample preview info (requires clone to be completed).
     * GET /api/v1/voice/samples/{id}/preview
     */
    @GetMapping("/samples/{id}/preview")
    public R<VoicePreviewResponse> getPreview(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long sampleId) {
        VoicePreviewResponse response = voiceSampleService.getPreview(userId, sampleId);
        return R.ok(response);
    }

}
