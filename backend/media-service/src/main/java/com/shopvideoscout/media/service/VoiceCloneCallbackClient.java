package com.shopvideoscout.media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for calling back to user-service after voice clone processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceCloneCallbackClient {

    private final RestTemplate restTemplate;

    @Value("${voice-clone.callback-url:http://user-service/internal/voice/samples/{sampleId}/clone-result}")
    private String callbackUrlTemplate;

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 2000;

    /**
     * Notify user-service that voice clone completed successfully.
     */
    public void notifyCloneComplete(Long sampleId, String cloneVoiceId) {
        Map<String, Object> body = new HashMap<>();
        body.put("cloneVoiceId", cloneVoiceId);
        body.put("status", "completed");
        callWithRetry(sampleId, body);
    }

    /**
     * Notify user-service that voice clone failed.
     */
    public void notifyCloneFailed(Long sampleId, String errorMessage) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "failed");
        body.put("errorMessage", errorMessage);
        callWithRetry(sampleId, body);
    }

    private void callWithRetry(Long sampleId, Map<String, Object> body) {
        String url = callbackUrlTemplate.replace("{sampleId}", sampleId.toString());
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (true) {
            attempt++;
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                restTemplate.postForEntity(url, entity, Void.class);
                log.info("Clone callback sent to user-service for sample {}: {}", sampleId, body.get("status"));
                return;
            } catch (Exception e) {
                if (attempt >= MAX_RETRIES) {
                    log.error("Failed to callback user-service for sample {} after {} attempts: {}",
                            sampleId, MAX_RETRIES, e.getMessage());
                    return;
                }
                log.warn("Clone callback failed (attempt {}/{}), retrying in {}ms: {}",
                        attempt, MAX_RETRIES, backoffMs, e.getMessage());
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
}
