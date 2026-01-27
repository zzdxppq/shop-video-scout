package com.shopvideoscout.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.user.dto.CloneResultRequest;
import com.shopvideoscout.user.service.VoiceSampleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller integration tests for InternalVoiceCallbackController (SEC-001: internal route).
 */
@WebMvcTest(InternalVoiceCallbackController.class)
class InternalVoiceCallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VoiceSampleService voiceSampleService;

    @Test
    @DisplayName("4.2-INT-008: Clone callback via internal route updates sample record")
    void cloneResult_Success_Returns200() throws Exception {
        CloneResultRequest request = new CloneResultRequest("clone_abc123", "completed", null);

        doNothing().when(voiceSampleService).updateCloneResult(eq(1L), eq("clone_abc123"), eq("completed"), any());

        mockMvc.perform(post("/internal/voice/samples/1/clone-result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(voiceSampleService).updateCloneResult(eq(1L), eq("clone_abc123"), eq("completed"), any());
    }

    @Test
    @DisplayName("SEC-001: Internal route does not require X-User-Id header")
    void cloneResult_NoUserIdHeader_StillWorks() throws Exception {
        CloneResultRequest request = new CloneResultRequest(null, "failed", "Clone API error");

        doNothing().when(voiceSampleService).updateCloneResult(eq(2L), any(), eq("failed"), eq("Clone API error"));

        mockMvc.perform(post("/internal/voice/samples/2/clone-result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("VAL-003: Invalid status value rejected by @Valid + @Pattern")
    void cloneResult_InvalidStatus_Returns400() throws Exception {
        CloneResultRequest request = new CloneResultRequest("clone_abc", "invalid_status", null);

        mockMvc.perform(post("/internal/voice/samples/1/clone-result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("VAL-003: Blank status rejected by @Valid + @NotBlank")
    void cloneResult_BlankStatus_Returns400() throws Exception {
        CloneResultRequest request = new CloneResultRequest("clone_abc", "", null);

        mockMvc.perform(post("/internal/voice/samples/1/clone-result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
