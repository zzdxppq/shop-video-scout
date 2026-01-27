package com.shopvideoscout.task.controller;

import com.shopvideoscout.common.result.R;
import com.shopvideoscout.task.dto.ComposeProgressResponse;
import com.shopvideoscout.task.dto.ComposeResponse;
import com.shopvideoscout.task.service.ComposeProgressService;
import com.shopvideoscout.task.service.ComposeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for compose operations (TTS synthesis).
 * POST /api/v1/tasks/{id}/compose - trigger compose
 * GET /api/v1/tasks/{id}/compose-progress - query progress
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class ComposeController {

    private final ComposeService composeService;
    private final ComposeProgressService composeProgressService;

    /**
     * Trigger compose for a task.
     * Validates task status (script_edited or voice_set),
     * publishes ComposeMessage to MQ, transitions status to composing.
     *
     * @param id     task ID
     * @param userId injected from JWT via Gateway header
     * @return compose response
     */
    @PostMapping("/{id}/compose")
    public R<ComposeResponse> triggerCompose(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.debug("Compose request for task {} from user {}", id, userId);
        ComposeResponse response = composeService.triggerCompose(id, userId);
        return R.ok(response);
    }

    /**
     * Get compose progress for a task.
     * Reads progress from Redis (written by media-service).
     *
     * @param id     task ID
     * @param userId injected from JWT via Gateway header
     * @return progress response
     */
    @GetMapping("/{id}/compose-progress")
    public R<ComposeProgressResponse> getComposeProgress(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        log.debug("Progress query for task {} from user {}", id, userId);
        ComposeProgressResponse response = composeProgressService.getProgress(id, userId);
        return R.ok(response);
    }
}
