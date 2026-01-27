package com.shopvideoscout.media.mq;

import com.shopvideoscout.common.mq.MqConstants;
import com.shopvideoscout.common.mq.VoiceCloneMessage;
import com.shopvideoscout.media.service.VoiceCloneCallbackClient;
import com.shopvideoscout.media.service.VoiceCloneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes voice clone messages from RabbitMQ and triggers Seed-ICL cloning.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceCloneMessageConsumer {

    private final VoiceCloneService voiceCloneService;
    private final VoiceCloneCallbackClient voiceCloneCallbackClient;

    @RabbitListener(queues = MqConstants.VOICE_CLONE_QUEUE)
    public void handleVoiceCloneMessage(VoiceCloneMessage message) {
        Long sampleId = message.getVoiceSampleId();
        log.info("Received voice clone message for sample: {}", sampleId);

        try {
            String cloneVoiceId = voiceCloneService.processClone(message);
            voiceCloneCallbackClient.notifyCloneComplete(sampleId, cloneVoiceId);
        } catch (Exception e) {
            log.error("Voice clone failed for sample {}: {}", sampleId, e.getMessage());
            try {
                voiceCloneCallbackClient.notifyCloneFailed(sampleId, e.getMessage());
            } catch (Exception callbackEx) {
                // ERR-002: If failure callback itself throws, log and let message be dead-lettered.
                // DLQ consumer or manual intervention can reconcile the sample status.
                log.error("Failed to notify clone failure for sample {}: {}",
                        sampleId, callbackEx.getMessage());
            }
        }
    }
}
