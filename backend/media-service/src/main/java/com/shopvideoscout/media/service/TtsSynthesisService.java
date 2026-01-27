package com.shopvideoscout.media.service;

import com.aliyun.oss.OSS;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.mq.ComposeMessage;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.media.client.VolcanoTtsClient;
import com.shopvideoscout.media.config.OssConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates paragraph-by-paragraph TTS synthesis.
 * Calls VolcanoTtsClient for each paragraph, uploads audio to OSS,
 * and tracks progress via ComposeProgressTracker.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsSynthesisService {

    private final VolcanoTtsClient volcanoTtsClient;
    private final OSS ossClient;
    private final OssConfig ossConfig;
    private final ComposeProgressTracker progressTracker;

    /**
     * Synthesize all paragraphs for a compose message.
     *
     * @param message the compose message containing paragraphs and voice config
     * @return synthesis result with audio URLs and durations
     */
    public SynthesisResult synthesize(ComposeMessage message) {
        List<ComposeMessage.Paragraph> paragraphs = message.getParagraphs();
        Long taskId = message.getTaskId();
        String voiceId = message.getVoiceConfig().getVoiceId();

        log.info("Starting TTS synthesis for task {}: {} paragraphs", taskId, paragraphs.size());
        progressTracker.initProgress(taskId, paragraphs.size());

        List<ParagraphResult> results = new ArrayList<>();
        double totalDuration = 0.0;
        int completedCount = 0;
        List<Double> durations = new ArrayList<>();

        for (ComposeMessage.Paragraph paragraph : paragraphs) {
            try {
                ParagraphResult result = synthesizeParagraph(taskId, paragraph, voiceId);
                results.add(result);
                totalDuration += result.getDurationSeconds();
                completedCount++;
                durations.add(result.getDurationSeconds());

                double avgDuration = durations.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(10.0);
                progressTracker.updateParagraphComplete(
                        taskId, completedCount, paragraphs.size(), avgDuration);

            } catch (Exception e) {
                log.error("Failed to synthesize paragraph {} for task {}: {}",
                        paragraph.getIndex(), taskId, e.getMessage());
                // Single-paragraph retry: try once more for isolated failure
                try {
                    ParagraphResult retryResult = synthesizeParagraph(taskId, paragraph, voiceId);
                    results.add(retryResult);
                    totalDuration += retryResult.getDurationSeconds();
                    completedCount++;
                    durations.add(retryResult.getDurationSeconds());

                    double avgDuration = durations.stream()
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(10.0);
                    progressTracker.updateParagraphComplete(
                            taskId, completedCount, paragraphs.size(), avgDuration);
                } catch (Exception retryEx) {
                    log.error("Retry failed for paragraph {} of task {}: {}",
                            paragraph.getIndex(), taskId, retryEx.getMessage());
                    progressTracker.markFailed(taskId,
                            "第" + (paragraph.getIndex() + 1) + "段配音失败: " + retryEx.getMessage());
                    throw new BusinessException(ResultCode.TTS_SERVICE_ERROR,
                            "第" + (paragraph.getIndex() + 1) + "段配音失败，已重试");
                }
            }
        }

        progressTracker.markComplete(taskId);

        return SynthesisResult.builder()
                .taskId(taskId)
                .paragraphResults(results)
                .totalDurationSeconds(totalDuration)
                .build();
    }

    /**
     * Synthesize a single paragraph and upload to OSS.
     */
    private ParagraphResult synthesizeParagraph(Long taskId,
                                                 ComposeMessage.Paragraph paragraph,
                                                 String voiceId) {
        List<VolcanoTtsClient.TtsResult> ttsResults =
                volcanoTtsClient.synthesize(paragraph.getText(), voiceId);

        // Merge audio segments if text was split
        byte[] mergedAudio = mergeAudioSegments(ttsResults);
        double totalDuration = ttsResults.stream()
                .mapToDouble(VolcanoTtsClient.TtsResult::getDurationSeconds)
                .sum();

        // Upload to OSS
        String ossKey = String.format("audio/%d/tts_%d.mp3", taskId, paragraph.getIndex());
        uploadToOss(ossKey, mergedAudio);

        String audioUrl = String.format("https://%s.%s/%s",
                ossConfig.getBucketName(), ossConfig.getEndpoint(), ossKey);

        return ParagraphResult.builder()
                .paragraphIndex(paragraph.getIndex())
                .audioUrl(audioUrl)
                .ossKey(ossKey)
                .durationSeconds(totalDuration)
                .build();
    }

    /**
     * Merge multiple audio byte arrays into one.
     * For MP3 format, simple concatenation works.
     */
    private byte[] mergeAudioSegments(List<VolcanoTtsClient.TtsResult> results) {
        if (results.size() == 1) {
            return results.get(0).getAudioData();
        }

        int totalSize = results.stream()
                .mapToInt(r -> r.getAudioData().length)
                .sum();

        byte[] merged = new byte[totalSize];
        int offset = 0;
        for (VolcanoTtsClient.TtsResult result : results) {
            System.arraycopy(result.getAudioData(), 0, merged, offset, result.getAudioData().length);
            offset += result.getAudioData().length;
        }

        return merged;
    }

    /**
     * Upload audio bytes to OSS.
     */
    private void uploadToOss(String ossKey, byte[] audioData) {
        try {
            ossClient.putObject(
                    ossConfig.getBucketName(),
                    ossKey,
                    new ByteArrayInputStream(audioData)
            );
            log.debug("Uploaded audio to OSS: {}", ossKey);
        } catch (Exception e) {
            log.error("Failed to upload audio to OSS: {}", e.getMessage());
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE,
                    "音频上传失败: " + e.getMessage());
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SynthesisResult {
        private Long taskId;
        private List<ParagraphResult> paragraphResults;
        private double totalDurationSeconds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParagraphResult {
        private int paragraphIndex;
        private String audioUrl;
        private String ossKey;
        private double durationSeconds;
    }
}
