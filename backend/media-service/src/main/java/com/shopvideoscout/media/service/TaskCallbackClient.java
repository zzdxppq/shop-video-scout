package com.shopvideoscout.media.service;

import com.shopvideoscout.media.config.ComposeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client for calling back to task-service after compose completion.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskCallbackClient {

    private final RestTemplate restTemplate;
    private final ComposeProperties composeProperties;

    private static final int MAX_CALLBACK_RETRIES = 3;
    private static final long CALLBACK_BACKOFF_MS = 2000;

    /**
     * Notify task-service that compose completed successfully (legacy - TTS only).
     */
    public void notifyComposeComplete(Long taskId, TtsSynthesisService.SynthesisResult result) {
        String url = resolveCallbackUrl(taskId);
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", taskId);
        body.put("status", "completed");
        body.put("totalDurationSeconds", result.getTotalDurationSeconds());
        body.put("audioFiles", result.getParagraphResults().stream()
                .map(p -> Map.of(
                        "paragraphIndex", p.getParagraphIndex(),
                        "audioUrl", p.getAudioUrl(),
                        "ossKey", p.getOssKey(),
                        "durationSeconds", p.getDurationSeconds()
                ))
                .collect(Collectors.toList()));

        callWithRetry(url, body);
    }

    /**
     * Notify task-service that composition completed with output video info (Story 4.3).
     */
    public void notifyComposeCompleteWithOutput(Long taskId, String outputOssKey,
                                                  int durationSeconds, long fileSizeBytes) {
        String url = resolveCallbackUrl(taskId);
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", taskId);
        body.put("status", "completed");
        body.put("outputOssKey", outputOssKey);
        body.put("outputDurationSeconds", durationSeconds);
        body.put("outputFileSize", fileSizeBytes);

        callWithRetry(url, body);
    }

    /**
     * Notify task-service that compose failed.
     */
    public void notifyComposeFailed(Long taskId, String errorMessage) {
        String url = resolveCallbackUrl(taskId);
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", taskId);
        body.put("status", "failed");
        body.put("errorMessage", errorMessage);

        callWithRetry(url, body);
    }

    private void callWithRetry(String url, Map<String, Object> body) {
        int attempt = 0;
        long backoffMs = CALLBACK_BACKOFF_MS;

        while (true) {
            attempt++;
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                restTemplate.postForEntity(url, entity, Void.class);
                log.info("Callback sent to task-service: {}", url);
                return;
            } catch (Exception e) {
                if (attempt >= MAX_CALLBACK_RETRIES) {
                    log.error("Failed to callback task-service after {} attempts: {}",
                            MAX_CALLBACK_RETRIES, e.getMessage());
                    return;
                }
                log.warn("Callback failed (attempt {}/{}), retrying in {}ms: {}",
                        attempt, MAX_CALLBACK_RETRIES, backoffMs, e.getMessage());
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                backoffMs *= 2;
            }
        }
    }

    private String resolveCallbackUrl(Long taskId) {
        return composeProperties.getCallbackUrl().replace("{taskId}", taskId.toString());
    }
}
