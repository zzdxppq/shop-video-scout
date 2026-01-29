package com.shopvideoscout.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.task.config.OssConfig;
import com.shopvideoscout.task.constant.TaskConstants;
import com.shopvideoscout.task.entity.Task;
import com.shopvideoscout.task.entity.Video;
import com.shopvideoscout.task.mapper.ScriptMapper;
import com.shopvideoscout.task.mapper.TaskMapper;
import com.shopvideoscout.task.mapper.VideoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OutputController (Story 4.3).
 */
@ExtendWith(MockitoExtension.class)
class OutputControllerTest {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private ScriptMapper scriptMapper;

    @Mock
    private VideoMapper videoMapper;

    @Mock
    private OssConfig ossConfig;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OutputController outputController;

    private UserDetails userDetails;
    private Task completedTask;

    @BeforeEach
    void setUp() {
        userDetails = User.builder()
                .username("123")
                .password("")
                .authorities(Collections.emptyList())
                .build();

        completedTask = new Task();
        completedTask.setId(1L);
        completedTask.setUserId(123L);
        completedTask.setStatus(TaskConstants.TaskStatus.COMPLETED);
        completedTask.setOutputOssKey("output/1/final.mp4");
        completedTask.setOutputDurationSeconds(62);
        completedTask.setOutputFileSize(47841280L);
    }

    @Nested
    @DisplayName("AC2: GET /api/v1/tasks/{id}/output")
    class GetOutputTests {

        @Test
        @DisplayName("4.3-INT-011: Returns OutputResponse with video URL, duration, size")
        void getOutput_WhenCompleted_ShouldReturnVideoInfo() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(completedTask);
            when(ossConfig.getCdnBaseUrl()).thenReturn("https://cdn.example.com/");
            when(scriptMapper.findContentByTaskId(1L)).thenReturn(null);

            // When
            var response = outputController.getOutput(1L, userDetails);

            // Then
            assertNotNull(response);
            assertTrue(response.isSuccess());

            var data = response.getData();
            assertEquals(TaskConstants.TaskStatus.COMPLETED, data.getStatus());
            assertNotNull(data.getVideo());
            assertEquals("https://cdn.example.com/output/1/final.mp4", data.getVideo().getUrl());
            assertEquals(62, data.getVideo().getDurationSeconds());
            assertEquals(47841280L, data.getVideo().getFileSize());
            assertEquals(1080, data.getVideo().getWidth());
            assertEquals(1920, data.getVideo().getHeight());
            assertEquals("mp4", data.getVideo().getFormat());
        }

        @Test
        @DisplayName("4.3-INT-012: Returns 404 OUTPUT_NOT_READY when task not completed")
        void getOutput_WhenNotCompleted_ShouldThrowNotReady() {
            // Given
            Task composingTask = new Task();
            composingTask.setId(1L);
            composingTask.setUserId(123L);
            composingTask.setStatus(TaskConstants.TaskStatus.COMPOSING);
            when(taskMapper.selectById(1L)).thenReturn(composingTask);

            // When/Then
            var ex = assertThrows(
                    com.shopvideoscout.common.exception.BusinessException.class,
                    () -> outputController.getOutput(1L, userDetails));

            assertEquals(ResultCode.OUTPUT_NOT_READY.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("Returns 404 when task not found")
        void getOutput_WhenTaskNotFound_ShouldThrowNotFound() {
            // Given
            when(taskMapper.selectById(999L)).thenReturn(null);

            // When/Then
            var ex = assertThrows(
                    com.shopvideoscout.common.exception.BusinessException.class,
                    () -> outputController.getOutput(999L, userDetails));

            assertEquals(ResultCode.TASK_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("Returns 403 when user doesn't own task")
        void getOutput_WhenNotOwner_ShouldThrowForbidden() {
            // Given
            Task otherUserTask = new Task();
            otherUserTask.setId(1L);
            otherUserTask.setUserId(999L); // Different user
            otherUserTask.setStatus(TaskConstants.TaskStatus.COMPLETED);
            when(taskMapper.selectById(1L)).thenReturn(otherUserTask);

            // When/Then
            var ex = assertThrows(
                    com.shopvideoscout.common.exception.BusinessException.class,
                    () -> outputController.getOutput(1L, userDetails));

            assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("Returns shots_used from script paragraphs")
        void getOutput_ShouldReturnShotsUsed() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(completedTask);
            when(ossConfig.getCdnBaseUrl()).thenReturn("https://cdn.example.com/");

            String scriptContent = "{\"paragraphs\": [{\"shot_id\": 101}, {\"shot_id\": 102}]}";
            when(scriptMapper.findContentByTaskId(1L)).thenReturn(scriptContent);

            Video video1 = new Video();
            video1.setId(101L);
            video1.setCategory("food");
            video1.setDurationSeconds(30);
            video1.setThumbnailOssKey("thumbnails/101.jpg");
            when(videoMapper.selectById(101L)).thenReturn(video1);

            Video video2 = new Video();
            video2.setId(102L);
            video2.setCategory("environment");
            video2.setDurationSeconds(20);
            when(videoMapper.selectById(102L)).thenReturn(video2);

            // When
            var response = outputController.getOutput(1L, userDetails);

            // Then
            var data = response.getData();
            assertNotNull(data.getShotsUsed());
            assertEquals(2, data.getShotsUsed().size());
        }
    }
}
