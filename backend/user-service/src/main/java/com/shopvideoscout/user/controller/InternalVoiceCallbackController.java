package com.shopvideoscout.user.controller;

import com.shopvideoscout.common.result.R;
import com.shopvideoscout.user.dto.CloneResultRequest;
import com.shopvideoscout.user.service.VoiceSampleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Internal callback endpoint for media-service → user-service communication.
 * Route prefix /internal/** is blocked at gateway level from external access.
 */
@Slf4j
@RestController
@RequestMapping("/internal/voice")
@RequiredArgsConstructor
public class InternalVoiceCallbackController {

    private final VoiceSampleService voiceSampleService;

    /**
     * Callback from media-service after voice clone processing.
     * POST /internal/voice/samples/{id}/clone-result
     */
    @PostMapping("/samples/{id}/clone-result")
    public R<Void> updateCloneResult(
            @PathVariable("id") Long sampleId,
            @Valid @RequestBody CloneResultRequest request) {
        log.info("Clone result callback for sample {}: status={}", sampleId, request.getStatus());
        voiceSampleService.updateCloneResult(
                sampleId, request.getCloneVoiceId(), request.getStatus(), request.getErrorMessage());
        return R.ok();
    }
}
