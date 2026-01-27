package com.shopvideoscout.task.mq;

import com.shopvideoscout.common.mq.ComposeMessage;
import com.shopvideoscout.common.mq.MqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes compose messages to RabbitMQ for media-service consumption.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComposeMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish a compose message to the compose queue.
     *
     * @param message the compose message
     */
    public void publish(ComposeMessage message) {
        log.info("Publishing compose message for task: {}", message.getTaskId());
        rabbitTemplate.convertAndSend(
                MqConstants.COMPOSE_EXCHANGE,
                MqConstants.COMPOSE_ROUTING_KEY,
                message
        );
        log.debug("Compose message published for task: {}", message.getTaskId());
    }
}
