package com.shopvideoscout.media.mq;

import com.shopvideoscout.common.mq.ComposeMessage;
import com.shopvideoscout.common.mq.MqConstants;
import com.shopvideoscout.media.service.ComposeProgressTracker;
import com.shopvideoscout.media.service.TaskCallbackClient;
import com.shopvideoscout.media.service.TtsSynthesisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes compose messages from RabbitMQ and triggers TTS synthesis.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComposeMessageConsumer {

    private final TtsSynthesisService ttsSynthesisService;
    private final TaskCallbackClient taskCallbackClient;
    private final ComposeProgressTracker progressTracker;

    @RabbitListener(queues = MqConstants.COMPOSE_QUEUE)
    public void handleComposeMessage(ComposeMessage message) {
        Long taskId = message.getTaskId();
        log.info("Received compose message for task: {}", taskId);

        try {
            TtsSynthesisService.SynthesisResult result = ttsSynthesisService.synthesize(message);
            log.info("TTS synthesis completed for task {}: {} paragraphs, total duration: {}s",
                    taskId, result.getParagraphResults().size(), result.getTotalDurationSeconds());

            // Callback to task-service to update status
            taskCallbackClient.notifyComposeComplete(taskId, result);

        } catch (Exception e) {
            log.error("Compose failed for task {}: {}", taskId, e.getMessage());
            progressTracker.markFailed(taskId, e.getMessage());
            taskCallbackClient.notifyComposeFailed(taskId, e.getMessage());
        }
    }
}
