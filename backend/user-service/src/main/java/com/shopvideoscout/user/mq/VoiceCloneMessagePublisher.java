package com.shopvideoscout.user.mq;

import com.shopvideoscout.common.mq.MqConstants;
import com.shopvideoscout.common.mq.VoiceCloneMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes voice clone messages to RabbitMQ for media-service consumption.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceCloneMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish a voice clone message to the voice clone queue.
     */
    public void publish(VoiceCloneMessage message) {
        log.info("Publishing voice clone message for sample: {}", message.getVoiceSampleId());
        rabbitTemplate.convertAndSend(
                MqConstants.VOICE_CLONE_EXCHANGE,
                MqConstants.VOICE_CLONE_ROUTING_KEY,
                message
        );
        log.debug("Voice clone message published for sample: {}", message.getVoiceSampleId());
    }
}
