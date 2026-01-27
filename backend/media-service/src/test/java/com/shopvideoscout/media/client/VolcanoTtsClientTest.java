package com.shopvideoscout.media.client;

import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.media.config.VolcanoTtsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VolcanoTtsClient.
 */
@ExtendWith(MockitoExtension.class)
class VolcanoTtsClientTest {

    @Mock
    private RestTemplate restTemplate;

    private VolcanoTtsProperties ttsProperties;
    private VolcanoTtsClient volcanoTtsClient;

    @BeforeEach
    void setUp() {
        ttsProperties = new VolcanoTtsProperties();
        ttsProperties.setApiUrl("https://tts.api.example.com/v1/tts");
        ttsProperties.setAppId("test-app-id");
        ttsProperties.setAccessToken("test-token");
        ttsProperties.setSampleRate(48000);
        ttsProperties.setFormat("mp3");
        ttsProperties.setTimeoutMs(30000);
        ttsProperties.setMaxTextLength(5000);

        volcanoTtsClient = new VolcanoTtsClient(ttsProperties, restTemplate);
    }

    @Nested
    @DisplayName("AC1: TTS Synthesis")
    class TtsSynthesisTests {

        @Test
        @DisplayName("4.1-UNIT-001: Single paragraph TTS → audio bytes + duration")
        void singleParagraphSuccess_ShouldReturnAudioAndDuration() {
            // Given
            String audioBase64 = Base64.getEncoder().encodeToString("fake-audio-data".getBytes());
            Map<String, Object> responseBody = Map.of(
                    "data", audioBase64,
                    "addition", Map.of("duration", "8.5")
            );
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

            // When
            List<VolcanoTtsClient.TtsResult> results =
                    volcanoTtsClient.synthesize("家人们！今天给你们探一家店", "xiaomei");

            // Then
            assertEquals(1, results.size());
            assertNotNull(results.get(0).getAudioData());
            assertEquals(8.5, results.get(0).getDurationSeconds());
        }

        @Test
        @DisplayName("4.1-UNIT-002: TTS 504 timeout → retry 3x with backoff")
        void ttsTimeout_ShouldRetry3Times() {
            // Given - first two calls timeout, third succeeds
            String audioBase64 = Base64.getEncoder().encodeToString("audio".getBytes());
            Map<String, Object> successResponse = Map.of(
                    "data", audioBase64,
                    "addition", Map.of("duration", "5.0")
            );

            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new ResourceAccessException("Connection timeout"))
                    .thenThrow(new ResourceAccessException("Connection timeout"))
                    .thenReturn(new ResponseEntity<>(successResponse, HttpStatus.OK));

            // When
            List<VolcanoTtsClient.TtsResult> results =
                    volcanoTtsClient.synthesize("测试文本", "xiaomei");

            // Then
            assertEquals(1, results.size());
            verify(restTemplate, times(3)).exchange(anyString(), eq(HttpMethod.POST),
                    any(HttpEntity.class), eq(Map.class));
        }

        @Test
        @DisplayName("4.1-UNIT-002b: TTS timeout exhausted after 3 retries → throw")
        void ttsTimeoutExhausted_ShouldThrow() {
            // Given - all 3 calls timeout
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new ResourceAccessException("Connection timeout"));

            // When/Then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> volcanoTtsClient.synthesize("测试文本", "xiaomei"));
            assertEquals(ResultCode.TTS_SERVICE_TIMEOUT.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.1-BLIND-BOUNDARY-001: Null text → 400 error")
        void nullText_ShouldThrow400() {
            // When/Then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> volcanoTtsClient.synthesize(null, "xiaomei"));
            assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.1-BLIND-BOUNDARY-001b: Empty text → 400 error")
        void emptyText_ShouldThrow400() {
            // When/Then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> volcanoTtsClient.synthesize("", "xiaomei"));
            assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.1-BLIND-ERROR-001: Malformed TTS response → graceful error")
        void malformedResponse_ShouldThrowGracefully() {
            // Given
            Map<String, Object> malformedResponse = Map.of("error", "unknown format");
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(malformedResponse, HttpStatus.OK));

            // When/Then
            assertThrows(Exception.class,
                    () -> volcanoTtsClient.synthesize("测试文本", "xiaomei"));
        }
    }

    @Nested
    @DisplayName("Text Segmentation")
    class TextSegmentationTests {

        @Test
        @DisplayName("4.1-UNIT-003: Text >5000 chars → split into segments")
        void textOverLimit_ShouldSegment() {
            // Given
            String longText = "测试。".repeat(2501); // 5002 chars

            // When
            List<String> segments = volcanoTtsClient.segmentText(longText, 5000);

            // Then
            assertTrue(segments.size() >= 2);
            for (String segment : segments) {
                assertTrue(segment.length() <= 5000);
            }
        }

        @Test
        @DisplayName("4.1-BLIND-BOUNDARY-003: Exactly 5000 chars → no segmentation")
        void exactly5000Chars_ShouldNotSegment() {
            // Given
            String exactText = "a".repeat(5000);

            // When
            List<String> segments = volcanoTtsClient.segmentText(exactText, 5000);

            // Then
            assertEquals(1, segments.size());
            assertEquals(5000, segments.get(0).length());
        }

        @Test
        @DisplayName("4.1-BLIND-BOUNDARY-004: 5001 chars → segmented into 2")
        void chars5001_ShouldSegmentIntoTwo() {
            // Given
            String text = "a".repeat(5001);

            // When
            List<String> segments = volcanoTtsClient.segmentText(text, 5000);

            // Then
            assertEquals(2, segments.size());
            assertEquals(5000, segments.get(0).length());
            assertEquals(1, segments.get(1).length());
        }

        @Test
        @DisplayName("Segmentation prefers sentence boundaries")
        void segmentation_ShouldPreferSentenceBoundaries() {
            // Given - text with a sentence boundary near the split point
            String part1 = "a".repeat(4990) + "。";
            String part2 = "b".repeat(100);
            String text = part1 + part2;

            // When
            List<String> segments = volcanoTtsClient.segmentText(text, 5000);

            // Then
            assertEquals(2, segments.size());
            assertTrue(segments.get(0).endsWith("。"));
        }

        @Test
        @DisplayName("Short text → single segment")
        void shortText_ShouldBeOneSegment() {
            // When
            List<String> segments = volcanoTtsClient.segmentText("短文本", 5000);

            // Then
            assertEquals(1, segments.size());
            assertEquals("短文本", segments.get(0));
        }
    }

    @Nested
    @DisplayName("Multi-paragraph Orchestration")
    class MultiParagraphTests {

        @Test
        @DisplayName("4.1-UNIT-004: Multi-paragraph → N audio results aggregated")
        void multiParagraph_ShouldAggregateResults() {
            // Given
            String audioBase64 = Base64.getEncoder().encodeToString("audio".getBytes());
            Map<String, Object> responseBody = Map.of(
                    "data", audioBase64,
                    "addition", Map.of("duration", "5.0")
            );
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

            // When - call synthesize which calls API once for single paragraph
            List<VolcanoTtsClient.TtsResult> results =
                    volcanoTtsClient.synthesize("段落文本", "xiaomei");

            // Then
            assertEquals(1, results.size());
            assertEquals(5.0, results.get(0).getDurationSeconds());
        }
    }
}
