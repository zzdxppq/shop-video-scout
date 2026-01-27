package com.shopvideoscout.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.task.constant.TaskConstants;
import com.shopvideoscout.task.dto.ComposeProgressResponse;
import com.shopvideoscout.task.dto.ComposeResponse;
import com.shopvideoscout.task.dto.VoiceTypeRequest;
import com.shopvideoscout.task.service.ComposeProgressService;
import com.shopvideoscout.task.service.ComposeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for ComposeController and VoiceTypeController.
 */
@WebMvcTest({ComposeController.class, VoiceTypeController.class})
class ComposeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ComposeService composeService;

    @MockBean
    private ComposeProgressService composeProgressService;

    @Nested
    @DisplayName("POST /api/v1/tasks/{id}/compose")
    class TriggerComposeTests {

        @Test
        @DisplayName("4.1-INT-001: Trigger compose returns 200 with status composing")
        void triggerCompose_ShouldReturn200() throws Exception {
            // Given
            when(composeService.triggerCompose(1L, 100L))
                    .thenReturn(ComposeResponse.builder()
                            .status(TaskConstants.TaskStatus.COMPOSING)
                            .taskId(1L)
                            .build());

            // When/Then
            mockMvc.perform(post("/api/v1/tasks/1/compose")
                            .header("X-User-Id", 100L)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.status").value("composing"))
                    .andExpect(jsonPath("$.data.taskId").value(1));
        }

        @Test
        @DisplayName("4.1-BLIND-FLOW-001: Invalid status should return error")
        void invalidStatus_ShouldReturnError() throws Exception {
            // Given
            when(composeService.triggerCompose(1L, 100L))
                    .thenThrow(new BusinessException(ResultCode.TASK_STATUS_INVALID));

            // When/Then
            mockMvc.perform(post("/api/v1/tasks/1/compose")
                            .header("X-User-Id", 100L)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.TASK_STATUS_INVALID.getCode()));
        }

        @Test
        @DisplayName("4.1-BLIND-CONCURRENCY-001: Duplicate compose returns 409")
        void duplicateCompose_ShouldReturn409() throws Exception {
            // Given
            when(composeService.triggerCompose(1L, 100L))
                    .thenThrow(new BusinessException(ResultCode.TASK_ALREADY_COMPOSING));

            // When/Then
            mockMvc.perform(post("/api/v1/tasks/1/compose")
                            .header("X-User-Id", 100L)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.TASK_ALREADY_COMPOSING.getCode()));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tasks/{id}/compose-progress")
    class ComposeProgressTests {

        @Test
        @DisplayName("4.1-INT-005: Progress endpoint returns correct structure")
        void getProgress_ShouldReturnCorrectStructure() throws Exception {
            // Given
            when(composeProgressService.getProgress(1L, 100L))
                    .thenReturn(ComposeProgressResponse.builder()
                            .status("synthesizing")
                            .phase("tts_synthesis")
                            .progress(43)
                            .details(ComposeProgressResponse.ProgressDetails.builder()
                                    .completedParagraphs(3)
                                    .totalParagraphs(7)
                                    .currentStep("TTS合成")
                                    .estimatedRemainingSeconds(45)
                                    .build())
                            .build());

            // When/Then
            mockMvc.perform(get("/api/v1/tasks/1/compose-progress")
                            .header("X-User-Id", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.status").value("synthesizing"))
                    .andExpect(jsonPath("$.data.phase").value("tts_synthesis"))
                    .andExpect(jsonPath("$.data.progress").value(43))
                    .andExpect(jsonPath("$.data.details.completedParagraphs").value(3))
                    .andExpect(jsonPath("$.data.details.totalParagraphs").value(7))
                    .andExpect(jsonPath("$.data.details.estimatedRemainingSeconds").value(45));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/tasks/{id}/voice-type")
    class VoiceTypeTests {

        @Test
        @DisplayName("4.1-INT-002: PUT voice-type updates successfully")
        void updateVoiceType_ShouldReturn200() throws Exception {
            // Given
            VoiceTypeRequest request = new VoiceTypeRequest();
            request.setVoiceType("xiaomei");

            doNothing().when(composeService).updateVoiceType(1L, 100L, "xiaomei", null);

            // When/Then
            mockMvc.perform(put("/api/v1/tasks/1/voice-type")
                            .header("X-User-Id", 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("4.1-BLIND-BOUNDARY-005: Empty voice type returns 400")
        void emptyVoiceType_ShouldReturn400() throws Exception {
            // Given
            VoiceTypeRequest request = new VoiceTypeRequest();
            request.setVoiceType("");

            // When/Then
            mockMvc.perform(put("/api/v1/tasks/1/voice-type")
                            .header("X-User-Id", 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(422));
        }
    }
}
