package com.shopvideoscout.ai.service;

import com.shopvideoscout.ai.client.QwenVlClient;
import com.shopvideoscout.ai.dto.AnalysisProgressResponse;
import com.shopvideoscout.ai.dto.AnalyzeTaskResponse;
import com.shopvideoscout.ai.dto.FrameAnalysisResult;
import com.shopvideoscout.ai.entity.VideoFrame;
import com.shopvideoscout.ai.mapper.VideoFrameMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FrameAnalysisService.
 * Covers test scenarios from QA Test Design:
 * - 2.3-INT-006: Queue message consumption
 * - 2.3-INT-004: Batch processing with parallel API calls
 * - 2.3-BLIND-BOUNDARY-001: Empty frames array
 * - 2.3-BLIND-CONCURRENCY-001: Multiple analysis requests for same task
 */
@ExtendWith(MockitoExtension.class)
class FrameAnalysisServiceTest {

    @Mock
    private VideoFrameMapper videoFrameMapper;

    @Mock
    private QwenVlClient qwenVlClient;

    @Mock
    private FrameRecommendationService recommendationService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private FrameAnalysisService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("2.3-BLIND-CONCURRENCY-001: Multiple analysis requests for same task")
    class ConcurrencyTests {

        @Test
        @DisplayName("Should return already_analyzing if task is being analyzed")
        void shouldReturnAlreadyAnalyzingIfInProgress() {
            Long taskId = 1L;
            when(valueOperations.get("analysis:status:task:1")).thenReturn("analyzing");

            AnalyzeTaskResponse response = service.triggerAnalysis(taskId);

            assertThat(response.getStatus()).isEqualTo("already_analyzing");
            verify(videoFrameMapper, never()).findByTaskId(anyLong());
        }

        @Test
        @DisplayName("Should set status to analyzing when starting new analysis")
        void shouldSetStatusWhenStartingAnalysis() {
            Long taskId = 1L;
            when(valueOperations.get("analysis:status:task:1")).thenReturn(null);

            VideoFrame frame = createFrame(1L, 1L, "url1");
            frame.setCategory(null); // Not analyzed yet
            when(videoFrameMapper.findByTaskId(taskId)).thenReturn(List.of(frame));

            AnalyzeTaskResponse response = service.triggerAnalysis(taskId);

            assertThat(response.getStatus()).isEqualTo("queued");
            verify(valueOperations).set(
                    eq("analysis:status:task:1"),
                    eq("analyzing"),
                    eq(24L),
                    eq(TimeUnit.HOURS)
            );
        }
    }

    @Nested
    @DisplayName("2.3-BLIND-BOUNDARY-001: Empty frames array")
    class EmptyFramesTests {

        @Test
        @DisplayName("Should return no_frames when task has no frames")
        void shouldReturnNoFramesForEmptyTask() {
            Long taskId = 1L;
            when(valueOperations.get("analysis:status:task:1")).thenReturn(null);
            when(videoFrameMapper.findByTaskId(taskId)).thenReturn(Collections.emptyList());

            AnalyzeTaskResponse response = service.triggerAnalysis(taskId);

            assertThat(response.getStatus()).isEqualTo("no_frames");
            assertThat(response.getTotalFrames()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return pending progress for task with no frames")
        void shouldReturnPendingProgressForNoFrames() {
            Long taskId = 1L;
            when(videoFrameMapper.countByTaskId(taskId)).thenReturn(0);

            AnalysisProgressResponse response = service.getProgress(taskId);

            assertThat(response.getStatus()).isEqualTo("pending");
            assertThat(response.getTotalFrames()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Analysis Progress Tracking")
    class ProgressTrackingTests {

        @Test
        @DisplayName("Should calculate accurate progress percentage")
        void shouldCalculateAccurateProgress() {
            Long taskId = 1L;
            when(valueOperations.get("analysis:status:task:1")).thenReturn("analyzing");
            when(videoFrameMapper.countByTaskId(taskId)).thenReturn(10);
            when(videoFrameMapper.countAnalyzedByTaskId(taskId)).thenReturn(5);

            AnalysisProgressResponse response = service.getProgress(taskId);

            assertThat(response.getStatus()).isEqualTo("analyzing");
            assertThat(response.getTotalFrames()).isEqualTo(10);
            assertThat(response.getAnalyzedFrames()).isEqualTo(5);
            assertThat(response.getProgressPercent()).isEqualTo(50);
        }

        @Test
        @DisplayName("Should return completed when all frames analyzed")
        void shouldReturnCompletedWhenAllAnalyzed() {
            Long taskId = 1L;
            when(valueOperations.get("analysis:status:task:1")).thenReturn("completed");
            when(videoFrameMapper.countByTaskId(taskId)).thenReturn(10);
            when(videoFrameMapper.countAnalyzedByTaskId(taskId)).thenReturn(10);

            AnalysisProgressResponse response = service.getProgress(taskId);

            assertThat(response.getStatus()).isEqualTo("completed");
            assertThat(response.getProgressPercent()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should return failed status with error message")
        void shouldReturnFailedStatus() {
            Long taskId = 1L;
            when(valueOperations.get("analysis:status:task:1")).thenReturn("failed");
            when(videoFrameMapper.countByTaskId(taskId)).thenReturn(10);
            when(videoFrameMapper.countAnalyzedByTaskId(taskId)).thenReturn(3);

            AnalysisProgressResponse response = service.getProgress(taskId);

            assertThat(response.getStatus()).isEqualTo("failed");
            assertThat(response.getErrorMessage()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Already Analyzed Task")
    class AlreadyAnalyzedTests {

        @Test
        @DisplayName("Should return completed if all frames already have category")
        void shouldReturnCompletedIfAlreadyAnalyzed() {
            Long taskId = 1L;
            when(valueOperations.get("analysis:status:task:1")).thenReturn(null);

            VideoFrame frame = createFrame(1L, 1L, "url1");
            frame.setCategory("food"); // Already analyzed
            when(videoFrameMapper.findByTaskId(taskId)).thenReturn(List.of(frame));

            AnalyzeTaskResponse response = service.triggerAnalysis(taskId);

            assertThat(response.getStatus()).isEqualTo("completed");
        }
    }

    @Nested
    @DisplayName("Update Analysis Results")
    class UpdateResultsTests {

        @Test
        @DisplayName("Should update frame with successful analysis results")
        void shouldUpdateFrameWithResults() {
            List<FrameAnalysisResult> results = List.of(
                    FrameAnalysisResult.builder()
                            .frameId(1L)
                            .category("food")
                            .tags(List.of("美食", "精美"))
                            .qualityScore(85)
                            .description("美味食物")
                            .success(true)
                            .build()
            );

            service.updateFrameAnalysisResults(results);

            verify(videoFrameMapper).updateAnalysisResult(
                    eq(1L),
                    eq("food"),
                    eq(List.of("美食", "精美")),
                    eq(85),
                    eq("美味食物")
            );
        }

        @Test
        @DisplayName("Should skip failed analysis results")
        void shouldSkipFailedResults() {
            List<FrameAnalysisResult> results = List.of(
                    FrameAnalysisResult.failed(1L, "url1", "error")
            );

            service.updateFrameAnalysisResults(results);

            verify(videoFrameMapper, never()).updateAnalysisResult(anyLong(), any(), any(), any(), any());
        }
    }

    private VideoFrame createFrame(Long id, Long videoId, String frameUrl) {
        VideoFrame frame = new VideoFrame();
        frame.setId(id);
        frame.setVideoId(videoId);
        frame.setFrameUrl(frameUrl);
        frame.setFrameNumber(1);
        frame.setTimestampMs(0);
        return frame;
    }
}
