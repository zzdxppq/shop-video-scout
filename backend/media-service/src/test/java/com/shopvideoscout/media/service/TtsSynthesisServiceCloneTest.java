package com.shopvideoscout.media.service;

import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.mq.ComposeMessage;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.media.mapper.VoiceSampleReadMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TtsSynthesisService clone voice resolution (AC3: Use Cloned Voice).
 */
@ExtendWith(MockitoExtension.class)
class TtsSynthesisServiceCloneTest {

    @Mock
    private com.shopvideoscout.media.client.VolcanoTtsClient volcanoTtsClient;

    @Mock
    private com.aliyun.oss.OSS ossClient;

    @Mock
    private com.shopvideoscout.media.config.OssConfig ossConfig;

    @Mock
    private ComposeProgressTracker progressTracker;

    @Mock
    private VoiceSampleReadMapper voiceSampleReadMapper;

    @InjectMocks
    private TtsSynthesisService ttsSynthesisService;

    private static final Long USER_ID = 100L;

    @Nested
    @DisplayName("Voice ID Resolution")
    class ResolveVoiceIdTests {

        @Test
        @DisplayName("4.2-UNIT-020: TTS with cloned voice — resolve clone_voice_id from voice_samples (status=completed)")
        void resolveVoiceId_CloneVoiceCompleted_ReturnsCloneId() {
            ComposeMessage.VoiceConfig config = ComposeMessage.VoiceConfig.builder()
                    .type("clone")
                    .voiceSampleId(1L)
                    .userId(USER_ID)
                    .build();

            when(voiceSampleReadMapper.getStatus(1L)).thenReturn("completed");
            when(voiceSampleReadMapper.getUserId(1L)).thenReturn(USER_ID);
            when(voiceSampleReadMapper.getCloneVoiceId(1L)).thenReturn("clone_abc123");

            String voiceId = ttsSynthesisService.resolveVoiceId(config);

            assertEquals("clone_abc123", voiceId);
        }

        @Test
        @DisplayName("4.2-UNIT-021: TTS with clone not ready (status≠completed) → 400 VOICE_CLONE_IN_PROGRESS")
        void resolveVoiceId_CloneNotReady_ThrowsException() {
            ComposeMessage.VoiceConfig config = ComposeMessage.VoiceConfig.builder()
                    .type("clone")
                    .voiceSampleId(1L)
                    .userId(USER_ID)
                    .build();

            when(voiceSampleReadMapper.getStatus(1L)).thenReturn("processing");
            when(voiceSampleReadMapper.getUserId(1L)).thenReturn(USER_ID);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> ttsSynthesisService.resolveVoiceId(config));
            assertEquals(ResultCode.VOICE_CLONE_IN_PROGRESS.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("Sample not found → VOICE_SAMPLE_NOT_FOUND")
        void resolveVoiceId_SampleNotFound_ThrowsException() {
            ComposeMessage.VoiceConfig config = ComposeMessage.VoiceConfig.builder()
                    .type("clone")
                    .voiceSampleId(999L)
                    .userId(USER_ID)
                    .build();

            when(voiceSampleReadMapper.getStatus(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> ttsSynthesisService.resolveVoiceId(config));
            assertEquals(ResultCode.VOICE_SAMPLE_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("Standard voice — returns voiceId directly")
        void resolveVoiceId_StandardVoice_ReturnsDirectly() {
            ComposeMessage.VoiceConfig config = ComposeMessage.VoiceConfig.builder()
                    .type("standard")
                    .voiceId("xiaomei")
                    .build();

            String voiceId = ttsSynthesisService.resolveVoiceId(config);

            assertEquals("xiaomei", voiceId);
        }

        @Test
        @DisplayName("4.2-UNIT-026: Preview while clone not completed → VOICE_CLONE_IN_PROGRESS")
        void resolveVoiceId_CloneFailed_ThrowsException() {
            ComposeMessage.VoiceConfig config = ComposeMessage.VoiceConfig.builder()
                    .type("clone")
                    .voiceSampleId(1L)
                    .userId(USER_ID)
                    .build();

            when(voiceSampleReadMapper.getStatus(1L)).thenReturn("failed");
            when(voiceSampleReadMapper.getUserId(1L)).thenReturn(USER_ID);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> ttsSynthesisService.resolveVoiceId(config));
            assertEquals(ResultCode.VOICE_CLONE_IN_PROGRESS.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("Clone completed but no clone_voice_id → VOICE_CLONE_FAILED")
        void resolveVoiceId_CompletedButNoCloneId_ThrowsException() {
            ComposeMessage.VoiceConfig config = ComposeMessage.VoiceConfig.builder()
                    .type("clone")
                    .voiceSampleId(1L)
                    .userId(USER_ID)
                    .build();

            when(voiceSampleReadMapper.getStatus(1L)).thenReturn("completed");
            when(voiceSampleReadMapper.getUserId(1L)).thenReturn(USER_ID);
            when(voiceSampleReadMapper.getCloneVoiceId(1L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> ttsSynthesisService.resolveVoiceId(config));
            assertEquals(ResultCode.VOICE_CLONE_FAILED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("SEC-002: Different user's voice sample → VOICE_SAMPLE_NOT_FOUND")
        void resolveVoiceId_DifferentUser_ThrowsException() {
            ComposeMessage.VoiceConfig config = ComposeMessage.VoiceConfig.builder()
                    .type("clone")
                    .voiceSampleId(1L)
                    .userId(USER_ID)
                    .build();

            when(voiceSampleReadMapper.getStatus(1L)).thenReturn("completed");
            when(voiceSampleReadMapper.getUserId(1L)).thenReturn(200L); // different user

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> ttsSynthesisService.resolveVoiceId(config));
            assertEquals(ResultCode.VOICE_SAMPLE_NOT_FOUND.getCode(), ex.getCode());
        }
    }
}
