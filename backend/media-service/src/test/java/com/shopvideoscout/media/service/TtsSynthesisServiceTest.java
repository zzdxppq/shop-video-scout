package com.shopvideoscout.media.service;

import com.aliyun.oss.OSS;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.mq.ComposeMessage;
import com.shopvideoscout.media.client.VolcanoTtsClient;
import com.shopvideoscout.media.config.OssConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TtsSynthesisService.
 */
@ExtendWith(MockitoExtension.class)
class TtsSynthesisServiceTest {

    @Mock
    private VolcanoTtsClient volcanoTtsClient;

    @Mock
    private OSS ossClient;

    @Mock
    private OssConfig ossConfig;

    @Mock
    private ComposeProgressTracker progressTracker;

    @InjectMocks
    private TtsSynthesisService ttsSynthesisService;

    private ComposeMessage mockMessage;

    @BeforeEach
    void setUp() {
        mockMessage = ComposeMessage.builder()
                .taskId(1L)
                .paragraphs(List.of(
                        ComposeMessage.Paragraph.builder().index(0).text("段落一").build(),
                        ComposeMessage.Paragraph.builder().index(1).text("段落二").build(),
                        ComposeMessage.Paragraph.builder().index(2).text("段落三").build()
                ))
                .voiceConfig(ComposeMessage.VoiceConfig.builder()
                        .type("standard")
                        .voiceId("xiaomei")
                        .build())
                .callbackUrl("http://task-service/internal/tasks/1/compose-complete")
                .build();
    }

    @Nested
    @DisplayName("AC1: TTS Synthesis Orchestration")
    class SynthesisOrchestrationTests {

        @Test
        @DisplayName("4.1-UNIT-004: Multi-paragraph orchestration → aggregate results")
        void multiParagraphSynthesis_ShouldAggregateResults() {
            // Given
            VolcanoTtsClient.TtsResult ttsResult = VolcanoTtsClient.TtsResult.builder()
                    .audioData("audio-bytes".getBytes())
                    .durationSeconds(8.0)
                    .build();

            when(volcanoTtsClient.synthesize(anyString(), eq("xiaomei")))
                    .thenReturn(List.of(ttsResult));
            when(ossConfig.getBucketName()).thenReturn("test-bucket");
            when(ossConfig.getEndpoint()).thenReturn("oss.example.com");

            // When
            TtsSynthesisService.SynthesisResult result =
                    ttsSynthesisService.synthesize(mockMessage);

            // Then
            assertEquals(1L, result.getTaskId());
            assertEquals(3, result.getParagraphResults().size());
            assertEquals(24.0, result.getTotalDurationSeconds()); // 3 × 8.0
            verify(volcanoTtsClient, times(3)).synthesize(anyString(), eq("xiaomei"));
            verify(ossClient, times(3)).putObject(eq("test-bucket"), anyString(), any(InputStream.class));
        }

        @Test
        @DisplayName("4.1-UNIT-005: Audio upload to OSS path audio/{task_id}/tts_{idx}.mp3")
        void audioUpload_ShouldUseCorrectOssPath() {
            // Given
            VolcanoTtsClient.TtsResult ttsResult = VolcanoTtsClient.TtsResult.builder()
                    .audioData("audio".getBytes())
                    .durationSeconds(5.0)
                    .build();

            when(volcanoTtsClient.synthesize(anyString(), eq("xiaomei")))
                    .thenReturn(List.of(ttsResult));
            when(ossConfig.getBucketName()).thenReturn("test-bucket");
            when(ossConfig.getEndpoint()).thenReturn("oss.example.com");

            // When
            ttsSynthesisService.synthesize(mockMessage);

            // Then
            verify(ossClient).putObject(eq("test-bucket"), eq("audio/1/tts_0.mp3"), any(InputStream.class));
            verify(ossClient).putObject(eq("test-bucket"), eq("audio/1/tts_1.mp3"), any(InputStream.class));
            verify(ossClient).putObject(eq("test-bucket"), eq("audio/1/tts_2.mp3"), any(InputStream.class));
        }

        @Test
        @DisplayName("4.1-UNIT-011: Update progress after each paragraph completion")
        void progressUpdate_ShouldCallTrackerForEachParagraph() {
            // Given
            VolcanoTtsClient.TtsResult ttsResult = VolcanoTtsClient.TtsResult.builder()
                    .audioData("audio".getBytes())
                    .durationSeconds(10.0)
                    .build();

            when(volcanoTtsClient.synthesize(anyString(), eq("xiaomei")))
                    .thenReturn(List.of(ttsResult));
            when(ossConfig.getBucketName()).thenReturn("test-bucket");
            when(ossConfig.getEndpoint()).thenReturn("oss.example.com");

            // When
            ttsSynthesisService.synthesize(mockMessage);

            // Then
            verify(progressTracker).initProgress(1L, 3);
            verify(progressTracker).updateParagraphComplete(eq(1L), eq(1), eq(3), anyDouble());
            verify(progressTracker).updateParagraphComplete(eq(1L), eq(2), eq(3), anyDouble());
            verify(progressTracker).updateParagraphComplete(eq(1L), eq(3), eq(3), anyDouble());
            verify(progressTracker).markComplete(1L);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("4.1-UNIT-013: Single paragraph failure → retry without blocking others")
        void singleParagraphFailure_ShouldRetryIsolated() {
            // Given - first paragraph fails once then succeeds
            VolcanoTtsClient.TtsResult successResult = VolcanoTtsClient.TtsResult.builder()
                    .audioData("audio".getBytes())
                    .durationSeconds(5.0)
                    .build();

            when(volcanoTtsClient.synthesize(eq("段落一"), eq("xiaomei")))
                    .thenThrow(new RuntimeException("Temporary failure"))
                    .thenReturn(List.of(successResult));
            when(volcanoTtsClient.synthesize(eq("段落二"), eq("xiaomei")))
                    .thenReturn(List.of(successResult));
            when(volcanoTtsClient.synthesize(eq("段落三"), eq("xiaomei")))
                    .thenReturn(List.of(successResult));
            when(ossConfig.getBucketName()).thenReturn("test-bucket");
            when(ossConfig.getEndpoint()).thenReturn("oss.example.com");

            // When
            TtsSynthesisService.SynthesisResult result =
                    ttsSynthesisService.synthesize(mockMessage);

            // Then - all 3 paragraphs completed (first one via retry)
            assertEquals(3, result.getParagraphResults().size());
            verify(progressTracker).markComplete(1L);
        }

        @Test
        @DisplayName("4.1-BLIND-ERROR-002: OSS upload fails → mark failed")
        void ossUploadFails_ShouldMarkFailed() {
            // Given
            VolcanoTtsClient.TtsResult ttsResult = VolcanoTtsClient.TtsResult.builder()
                    .audioData("audio".getBytes())
                    .durationSeconds(5.0)
                    .build();

            when(volcanoTtsClient.synthesize(anyString(), eq("xiaomei")))
                    .thenReturn(List.of(ttsResult));
            when(ossConfig.getBucketName()).thenReturn("test-bucket");
            when(ossClient.putObject(eq("test-bucket"), anyString(), any(InputStream.class)))
                    .thenThrow(new RuntimeException("OSS unavailable"));

            // When/Then
            assertThrows(BusinessException.class,
                    () -> ttsSynthesisService.synthesize(mockMessage));
        }

        @Test
        @DisplayName("Paragraph failure + retry failure → mark failed with error")
        void paragraphDoubleFailure_ShouldMarkFailed() {
            // Given - paragraph fails both initial and retry
            when(volcanoTtsClient.synthesize(eq("段落一"), eq("xiaomei")))
                    .thenThrow(new RuntimeException("TTS down"));
            when(ossConfig.getBucketName()).thenReturn("test-bucket");
            when(ossConfig.getEndpoint()).thenReturn("oss.example.com");

            // When/Then
            assertThrows(BusinessException.class,
                    () -> ttsSynthesisService.synthesize(mockMessage));
            verify(progressTracker).markFailed(eq(1L), anyString());
        }
    }
}
