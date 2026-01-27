package com.shopvideoscout.media.service;

import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.mq.VoiceCloneMessage;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.media.client.VolcanoTtsClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VoiceCloneService (AC2: Voice Clone Processing).
 */
@ExtendWith(MockitoExtension.class)
class VoiceCloneServiceTest {

    @Mock
    private VolcanoTtsClient volcanoTtsClient;

    @InjectMocks
    private VoiceCloneService voiceCloneService;

    private VoiceCloneMessage createMessage() {
        return VoiceCloneMessage.builder()
                .voiceSampleId(1L)
                .userId(100L)
                .ossKey("voice/100/uuid.mp3")
                .durationSeconds(60)
                .build();
    }

    @Test
    @DisplayName("4.2-UNIT-013: Successful Seed-ICL call → returns clone_voice_id")
    void processClone_Success_ReturnsCloneVoiceId() {
        VoiceCloneMessage message = createMessage();
        when(volcanoTtsClient.cloneVoice("voice/100/uuid.mp3")).thenReturn("clone_voice_abc123");

        String result = voiceCloneService.processClone(message);

        assertEquals("clone_voice_abc123", result);
        verify(volcanoTtsClient, times(1)).cloneVoice("voice/100/uuid.mp3");
    }

    @Test
    @DisplayName("4.2-UNIT-014: Seed-ICL API failure → throws VOICE_CLONE_FAILED")
    void processClone_AllRetrysFail_ThrowsException() {
        VoiceCloneMessage message = createMessage();
        when(volcanoTtsClient.cloneVoice("voice/100/uuid.mp3"))
                .thenThrow(new RuntimeException("API timeout"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> voiceCloneService.processClone(message));
        assertEquals(ResultCode.VOICE_CLONE_FAILED.getCode(), ex.getCode());

        // Should retry up to 3 times total (1 initial + 2 retries)
        verify(volcanoTtsClient, times(3)).cloneVoice("voice/100/uuid.mp3");
    }

    @Test
    @DisplayName("4.2-UNIT-016: Retry on transient failure — up to 2 retries with success on 2nd attempt")
    void processClone_TransientFailureThenSuccess_Retries() {
        VoiceCloneMessage message = createMessage();
        when(volcanoTtsClient.cloneVoice("voice/100/uuid.mp3"))
                .thenThrow(new RuntimeException("timeout"))
                .thenReturn("clone_voice_abc123");

        String result = voiceCloneService.processClone(message);

        assertEquals("clone_voice_abc123", result);
        verify(volcanoTtsClient, times(2)).cloneVoice("voice/100/uuid.mp3");
    }

    @Test
    @DisplayName("4.2-UNIT-017: Status transition tracking — process is called with correct params")
    void processClone_VerifyMessagePassedCorrectly() {
        VoiceCloneMessage message = createMessage();
        when(volcanoTtsClient.cloneVoice("voice/100/uuid.mp3")).thenReturn("clone_id");

        voiceCloneService.processClone(message);

        verify(volcanoTtsClient).cloneVoice("voice/100/uuid.mp3");
    }
}
