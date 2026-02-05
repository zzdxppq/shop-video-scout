package com.shopvideoscout.publish.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.publish.dto.PublishAssistResponse;
import com.shopvideoscout.publish.service.PublishAssistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for PublishAssistController.
 * Story 5.3: 发布辅助服务
 */
@WebMvcTest(PublishAssistController.class)
@DisplayName("PublishAssistController Tests")
class PublishAssistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PublishAssistService publishAssistService;

    @Nested
    @DisplayName("GET /api/v1/publish/tasks/{id}/assist")
    class GetPublishAssist {

        @Test
        @WithMockUser(username = "100")
        @DisplayName("should return 200 with publish assist response")
        void shouldReturn200WithResponse() throws Exception {
            // Given
            Long taskId = 1L;
            Long userId = 100L;

            PublishAssistResponse response = PublishAssistResponse.builder()
                    .topics(List.of("#探店", "#美食推荐"))
                    .titles(List.of("这是一个测试标题需要二十个字符"))
                    .regenerateRemaining(3)
                    .build();

            when(publishAssistService.getPublishAssist(eq(taskId), eq(userId)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/publish/tasks/{id}/assist", taskId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.topics").isArray())
                    .andExpect(jsonPath("$.data.topics[0]").value("#探店"))
                    .andExpect(jsonPath("$.data.titles").isArray())
                    .andExpect(jsonPath("$.data.regenerateRemaining").value(3));
        }

        @Test
        @WithMockUser(username = "100")
        @DisplayName("should return 404 when task not found")
        void shouldReturn404WhenTaskNotFound() throws Exception {
            // Given
            Long taskId = 999L;
            Long userId = 100L;

            when(publishAssistService.getPublishAssist(eq(taskId), eq(userId)))
                    .thenThrow(new BusinessException(ResultCode.TASK_NOT_FOUND));

            // When/Then
            mockMvc.perform(get("/api/v1/publish/tasks/{id}/assist", taskId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.TASK_NOT_FOUND.getCode()));
        }

        @Test
        @WithMockUser(username = "100")
        @DisplayName("should return 403 when user is not owner")
        void shouldReturn403WhenNotOwner() throws Exception {
            // Given
            Long taskId = 1L;
            Long userId = 100L;

            when(publishAssistService.getPublishAssist(eq(taskId), eq(userId)))
                    .thenThrow(new BusinessException(ResultCode.FORBIDDEN));

            // When/Then
            mockMvc.perform(get("/api/v1/publish/tasks/{id}/assist", taskId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.FORBIDDEN.getCode()));
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/v1/publish/tasks/1/assist"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/publish/tasks/{id}/assist/regenerate")
    class Regenerate {

        @Test
        @WithMockUser(username = "100")
        @DisplayName("should return 200 with regenerated response")
        void shouldReturn200WithRegeneratedResponse() throws Exception {
            // Given
            Long taskId = 1L;
            Long userId = 100L;

            PublishAssistResponse response = PublishAssistResponse.builder()
                    .topics(List.of("#新话题"))
                    .titles(List.of("这是新生成的标题需要二十个字符"))
                    .regenerateRemaining(2)
                    .build();

            when(publishAssistService.regenerate(eq(taskId), eq(userId)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/publish/tasks/{id}/assist/regenerate", taskId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.regenerateRemaining").value(2));
        }

        @Test
        @WithMockUser(username = "100")
        @DisplayName("should return error when regenerate limit exceeded")
        void shouldReturnErrorWhenLimitExceeded() throws Exception {
            // Given
            Long taskId = 1L;
            Long userId = 100L;

            when(publishAssistService.regenerate(eq(taskId), eq(userId)))
                    .thenThrow(new BusinessException(ResultCode.PUBLISH_ASSIST_LIMIT_EXCEEDED));

            // When/Then
            mockMvc.perform(post("/api/v1/publish/tasks/{id}/assist/regenerate", taskId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.PUBLISH_ASSIST_LIMIT_EXCEEDED.getCode()));
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // When/Then
            mockMvc.perform(post("/api/v1/publish/tasks/1/assist/regenerate")
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }
}
