package com.shopvideoscout.media.mq;

import com.shopvideoscout.common.mq.ComposeMessage;
import com.shopvideoscout.common.mq.MqConstants;
import com.shopvideoscout.media.service.ComposeProgressTracker;
import com.shopvideoscout.media.service.SubtitleGenerationService;
import com.shopvideoscout.media.service.TaskCallbackClient;
import com.shopvideoscout.media.service.TtsSynthesisService;
import com.shopvideoscout.media.service.VideoCompositionService;
import com.shopvideoscout.media.service.VideoSegmentCuttingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Consumes compose messages from RabbitMQ and orchestrates the full video composition pipeline.
 *
 * Pipeline (Story 4.3):
 * 1. TTS Synthesis → paragraph audio URLs + actual durations
 * 2. Subtitle Generation → ASS file (if enabled)
 * 3. Video Segment Cutting → segment files per paragraph
 * 4. Video Composition → concat + audio + subtitle burn
 * 5. Output Upload → final video to OSS
 * 6. Callback → notify task-service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComposeMessageConsumer {

    private final TtsSynthesisService ttsSynthesisService;
    private final SubtitleGenerationService subtitleGenerationService;
    private final VideoSegmentCuttingService videoSegmentCuttingService;
    private final VideoCompositionService videoCompositionService;
    private final TaskCallbackClient taskCallbackClient;
    private final ComposeProgressTracker progressTracker;

    @RabbitListener(queues = MqConstants.COMPOSE_QUEUE)
    public void handleComposeMessage(ComposeMessage message) {
        Long taskId = message.getTaskId();
        log.info("Received compose message for task: {}", taskId);

        List<VideoSegmentCuttingService.SegmentResult> segments = new ArrayList<>();
        File subtitleFile = null;

        try {
            // Phase 1: TTS Synthesis
            progressTracker.updatePhase(taskId, ComposeProgressTracker.PHASE_TTS_SYNTHESIS, "TTS配音合成");
            TtsSynthesisService.SynthesisResult ttsResult = ttsSynthesisService.synthesize(message);
            log.info("Phase 1 complete - TTS synthesis for task {}: {} paragraphs, duration: {}s",
                    taskId, ttsResult.getParagraphResults().size(), ttsResult.getTotalDurationSeconds());

            // Convert TTS results to ParagraphDuration list
            List<VideoSegmentCuttingService.ParagraphDuration> paragraphDurations =
                    convertToParagraphDurations(message.getParagraphs(), ttsResult.getParagraphResults());

            // Phase 2: Subtitle Generation (if enabled)
            Boolean subtitleEnabled = message.getSubtitleEnabled() != null ? message.getSubtitleEnabled() : true;
            if (subtitleEnabled) {
                progressTracker.updatePhase(taskId, ComposeProgressTracker.PHASE_SUBTITLE_GENERATION, "生成字幕");
                try {
                    subtitleFile = subtitleGenerationService.generateSubtitle(
                            paragraphDurations,
                            message.getSubtitleStyle(),
                            taskId);
                    if (subtitleFile != null) {
                        subtitleGenerationService.uploadToOss(subtitleFile, taskId);
                        log.info("Phase 2 complete - Subtitle generated for task {}", taskId);
                    }
                } catch (Exception e) {
                    // Graceful degradation: log warning and continue without subtitles
                    log.warn("Subtitle generation failed for task {}, continuing without subtitles: {}",
                            taskId, e.getMessage());
                    subtitleFile = null;
                }
            } else {
                log.info("Phase 2 skipped - Subtitles disabled for task {}", taskId);
            }

            // Phase 3: Video Segment Cutting
            progressTracker.updatePhase(taskId, ComposeProgressTracker.PHASE_VIDEO_CUTTING, "裁剪视频片段");
            segments = videoSegmentCuttingService.cutSegments(taskId, paragraphDurations);
            log.info("Phase 3 complete - {} segments cut for task {}", segments.size(), taskId);

            // Phase 4: Video Composition
            progressTracker.updatePhase(taskId, ComposeProgressTracker.PHASE_VIDEO_COMPOSITION, "合成视频");
            List<String> audioUrls = ttsResult.getParagraphResults().stream()
                    .map(TtsSynthesisService.ParagraphResult::getAudioUrl)
                    .collect(Collectors.toList());
            VideoCompositionService.CompositionResult compositionResult =
                    videoCompositionService.compose(taskId, segments, audioUrls, subtitleFile);
            log.info("Phase 4 complete - Video composed for task {}: duration={}s, size={}",
                    taskId, compositionResult.getDurationSeconds(), compositionResult.getFileSizeBytes());

            // Phase 5: Output Upload
            progressTracker.updatePhase(taskId, ComposeProgressTracker.PHASE_OUTPUT_UPLOAD, "上传视频");
            String outputOssKey = videoCompositionService.uploadToOss(
                    compositionResult.getOutputFile(), taskId);
            log.info("Phase 5 complete - Output uploaded for task {}: {}", taskId, outputOssKey);

            // Phase 6: Callback
            progressTracker.markComplete(taskId);
            taskCallbackClient.notifyComposeCompleteWithOutput(
                    taskId,
                    outputOssKey,
                    (int) compositionResult.getDurationSeconds(),
                    compositionResult.getFileSizeBytes());
            log.info("Composition pipeline completed for task {}", taskId);

        } catch (Exception e) {
            log.error("Compose failed for task {}: {}", taskId, e.getMessage(), e);
            progressTracker.markFailed(taskId, e.getMessage());
            taskCallbackClient.notifyComposeFailed(taskId, e.getMessage());
        } finally {
            // Cleanup
            if (!segments.isEmpty()) {
                videoSegmentCuttingService.cleanupSegments(segments);
            }
            videoCompositionService.cleanup(taskId);
        }
    }

    /**
     * Convert TTS results to ParagraphDuration list for downstream processing.
     */
    private List<VideoSegmentCuttingService.ParagraphDuration> convertToParagraphDurations(
            List<ComposeMessage.Paragraph> messageParagraphs,
            List<TtsSynthesisService.ParagraphResult> ttsResults) {

        List<VideoSegmentCuttingService.ParagraphDuration> result = new ArrayList<>();

        for (int i = 0; i < messageParagraphs.size() && i < ttsResults.size(); i++) {
            ComposeMessage.Paragraph mp = messageParagraphs.get(i);
            TtsSynthesisService.ParagraphResult tr = ttsResults.get(i);

            result.add(VideoSegmentCuttingService.ParagraphDuration.builder()
                    .paragraphIndex(i)
                    .shotId(mp.getShotId())
                    .text(mp.getText())
                    .audioUrl(tr.getAudioUrl())
                    .actualDurationSeconds(tr.getDurationSeconds())
                    .build());
        }

        return result;
    }
}
