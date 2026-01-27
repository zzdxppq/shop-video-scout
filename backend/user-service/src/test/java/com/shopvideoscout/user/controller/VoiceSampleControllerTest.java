package com.shopvideoscout.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.user.dto.*;
import com.shopvideoscout.user.entity.VoiceSample;
import com.shopvideoscout.user.service.VoiceSampleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller integration tests for VoiceSampleController (T4: API contract verification).
 */
@WebMvcTest(VoiceSampleController.class)
class VoiceSampleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VoiceSampleService voiceSampleService;

    private static final Long USER_ID = 100L;

    // ===== Upload URL Endpoint =====

    @Nested
    @DisplayName("POST /api/v1/voice/upload-url")
    class UploadUrlEndpointTests {

        @Test
        @DisplayName("4.2-INT-002: POST upload-url → 200 with presigned URL")
        void uploadUrl_ValidRequest_Returns200() throws Exception {
            VoiceUploadUrlRequest request = new VoiceUploadUrlRequest("sample.mp3");
            VoiceUploadUrlResponse response = VoiceUploadUrlResponse.builder()
                    .uploadUrl("https://oss.example.com/upload?signature=xxx")
                    .ossKey("voice/100/uuid.mp3")
                    .expiresIn(900)
                    .build();

            when(voiceSampleService.generateUploadUrl(eq(USER_ID), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/voice/upload-url")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.uploadUrl").value("https://oss.example.com/upload?signature=xxx"))
                    .andExpect(jsonPath("$.data.ossKey").value("voice/100/uuid.mp3"))
                    .andExpect(jsonPath("$.data.expiresIn").value(900));
        }

        @Test
        @DisplayName("4.2-INT-004 (upload-url): Invalid format → error code INVALID_AUDIO_FORMAT")
        void uploadUrl_InvalidFormat_ReturnsError() throws Exception {
            VoiceUploadUrlRequest request = new VoiceUploadUrlRequest("document.txt");

            when(voiceSampleService.generateUploadUrl(eq(USER_ID), any()))
                    .thenThrow(new BusinessException(ResultCode.INVALID_AUDIO_FORMAT));

            mockMvc.perform(post("/api/v1/voice/upload-url")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.INVALID_AUDIO_FORMAT.getCode()));
        }
    }

    // ===== Create Sample Endpoint =====

    @Nested
    @DisplayName("POST /api/v1/voice/samples")
    class CreateSampleEndpointTests {

        @Test
        @DisplayName("4.2-INT-003: POST samples → 201 with sample record")
        void createSample_ValidRequest_Returns201() throws Exception {
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("My Voice", "voice/100/uuid.mp3", 60);
            VoiceSampleResponse response = VoiceSampleResponse.builder()
                    .id(1L)
                    .name("My Voice")
                    .status(VoiceSample.STATUS_UPLOADING)
                    .durationSeconds(60)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(voiceSampleService.createVoiceSample(eq(USER_ID), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/voice/samples")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("My Voice"))
                    .andExpect(jsonPath("$.data.status").value("uploading"));
        }

        @Test
        @DisplayName("4.2-INT-004: POST samples with invalid format → error code")
        void createSample_InvalidFormat_ReturnsError() throws Exception {
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("Voice", "voice/100/uuid.txt", 60);

            when(voiceSampleService.createVoiceSample(eq(USER_ID), any()))
                    .thenThrow(new BusinessException(ResultCode.INVALID_AUDIO_FORMAT));

            mockMvc.perform(post("/api/v1/voice/samples")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.INVALID_AUDIO_FORMAT.getCode()));
        }

        @Test
        @DisplayName("4.2-INT-005: POST samples with invalid duration → error code")
        void createSample_InvalidDuration_ReturnsError() throws Exception {
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("Voice", "voice/100/uuid.mp3", 4);

            when(voiceSampleService.createVoiceSample(eq(USER_ID), any()))
                    .thenThrow(new BusinessException(ResultCode.AUDIO_DURATION_INVALID));

            mockMvc.perform(post("/api/v1/voice/samples")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.AUDIO_DURATION_INVALID.getCode()));
        }

        @Test
        @DisplayName("4.2-INT-006: POST samples when limit exceeded → error code")
        void createSample_LimitExceeded_ReturnsError() throws Exception {
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("Voice", "voice/100/uuid.mp3", 60);

            when(voiceSampleService.createVoiceSample(eq(USER_ID), any()))
                    .thenThrow(new BusinessException(ResultCode.VOICE_SAMPLE_LIMIT_EXCEEDED));

            mockMvc.perform(post("/api/v1/voice/samples")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.VOICE_SAMPLE_LIMIT_EXCEEDED.getCode()));
        }
    }

    // ===== List / Get / Delete / Preview Endpoints =====

    @Nested
    @DisplayName("GET /api/v1/voice/samples")
    class ListSamplesEndpointTests {

        @Test
        @DisplayName("4.2-INT-010: GET samples → 200 with list")
        void listSamples_ReturnsListWithStatus() throws Exception {
            List<VoiceSampleResponse> samples = List.of(
                    VoiceSampleResponse.builder()
                            .id(1L).name("Voice 1").status("completed").durationSeconds(30).build(),
                    VoiceSampleResponse.builder()
                            .id(2L).name("Voice 2").status("processing").durationSeconds(60).build()
            );

            when(voiceSampleService.listByUserId(USER_ID)).thenReturn(samples);

            mockMvc.perform(get("/api/v1/voice/samples")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].name").value("Voice 1"))
                    .andExpect(jsonPath("$.data[0].status").value("completed"))
                    .andExpect(jsonPath("$.data[1].status").value("processing"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/voice/samples/{id}")
    class GetSampleEndpointTests {

        @Test
        @DisplayName("4.2-INT-011: GET samples/{id} → 200 with detail")
        void getSample_Returns200WithDetail() throws Exception {
            VoiceSampleResponse response = VoiceSampleResponse.builder()
                    .id(1L).name("My Voice").status("completed")
                    .cloneVoiceId("clone_abc123").durationSeconds(60).build();

            when(voiceSampleService.getById(USER_ID, 1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/voice/samples/1")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("My Voice"))
                    .andExpect(jsonPath("$.data.cloneVoiceId").value("clone_abc123"));
        }

        @Test
        @DisplayName("GET samples/{id} not found → VOICE_SAMPLE_NOT_FOUND")
        void getSample_NotFound_ReturnsError() throws Exception {
            when(voiceSampleService.getById(USER_ID, 999L))
                    .thenThrow(new BusinessException(ResultCode.VOICE_SAMPLE_NOT_FOUND));

            mockMvc.perform(get("/api/v1/voice/samples/999")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.VOICE_SAMPLE_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/voice/samples/{id}")
    class DeleteSampleEndpointTests {

        @Test
        @DisplayName("4.2-INT-012: DELETE samples/{id} → 200")
        void deleteSample_Returns200() throws Exception {
            doNothing().when(voiceSampleService).deleteById(USER_ID, 1L);

            mockMvc.perform(delete("/api/v1/voice/samples/1")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(voiceSampleService).deleteById(USER_ID, 1L);
        }

        @Test
        @DisplayName("DELETE samples/{id} not found → VOICE_SAMPLE_NOT_FOUND")
        void deleteSample_NotFound_ReturnsError() throws Exception {
            doThrow(new BusinessException(ResultCode.VOICE_SAMPLE_NOT_FOUND))
                    .when(voiceSampleService).deleteById(USER_ID, 999L);

            mockMvc.perform(delete("/api/v1/voice/samples/999")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.VOICE_SAMPLE_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/voice/samples/{id}/preview")
    class PreviewEndpointTests {

        @Test
        @DisplayName("4.2-INT-013: GET preview → 200 with preview info")
        void getPreview_Completed_Returns200() throws Exception {
            VoicePreviewResponse response = VoicePreviewResponse.builder()
                    .sampleId(1L)
                    .previewText("你好，这是我的声音克隆效果预览。")
                    .status("completed")
                    .cloneVoiceId("clone_abc123")
                    .build();

            when(voiceSampleService.getPreview(USER_ID, 1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/voice/samples/1/preview")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.sampleId").value(1))
                    .andExpect(jsonPath("$.data.previewText").isNotEmpty())
                    .andExpect(jsonPath("$.data.cloneVoiceId").value("clone_abc123"));
        }

        @Test
        @DisplayName("4.2-BLIND-FLOW-001: Preview before clone completes → VOICE_CLONE_IN_PROGRESS")
        void getPreview_NotCompleted_ReturnsError() throws Exception {
            when(voiceSampleService.getPreview(USER_ID, 1L))
                    .thenThrow(new BusinessException(ResultCode.VOICE_CLONE_IN_PROGRESS));

            mockMvc.perform(get("/api/v1/voice/samples/1/preview")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.VOICE_CLONE_IN_PROGRESS.getCode()));
        }
    }
}
