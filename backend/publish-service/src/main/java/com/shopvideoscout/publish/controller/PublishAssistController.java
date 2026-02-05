package com.shopvideoscout.publish.controller;

import com.shopvideoscout.common.result.R;
import com.shopvideoscout.publish.dto.PublishAssistResponse;
import com.shopvideoscout.publish.service.PublishAssistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for publish assist APIs.
 * Story 5.3: 发布辅助服务
 *
 * Endpoints:
 * - GET /api/v1/publish/tasks/{id}/assist
 * - POST /api/v1/publish/tasks/{id}/assist/regenerate
 */
@RestController
@RequestMapping("/api/v1/publish/tasks")
@RequiredArgsConstructor
public class PublishAssistController {

    private final PublishAssistService publishAssistService;

    /**
     * Get publish assist (topics + titles) for a task.
     *
     * @param taskId      task ID
     * @param userDetails authenticated user
     * @return publish assist response with topics, titles, and regenerate remaining
     */
    @GetMapping("/{id}/assist")
    public R<PublishAssistResponse> getPublishAssist(
            @PathVariable("id") Long taskId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        PublishAssistResponse response = publishAssistService.getPublishAssist(taskId, userId);
        return R.ok(response);
    }

    /**
     * Regenerate publish assist with higher temperature.
     * Max 3 regenerations per task.
     *
     * @param taskId      task ID
     * @param userDetails authenticated user
     * @return regenerated publish assist response
     */
    @PostMapping("/{id}/assist/regenerate")
    public R<PublishAssistResponse> regenerate(
            @PathVariable("id") Long taskId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        PublishAssistResponse response = publishAssistService.regenerate(taskId, userId);
        return R.ok(response);
    }
}
