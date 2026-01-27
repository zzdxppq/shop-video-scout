package com.shopvideoscout.media.service;

import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.mq.VoiceCloneMessage;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.media.client.VolcanoTtsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service handling voice clone processing via Volcano Seed-ICL API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceCloneService {

    private final VolcanoTtsClient volcanoTtsClient;

    private static final int MAX_CLONE_RETRIES = 2;
    private static final long INITIAL_BACKOFF_MS = 1000;

    /**
     * Process voice cloning request.
     * Calls Seed-ICL API to create a clone voice from the audio sample.
     * Retries with exponential backoff on failure (ERR-001).
     *
     * @param message the voice clone MQ message
     * @return the clone_voice_id from Seed-ICL
     * @throws BusinessException if cloning fails after retries
     */
    public String processClone(VoiceCloneMessage message) {
        log.info("Processing voice clone for sample {}, user {}", message.getVoiceSampleId(), message.getUserId());

        int attempt = 0;
        Exception lastException = null;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (attempt <= MAX_CLONE_RETRIES) {
            attempt++;
            try {
                String cloneVoiceId = volcanoTtsClient.cloneVoice(message.getOssKey());
                log.info("Voice clone succeeded for sample {}: cloneVoiceId={}", message.getVoiceSampleId(), cloneVoiceId);
                return cloneVoiceId;
            } catch (Exception e) {
                lastException = e;
                if (attempt <= MAX_CLONE_RETRIES) {
                    log.warn("Voice clone attempt {}/{} failed for sample {}: {}, retrying in {}ms...",
                            attempt, MAX_CLONE_RETRIES + 1, message.getVoiceSampleId(), e.getMessage(), backoffMs);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    backoffMs *= 2;
                }
            }
        }

        log.error("Voice clone failed for sample {} after {} attempts",
                message.getVoiceSampleId(), MAX_CLONE_RETRIES + 1);
        throw new BusinessException(ResultCode.VOICE_CLONE_FAILED,
                "声音克隆失败: " + (lastException != null ? lastException.getMessage() : "unknown error"));
    }
}
