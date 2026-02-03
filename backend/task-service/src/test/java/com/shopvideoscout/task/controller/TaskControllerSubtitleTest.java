package com.shopvideoscout.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.task.dto.SubtitleSettingsRequest;
import com.shopvideoscout.task.dto.SubtitleSettingsResponse;
import com.shopvideoscout.task.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for subtitle settings endpoint.
 * Story 4.5: 字幕设置页面
 *
 * Test IDs: 4.5-UNIT-037 to 4.5-INT-004
 */
@WebMvcTest(TaskController.class)
class TaskControllerSubtitleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @Nested
    @DisplayName("PUT /api/v1/tasks/{id}/subtitle-settings")
    class UpdateSubtitleSettingsTests {

        // 4.5-INT-001: PUT subtitle-settings returns 200
        @Test
        @DisplayName("4.5-INT-001: Update subtitle settings returns 200 with updated settings")
        void updateSubtitleSettings_ShouldReturn200() throws Exception {
            // Given
            SubtitleSettingsRequest request = new SubtitleSettingsRequest();
            request.setSubtitleEnabled(false);
            request.setSubtitleStyle("neon");

            SubtitleSettingsResponse response = SubtitleSettingsResponse.of(false, "neon");
            when(taskService.updateSubtitleSettings(eq(1L), eq(100L), any(SubtitleSettingsRequest.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(put("/api/v1/tasks/1/subtitle-settings")
                            .header("X-User-Id", 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.subtitleEnabled").value(false))
                    .andExpect(jsonPath("$.data.subtitleStyle").value("neon"));
        }

        // 4.5-INT-002: PUT with task not found
        @Test
        @DisplayName("4.5-INT-002: Update subtitle settings with non-existent task returns 404")
        void updateSubtitleSettings_TaskNotFound_ShouldReturn404() throws Exception {
            // Given
            SubtitleSettingsRequest request = new SubtitleSettingsRequest();
            request.setSubtitleEnabled(true);
            request.setSubtitleStyle("simple_white");

            when(taskService.updateSubtitleSettings(eq(999L), eq(100L), any(SubtitleSettingsRequest.class)))
                    .thenThrow(new BusinessException(ResultCode.TASK_NOT_FOUND, "任务不存在"));

            // When/Then
            mockMvc.perform(put("/api/v1/tasks/999/subtitle-settings")
                            .header("X-User-Id", 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.TASK_NOT_FOUND.getCode()));
        }

        // 4.5-INT-003: PUT with invalid style value
        @Test
        @DisplayName("4.5-INT-003: Update with invalid style returns 400 validation error")
        void updateSubtitleSettings_InvalidStyle_ShouldReturn400() throws Exception {
            // Given - invalid style value
            String invalidRequest = "{\"subtitleEnabled\":true,\"subtitleStyle\":\"invalid_style\"}";

            // When/Then
            mockMvc.perform(put("/api/v1/tasks/1/subtitle-settings")
                            .header("X-User-Id", 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        // 4.5-UNIT-037: DTO validation accepts valid style
        @Test
        @DisplayName("4.5-UNIT-037: Valid style neon should pass validation")
        void validStyle_ShouldPassValidation() throws Exception {
            // Given
            SubtitleSettingsRequest request = new SubtitleSettingsRequest();
            request.setSubtitleEnabled(true);
            request.setSubtitleStyle("neon");

            SubtitleSettingsResponse response = SubtitleSettingsResponse.of(true, "neon");
            when(taskService.updateSubtitleSettings(eq(1L), eq(100L), any(SubtitleSettingsRequest.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(put("/api/v1/tasks/1/subtitle-settings")
                            .header("X-User-Id", 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        // 4.5-UNIT-038: DTO validation rejects invalid style
        @Test
        @DisplayName("4.5-UNIT-038: Invalid style should fail validation")
        void invalidStyle_ShouldFailValidation() throws Exception {
            // Given
            String invalidRequest = "{\"subtitleEnabled\":true,\"subtitleStyle\":\"bad_style\"}";

            // When/Then
            mockMvc.perform(put("/api/v1/tasks/1/subtitle-settings")
                            .header("X-User-Id", 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        // 4.5-UNIT-039: DTO accepts boolean subtitleEnabled
        @Test
        @DisplayName("4.5-UNIT-039: Boolean subtitleEnabled should pass validation")
        void booleanSubtitleEnabled_ShouldPassValidation() throws Exception {
            // Given
            SubtitleSettingsRequest request = new SubtitleSettingsRequest();
            request.setSubtitleEnabled(false);
            request.setSubtitleStyle("simple_white");

            SubtitleSettingsResponse response = SubtitleSettingsResponse.of(false, "simple_white");
            when(taskService.updateSubtitleSettings(eq(1L), eq(100L), any(SubtitleSettingsRequest.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(put("/api/v1/tasks/1/subtitle-settings")
                            .header("X-User-Id", 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.subtitleEnabled").value(false));
        }

        // 4.5-INT-004: Unauthorized access
        @Test
        @DisplayName("4.5-INT-004: Update other user's task returns 403")
        void updateSubtitleSettings_Unauthorized_ShouldReturn403() throws Exception {
            // Given
            SubtitleSettingsRequest request = new SubtitleSettingsRequest();
            request.setSubtitleEnabled(true);
            request.setSubtitleStyle("simple_white");

            when(taskService.updateSubtitleSettings(eq(1L), eq(200L), any(SubtitleSettingsRequest.class)))
                    .thenThrow(new BusinessException(ResultCode.FORBIDDEN, "无权访问此任务"));

            // When/Then
            mockMvc.perform(put("/api/v1/tasks/1/subtitle-settings")
                            .header("X-User-Id", 200L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.FORBIDDEN.getCode()));
        }

        // All valid styles test
        @Test
        @DisplayName("All 5 valid styles should pass validation")
        void allValidStyles_ShouldPassValidation() throws Exception {
            String[] validStyles = {"simple_white", "vibrant_yellow", "xiaohongshu", "douyin_hot", "neon"};

            for (String style : validStyles) {
                SubtitleSettingsRequest request = new SubtitleSettingsRequest();
                request.setSubtitleEnabled(true);
                request.setSubtitleStyle(style);

                SubtitleSettingsResponse response = SubtitleSettingsResponse.of(true, style);
                when(taskService.updateSubtitleSettings(eq(1L), eq(100L), any(SubtitleSettingsRequest.class)))
                        .thenReturn(response);

                mockMvc.perform(put("/api/v1/tasks/1/subtitle-settings")
                                .header("X-User-Id", 100L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200))
                        .andExpect(jsonPath("$.data.subtitleStyle").value(style));
            }
        }
    }
}
