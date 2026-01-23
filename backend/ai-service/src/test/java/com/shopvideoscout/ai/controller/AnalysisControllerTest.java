package com.shopvideoscout.ai.controller;

import com.shopvideoscout.ai.dto.AnalysisProgressResponse;
import com.shopvideoscout.ai.dto.AnalyzeTaskResponse;
import com.shopvideoscout.ai.service.FrameAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for AnalysisController.
 * Covers test scenarios from QA Test Design:
 * - 2.3-E2E-001: POST /api/v1/tasks/{id}/analyze triggers full analysis
 * - 2.3-E2E-002: GET /api/v1/tasks/{id}/analysis-progress returns accurate progress
 */
@WebMvcTest(AnalysisController.class)
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FrameAnalysisService frameAnalysisService;

    @Nested
    @DisplayName("2.3-E2E-001: POST /api/v1/tasks/{id}/analyze")
    class TriggerAnalysisTests {

        @Test
        @DisplayName("Should return 202 Accepted when analysis is queued")
        void shouldReturn202WhenQueued() throws Exception {
            Long taskId = 1L;
            when(frameAnalysisService.triggerAnalysis(taskId))
                    .thenReturn(AnalyzeTaskResponse.queued(taskId, 10));

            mockMvc.perform(post("/api/v1/tasks/{taskId}/analyze", taskId)
                            .header("X-User-Id", "123"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.task_id").value(taskId))
                    .andExpect(jsonPath("$.data.status").value("queued"))
                    .andExpect(jsonPath("$.data.total_frames").value(10));

            verify(frameAnalysisService).triggerAnalysis(taskId);
        }

        @Test
        @DisplayName("Should return already_analyzing status if analysis in progress")
        void shouldReturnAlreadyAnalyzing() throws Exception {
            Long taskId = 1L;
            when(frameAnalysisService.triggerAnalysis(taskId))
                    .thenReturn(AnalyzeTaskResponse.alreadyAnalyzing(taskId));

            mockMvc.perform(post("/api/v1/tasks/{taskId}/analyze", taskId)
                            .header("X-User-Id", "123"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.data.status").value("already_analyzing"));
        }

        @Test
        @DisplayName("Should return no_frames status if no frames to analyze")
        void shouldReturnNoFrames() throws Exception {
            Long taskId = 1L;
            when(frameAnalysisService.triggerAnalysis(taskId))
                    .thenReturn(AnalyzeTaskResponse.noFrames(taskId));

            mockMvc.perform(post("/api/v1/tasks/{taskId}/analyze", taskId)
                            .header("X-User-Id", "123"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.data.status").value("no_frames"))
                    .andExpect(jsonPath("$.data.total_frames").value(0));
        }
    }

    @Nested
    @DisplayName("2.3-E2E-002: GET /api/v1/tasks/{id}/analysis-progress")
    class GetProgressTests {

        @Test
        @DisplayName("Should return accurate progress percentage")
        void shouldReturnAccurateProgress() throws Exception {
            Long taskId = 1L;
            when(frameAnalysisService.getProgress(taskId))
                    .thenReturn(AnalysisProgressResponse.inProgress(taskId, 10, 5));

            mockMvc.perform(get("/api/v1/tasks/{taskId}/analysis-progress", taskId)
                            .header("X-User-Id", "123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.task_id").value(taskId))
                    .andExpect(jsonPath("$.data.status").value("analyzing"))
                    .andExpect(jsonPath("$.data.total_frames").value(10))
                    .andExpect(jsonPath("$.data.analyzed_frames").value(5))
                    .andExpect(jsonPath("$.data.progress_percent").value(50));
        }

        @Test
        @DisplayName("Should return pending status for new task")
        void shouldReturnPendingForNewTask() throws Exception {
            Long taskId = 1L;
            when(frameAnalysisService.getProgress(taskId))
                    .thenReturn(AnalysisProgressResponse.pending(taskId));

            mockMvc.perform(get("/api/v1/tasks/{taskId}/analysis-progress", taskId)
                            .header("X-User-Id", "123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("pending"))
                    .andExpect(jsonPath("$.data.progress_percent").value(0));
        }

        @Test
        @DisplayName("Should return completed status when analysis done")
        void shouldReturnCompletedWhenDone() throws Exception {
            Long taskId = 1L;
            when(frameAnalysisService.getProgress(taskId))
                    .thenReturn(AnalysisProgressResponse.completed(taskId, 10));

            mockMvc.perform(get("/api/v1/tasks/{taskId}/analysis-progress", taskId)
                            .header("X-User-Id", "123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("completed"))
                    .andExpect(jsonPath("$.data.progress_percent").value(100));
        }

        @Test
        @DisplayName("Should return failed status with error message")
        void shouldReturnFailedWithError() throws Exception {
            Long taskId = 1L;
            when(frameAnalysisService.getProgress(taskId))
                    .thenReturn(AnalysisProgressResponse.failed(taskId, "AI服务超时"));

            mockMvc.perform(get("/api/v1/tasks/{taskId}/analysis-progress", taskId)
                            .header("X-User-Id", "123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("failed"))
                    .andExpect(jsonPath("$.data.error_message").value("AI服务超时"));
        }
    }
}
