package com.shopvideoscout.media.mq;

import com.shopvideoscout.common.mq.VoiceCloneMessage;
import com.shopvideoscout.media.service.VoiceCloneCallbackClient;
import com.shopvideoscout.media.service.VoiceCloneService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for VoiceCloneMessageConsumer (AC2: MQ integration).
 */
@ExtendWith(MockitoExtension.class)
class VoiceCloneMessageConsumerTest {

    @Mock
    private VoiceCloneService voiceCloneService;

    @Mock
    private VoiceCloneCallbackClient voiceCloneCallbackClient;

    @InjectMocks
    private VoiceCloneMessageConsumer consumer;

    private VoiceCloneMessage createMessage() {
        return VoiceCloneMessage.builder()
                .voiceSampleId(1L)
                .userId(100L)
                .ossKey("voice/100/uuid.mp3")
                .durationSeconds(60)
                .build();
    }

    @Test
    @DisplayName("4.2-UNIT-015: Clone consumer processes VoiceCloneMessage → invokes VoiceCloneService")
    void handleMessage_Success_InvokesService() {
        VoiceCloneMessage message = createMessage();
        when(voiceCloneService.processClone(message)).thenReturn("clone_voice_id_123");

        consumer.handleVoiceCloneMessage(message);

        verify(voiceCloneService).processClone(message);
        verify(voiceCloneCallbackClient).notifyCloneComplete(1L, "clone_voice_id_123");
    }

    @Test
    @DisplayName("4.2-INT-007/008: Successful clone → callback with completed status")
    void handleMessage_Success_CallbackComplete() {
        VoiceCloneMessage message = createMessage();
        when(voiceCloneService.processClone(message)).thenReturn("clone_id");

        consumer.handleVoiceCloneMessage(message);

        verify(voiceCloneCallbackClient).notifyCloneComplete(1L, "clone_id");
        verify(voiceCloneCallbackClient, never()).notifyCloneFailed(anyLong(), anyString());
    }

    @Test
    @DisplayName("4.2-UNIT-014 (consumer path): Clone failure → callback with failed status")
    void handleMessage_Failure_CallbackFailed() {
        VoiceCloneMessage message = createMessage();
        when(voiceCloneService.processClone(message))
                .thenThrow(new RuntimeException("Clone failed"));

        consumer.handleVoiceCloneMessage(message);

        verify(voiceCloneCallbackClient).notifyCloneFailed(eq(1L), contains("Clone failed"));
        verify(voiceCloneCallbackClient, never()).notifyCloneComplete(anyLong(), anyString());
    }
}
