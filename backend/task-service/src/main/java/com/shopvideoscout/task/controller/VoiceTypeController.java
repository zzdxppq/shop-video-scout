package com.shopvideoscout.task.controller;

import com.shopvideoscout.common.result.R;
import com.shopvideoscout.task.dto.VoiceTypeRequest;
import com.shopvideoscout.task.service.ComposeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for voice type management.
 * PUT /api/v1/tasks/{id}/voice-type - update voice type
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class VoiceTypeController {

    private final ComposeService composeService;

    /**
     * Update voice type for a task.
     * Also transitions task status from script_edited to voice_set.
     *
     * @param id      task ID
     * @param request voice type request
     * @param userId  injected from JWT via Gateway header
     * @return success response
     */
    @PutMapping("/{id}/voice-type")
    public R<Void> updateVoiceType(
            @PathVariable Long id,
            @Valid @RequestBody VoiceTypeRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.debug("Update voice type for task {} from user {}: {}",
                id, userId, request.getVoiceType());
        composeService.updateVoiceType(id, userId, request.getVoiceType(), request.getVoiceSampleId());
        return R.ok();
    }
}
