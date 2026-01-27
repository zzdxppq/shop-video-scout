package com.shopvideoscout.task.service;

import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.common.util.RedisUtils;
import com.shopvideoscout.task.constant.TaskConstants;
import com.shopvideoscout.task.dto.ComposeProgressResponse;
import com.shopvideoscout.task.entity.Task;
import com.shopvideoscout.task.mapper.TaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ComposeProgressService.
 */
@ExtendWith(MockitoExtension.class)
class ComposeProgressServiceTest {

    @Mock
    private RedisUtils redisUtils;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private ComposeProgressService composeProgressService;

    private Task mockTask;

    @BeforeEach
    void setUp() {
        mockTask = new Task();
        mockTask.setId(1L);
        mockTask.setUserId(100L);
        mockTask.setStatus(TaskConstants.TaskStatus.COMPOSING);
    }

    @Nested
    @DisplayName("AC2: Progress Query")
    class ProgressQueryTests {

        @Test
        @DisplayName("4.1-UNIT-010: Read progress from Redis hash → ComposeProgressResponse")
        void readProgressFromRedis_ShouldReturnResponse() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            Map<Object, Object> progressData = new HashMap<>();
            progressData.put("status", "synthesizing");
            progressData.put("completed_paragraphs", "3");
            progressData.put("total_paragraphs", "7");
            progressData.put("estimated_remaining_seconds", "40");
            progressData.put("current_step", "TTS合成");
            when(redisUtils.hGetAll("task:progress:1")).thenReturn(progressData);

            // When
            ComposeProgressResponse response = composeProgressService.getProgress(1L, 100L);

            // Then
            assertEquals("synthesizing", response.getStatus());
            assertEquals("tts_synthesis", response.getPhase());
            assertEquals(42, response.getProgress()); // 3/7 ≈ 42%
            assertEquals(3, response.getDetails().getCompletedParagraphs());
            assertEquals(7, response.getDetails().getTotalParagraphs());
            assertEquals(40, response.getDetails().getEstimatedRemainingSeconds());
            assertEquals("TTS合成", response.getDetails().getCurrentStep());
        }

        @Test
        @DisplayName("4.1-UNIT-012: Estimate remaining time from average duration")
        void estimateRemainingTime_ShouldCalculateCorrectly() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            Map<Object, Object> progressData = new HashMap<>();
            progressData.put("status", "synthesizing");
            progressData.put("completed_paragraphs", "5");
            progressData.put("total_paragraphs", "10");
            progressData.put("estimated_remaining_seconds", "50");
            progressData.put("current_step", "TTS合成");
            when(redisUtils.hGetAll("task:progress:1")).thenReturn(progressData);

            // When
            ComposeProgressResponse response = composeProgressService.getProgress(1L, 100L);

            // Then
            assertEquals(50, response.getProgress()); // 5/10 = 50%
            assertEquals(50, response.getDetails().getEstimatedRemainingSeconds());
        }

        @Test
        @DisplayName("4.1-INT-005: Progress endpoint returns correct JSON structure")
        void progressEndpoint_ShouldReturnCorrectStructure() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            Map<Object, Object> progressData = new HashMap<>();
            progressData.put("status", "completed");
            progressData.put("completed_paragraphs", "7");
            progressData.put("total_paragraphs", "7");
            progressData.put("estimated_remaining_seconds", "0");
            progressData.put("current_step", "完成");
            when(redisUtils.hGetAll("task:progress:1")).thenReturn(progressData);

            // When
            ComposeProgressResponse response = composeProgressService.getProgress(1L, 100L);

            // Then
            assertNotNull(response);
            assertNotNull(response.getDetails());
            assertEquals(100, response.getProgress()); // 7/7 = 100%
            assertEquals("completed", response.getStatus());
        }

        @Test
        @DisplayName("No progress data → return default progress")
        void noProgressData_ShouldReturnDefault() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(redisUtils.hGetAll("task:progress:1")).thenReturn(new HashMap<>());

            // When
            ComposeProgressResponse response = composeProgressService.getProgress(1L, 100L);

            // Then
            assertEquals(TaskConstants.TaskStatus.COMPOSING, response.getStatus());
            assertEquals(0, response.getProgress());
            assertEquals(0, response.getDetails().getCompletedParagraphs());
        }

        @Test
        @DisplayName("4.1-BLIND-ERROR-004: Redis unavailable → 503 error")
        void redisUnavailable_ShouldReturn503() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(redisUtils.hGetAll("task:progress:1"))
                    .thenThrow(new RuntimeException("Connection refused"));

            // When/Then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> composeProgressService.getProgress(1L, 100L));
            assertEquals(ResultCode.SERVICE_UNAVAILABLE.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("Task not found → 404 error")
        void taskNotFound_ShouldThrow404() {
            // Given
            when(taskMapper.selectById(999L)).thenReturn(null);

            // When/Then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> composeProgressService.getProgress(999L, 100L));
            assertEquals(ResultCode.TASK_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("User not owner → 403 error")
        void userNotOwner_ShouldThrow403() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(mockTask);

            // When/Then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> composeProgressService.getProgress(1L, 999L));
            assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        }
    }
}
