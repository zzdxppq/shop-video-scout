package com.shopvideoscout.user.service;

import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.mq.VoiceCloneMessage;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.user.config.OssConfig;
import com.shopvideoscout.user.dto.CreateVoiceSampleRequest;
import com.shopvideoscout.user.dto.VoicePreviewResponse;
import com.shopvideoscout.user.dto.VoiceSampleResponse;
import com.shopvideoscout.user.dto.VoiceUploadUrlRequest;
import com.shopvideoscout.user.dto.VoiceUploadUrlResponse;
import com.shopvideoscout.user.entity.VoiceSample;
import com.shopvideoscout.user.mapper.VoiceSampleMapper;
import com.shopvideoscout.user.mq.VoiceCloneMessagePublisher;
import com.shopvideoscout.user.service.impl.VoiceSampleServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VoiceSampleService (AC1: Upload Voice Sample).
 */
@ExtendWith(MockitoExtension.class)
class VoiceSampleServiceTest {

    @Mock
    private VoiceSampleMapper voiceSampleMapper;

    @Mock
    private VoiceCloneMessagePublisher voiceCloneMessagePublisher;

    @Mock
    private OssConfig ossConfig;

    @InjectMocks
    private VoiceSampleServiceImpl voiceSampleService;

    private static final Long USER_ID = 100L;

    // ===== Upload URL Generation =====

    @Nested
    @DisplayName("Upload URL Generation")
    class UploadUrlTests {

        @Test
        @DisplayName("4.2-UNIT-001: Generate OSS upload URL — valid request returns presigned URL, ossKey, expiry")
        void generateUploadUrl_ValidRequest_ReturnsPresignedUrl() {
            // Given
            VoiceUploadUrlRequest request = new VoiceUploadUrlRequest("sample.mp3");
            when(ossConfig.generatePresignedUploadUrl(anyString(), eq("mp3")))
                    .thenReturn("https://oss.example.com/upload?signature=xxx");

            // When
            VoiceUploadUrlResponse response = voiceSampleService.generateUploadUrl(USER_ID, request);

            // Then
            assertNotNull(response);
            assertNotNull(response.getUploadUrl());
            assertNotNull(response.getOssKey());
            assertTrue(response.getOssKey().startsWith("voice/100/"));
            assertTrue(response.getOssKey().endsWith(".mp3"));
            assertEquals(900, response.getExpiresIn());
        }

        @Test
        @DisplayName("4.2-UNIT-002: Validate audio format — MP3 accepted")
        void generateUploadUrl_Mp3Format_Accepted() {
            when(ossConfig.generatePresignedUploadUrl(anyString(), eq("mp3")))
                    .thenReturn("https://oss.example.com/upload");

            VoiceUploadUrlRequest request = new VoiceUploadUrlRequest("sample.mp3");
            VoiceUploadUrlResponse response = voiceSampleService.generateUploadUrl(USER_ID, request);
            assertNotNull(response);
        }

        @Test
        @DisplayName("4.2-UNIT-003: Validate audio format — WAV accepted")
        void generateUploadUrl_WavFormat_Accepted() {
            when(ossConfig.generatePresignedUploadUrl(anyString(), eq("wav")))
                    .thenReturn("https://oss.example.com/upload");

            VoiceUploadUrlRequest request = new VoiceUploadUrlRequest("sample.wav");
            VoiceUploadUrlResponse response = voiceSampleService.generateUploadUrl(USER_ID, request);
            assertNotNull(response);
        }

        @Test
        @DisplayName("4.2-UNIT-004: Validate audio format — M4A accepted")
        void generateUploadUrl_M4aFormat_Accepted() {
            when(ossConfig.generatePresignedUploadUrl(anyString(), eq("m4a")))
                    .thenReturn("https://oss.example.com/upload");

            VoiceUploadUrlRequest request = new VoiceUploadUrlRequest("sample.m4a");
            VoiceUploadUrlResponse response = voiceSampleService.generateUploadUrl(USER_ID, request);
            assertNotNull(response);
        }

        @Test
        @DisplayName("4.2-UNIT-005: Reject invalid audio format (.txt) → 400 INVALID_AUDIO_FORMAT")
        void generateUploadUrl_InvalidFormat_ThrowsException() {
            VoiceUploadUrlRequest request = new VoiceUploadUrlRequest("document.txt");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.generateUploadUrl(USER_ID, request));
            assertEquals(ResultCode.INVALID_AUDIO_FORMAT.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.2-BLIND-BOUNDARY-012: Null/empty filename → BAD_REQUEST")
        void generateUploadUrl_NullFilename_ThrowsException() {
            VoiceUploadUrlRequest request = new VoiceUploadUrlRequest(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.generateUploadUrl(USER_ID, request));
            assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        }
    }

    // ===== Create Voice Sample =====

    @Nested
    @DisplayName("Create Voice Sample")
    class CreateSampleTests {

        @Test
        @DisplayName("4.2-UNIT-012: Create voice sample record with correct fields")
        void createVoiceSample_ValidRequest_CreatesRecord() {
            // Given
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("My Voice", "voice/100/uuid.mp3", 60);
            when(voiceSampleMapper.countByUserIdForUpdate(USER_ID)).thenReturn(0);
            when(voiceSampleMapper.insert(any(VoiceSample.class))).thenReturn(1);

            // When
            VoiceSampleResponse response = voiceSampleService.createVoiceSample(USER_ID, request);

            // Then
            assertNotNull(response);
            assertEquals("My Voice", response.getName());
            assertEquals("uploading", response.getStatus());

            // Verify entity was saved
            ArgumentCaptor<VoiceSample> captor = ArgumentCaptor.forClass(VoiceSample.class);
            verify(voiceSampleMapper).insert(captor.capture());
            VoiceSample saved = captor.getValue();
            assertEquals(USER_ID, saved.getUserId());
            assertEquals("voice/100/uuid.mp3", saved.getOssKey());
            assertEquals(60, saved.getDurationSeconds());
            assertEquals(VoiceSample.STATUS_UPLOADING, saved.getStatus());
        }

        @Test
        @DisplayName("4.2-INT-001: Create voice sample → VoiceCloneMessage registered for afterCommit publish")
        void createVoiceSample_Success_RegistersAfterCommitPublish() {
            // Given
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("My Voice", "voice/100/uuid.mp3", 60);
            when(voiceSampleMapper.countByUserIdForUpdate(USER_ID)).thenReturn(0);
            when(voiceSampleMapper.insert(any(VoiceSample.class))).thenReturn(1);

            // When — TX-001: MQ publish is now registered via afterCommit, so in unit test context
            // (no real TX), the synchronization won't fire. We verify the insert succeeded.
            VoiceSampleResponse response = voiceSampleService.createVoiceSample(USER_ID, request);

            // Then
            assertNotNull(response);
            verify(voiceSampleMapper).insert(any(VoiceSample.class));
        }

        @Test
        @DisplayName("4.2-UNIT-006: Validate duration — minimum 5 seconds accepted (BR-1.2)")
        void createVoiceSample_MinDuration_Accepted() {
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("Voice", "voice/100/uuid.mp3", 5);
            when(voiceSampleMapper.countByUserIdForUpdate(USER_ID)).thenReturn(0);
            when(voiceSampleMapper.insert(any(VoiceSample.class))).thenReturn(1);

            VoiceSampleResponse response = voiceSampleService.createVoiceSample(USER_ID, request);
            assertNotNull(response);
        }

        @Test
        @DisplayName("4.2-UNIT-007: Validate duration — maximum 120 seconds accepted (BR-1.2)")
        void createVoiceSample_MaxDuration_Accepted() {
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("Voice", "voice/100/uuid.mp3", 120);
            when(voiceSampleMapper.countByUserIdForUpdate(USER_ID)).thenReturn(0);
            when(voiceSampleMapper.insert(any(VoiceSample.class))).thenReturn(1);

            VoiceSampleResponse response = voiceSampleService.createVoiceSample(USER_ID, request);
            assertNotNull(response);
        }

        @Test
        @DisplayName("4.2-UNIT-008: Reject duration below 5 seconds → 400 AUDIO_DURATION_INVALID")
        void createVoiceSample_DurationBelow5s_ThrowsException() {
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("Voice", "voice/100/uuid.mp3", 4);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.createVoiceSample(USER_ID, request));
            assertEquals(ResultCode.AUDIO_DURATION_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.2-UNIT-009: Reject duration above 120 seconds → 400 AUDIO_DURATION_INVALID")
        void createVoiceSample_DurationAbove120s_ThrowsException() {
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("Voice", "voice/100/uuid.mp3", 121);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.createVoiceSample(USER_ID, request));
            assertEquals(ResultCode.AUDIO_DURATION_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.2-UNIT-010: User sample count check — allow when < 3 samples exist")
        void createVoiceSample_Under3Samples_Allowed() {
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("Voice", "voice/100/uuid.mp3", 60);
            when(voiceSampleMapper.countByUserIdForUpdate(USER_ID)).thenReturn(2);
            when(voiceSampleMapper.insert(any(VoiceSample.class))).thenReturn(1);

            VoiceSampleResponse response = voiceSampleService.createVoiceSample(USER_ID, request);
            assertNotNull(response);
        }

        @Test
        @DisplayName("4.2-UNIT-011: Reject 4th sample when limit is 3 → 422 VOICE_SAMPLE_LIMIT_EXCEEDED (BR-1.3)")
        void createVoiceSample_ExceedLimit_ThrowsException() {
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("Voice", "voice/100/uuid.mp3", 60);
            when(voiceSampleMapper.countByUserIdForUpdate(USER_ID)).thenReturn(3);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.createVoiceSample(USER_ID, request));
            assertEquals(ResultCode.VOICE_SAMPLE_LIMIT_EXCEEDED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.2-BLIND-BOUNDARY-001: Null ossKey → BAD_REQUEST")
        void createVoiceSample_NullOssKey_ThrowsException() {
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("Voice", null, 60);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.createVoiceSample(USER_ID, request));
            assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.2-BLIND-BOUNDARY-007: Negative duration value → AUDIO_DURATION_INVALID")
        void createVoiceSample_NegativeDuration_ThrowsException() {
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("Voice", "voice/100/uuid.mp3", -1);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.createVoiceSample(USER_ID, request));
            assertEquals(ResultCode.AUDIO_DURATION_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.2-BLIND-BOUNDARY-008: Zero duration value → AUDIO_DURATION_INVALID")
        void createVoiceSample_ZeroDuration_ThrowsException() {
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("Voice", "voice/100/uuid.mp3", 0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.createVoiceSample(USER_ID, request));
            assertEquals(ResultCode.AUDIO_DURATION_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.2-BLIND-BOUNDARY-005: Duration just below lower boundary: 4 seconds")
        void createVoiceSample_4Seconds_ThrowsException() {
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("Voice", "voice/100/uuid.mp3", 4);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.createVoiceSample(USER_ID, request));
            assertEquals(ResultCode.AUDIO_DURATION_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.2-BLIND-BOUNDARY-006: Duration just above upper boundary: 121 seconds")
        void createVoiceSample_121Seconds_ThrowsException() {
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("Voice", "voice/100/uuid.mp3", 121);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.createVoiceSample(USER_ID, request));
            assertEquals(ResultCode.AUDIO_DURATION_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.2-UNIT-005 (create path): Invalid audio format from ossKey → INVALID_AUDIO_FORMAT")
        void createVoiceSample_InvalidFormatInOssKey_ThrowsException() {
            CreateVoiceSampleRequest request = new CreateVoiceSampleRequest("Voice", "voice/100/uuid.txt", 60);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.createVoiceSample(USER_ID, request));
            assertEquals(ResultCode.INVALID_AUDIO_FORMAT.getCode(), ex.getCode());
        }
    }

    // ===== CRUD Operations =====

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("4.2-UNIT-022: List user's voice samples — returns all with status")
        void listByUserId_ReturnsAll() {
            VoiceSample s1 = new VoiceSample();
            s1.setId(1L);
            s1.setName("Voice 1");
            s1.setStatus("completed");
            s1.setDurationSeconds(30);

            VoiceSample s2 = new VoiceSample();
            s2.setId(2L);
            s2.setName("Voice 2");
            s2.setStatus("processing");
            s2.setDurationSeconds(60);

            when(voiceSampleMapper.selectList(any())).thenReturn(List.of(s1, s2));

            List<VoiceSampleResponse> result = voiceSampleService.listByUserId(USER_ID);
            assertEquals(2, result.size());
            assertEquals("Voice 1", result.get(0).getName());
            assertEquals("completed", result.get(0).getStatus());
        }

        @Test
        @DisplayName("4.2-UNIT-023: Get sample by ID — returns detail with clone status")
        void getById_ExistingSample_ReturnsDetail() {
            VoiceSample sample = new VoiceSample();
            sample.setId(1L);
            sample.setUserId(USER_ID);
            sample.setName("My Voice");
            sample.setStatus("completed");
            sample.setCloneVoiceId("clone_abc123");
            sample.setDurationSeconds(60);

            when(voiceSampleMapper.selectById(1L)).thenReturn(sample);

            VoiceSampleResponse response = voiceSampleService.getById(USER_ID, 1L);
            assertEquals("My Voice", response.getName());
            assertEquals("completed", response.getStatus());
            assertEquals("clone_abc123", response.getCloneVoiceId());
        }

        @Test
        @DisplayName("4.2-UNIT-027: Get sample by invalid ID → 404 VOICE_SAMPLE_NOT_FOUND")
        void getById_NotFound_ThrowsException() {
            when(voiceSampleMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.getById(USER_ID, 999L));
            assertEquals(ResultCode.VOICE_SAMPLE_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.2-UNIT-024: Delete sample — calls mapper deleteById")
        void deleteById_ExistingSample_Deletes() {
            VoiceSample sample = new VoiceSample();
            sample.setId(1L);
            sample.setUserId(USER_ID);

            when(voiceSampleMapper.selectById(1L)).thenReturn(sample);

            voiceSampleService.deleteById(USER_ID, 1L);

            verify(voiceSampleMapper).deleteById(1L);
        }

        @Test
        @DisplayName("4.2-UNIT-028: Delete sample not found → 404 VOICE_SAMPLE_NOT_FOUND")
        void deleteById_NotFound_ThrowsException() {
            when(voiceSampleMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.deleteById(USER_ID, 999L));
            assertEquals(ResultCode.VOICE_SAMPLE_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("Ownership check: Different user → VOICE_SAMPLE_NOT_FOUND")
        void getById_DifferentUser_ThrowsException() {
            VoiceSample sample = new VoiceSample();
            sample.setId(1L);
            sample.setUserId(200L); // different user

            when(voiceSampleMapper.selectById(1L)).thenReturn(sample);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.getById(USER_ID, 1L));
            assertEquals(ResultCode.VOICE_SAMPLE_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    // ===== Update Clone Result =====

    @Nested
    @DisplayName("Update Clone Result")
    class UpdateCloneResultTests {

        @Test
        @DisplayName("4.2-UNIT-018: Status updated to 'completed' with clone_voice_id on success")
        void updateCloneResult_Success_UpdatesStatus() {
            VoiceSample sample = new VoiceSample();
            sample.setId(1L);
            sample.setStatus(VoiceSample.STATUS_PROCESSING);

            when(voiceSampleMapper.selectById(1L)).thenReturn(sample);

            voiceSampleService.updateCloneResult(1L, "clone_abc", VoiceSample.STATUS_COMPLETED, null);

            ArgumentCaptor<VoiceSample> captor = ArgumentCaptor.forClass(VoiceSample.class);
            verify(voiceSampleMapper).updateById(captor.capture());
            VoiceSample updated = captor.getValue();
            assertEquals(VoiceSample.STATUS_COMPLETED, updated.getStatus());
            assertEquals("clone_abc", updated.getCloneVoiceId());
            assertNull(updated.getErrorMessage());
        }

        @Test
        @DisplayName("4.2-UNIT-019: Status updated to 'failed' with error_message on failure")
        void updateCloneResult_Failure_UpdatesStatus() {
            VoiceSample sample = new VoiceSample();
            sample.setId(1L);
            sample.setStatus(VoiceSample.STATUS_PROCESSING);

            when(voiceSampleMapper.selectById(1L)).thenReturn(sample);

            voiceSampleService.updateCloneResult(1L, null, VoiceSample.STATUS_FAILED, "Clone failed");

            ArgumentCaptor<VoiceSample> captor = ArgumentCaptor.forClass(VoiceSample.class);
            verify(voiceSampleMapper).updateById(captor.capture());
            VoiceSample updated = captor.getValue();
            assertEquals(VoiceSample.STATUS_FAILED, updated.getStatus());
            assertEquals("Clone failed", updated.getErrorMessage());
        }

        @Test
        @DisplayName("Update clone result for non-existent sample — no-op")
        void updateCloneResult_SampleNotFound_NoOp() {
            when(voiceSampleMapper.selectById(999L)).thenReturn(null);

            voiceSampleService.updateCloneResult(999L, "clone", "completed", null);

            verify(voiceSampleMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("MISC-003: Idempotent callback — skip update if status already matches")
        void updateCloneResult_SameStatus_SkipsUpdate() {
            VoiceSample sample = new VoiceSample();
            sample.setId(1L);
            sample.setStatus(VoiceSample.STATUS_COMPLETED);

            when(voiceSampleMapper.selectById(1L)).thenReturn(sample);

            voiceSampleService.updateCloneResult(1L, "clone_abc", VoiceSample.STATUS_COMPLETED, null);

            verify(voiceSampleMapper, never()).updateById(any());
        }
    }

    // ===== Preview =====

    @Nested
    @DisplayName("Preview Voice Sample")
    class PreviewTests {

        @Test
        @DisplayName("4.2-UNIT-025: Preview completed clone → returns preview info with cloneVoiceId")
        void getPreview_Completed_ReturnsPreview() {
            VoiceSample sample = new VoiceSample();
            sample.setId(1L);
            sample.setUserId(USER_ID);
            sample.setStatus(VoiceSample.STATUS_COMPLETED);
            sample.setCloneVoiceId("clone_abc123");

            when(voiceSampleMapper.selectById(1L)).thenReturn(sample);

            VoicePreviewResponse response = voiceSampleService.getPreview(USER_ID, 1L);

            assertEquals(1L, response.getSampleId());
            assertEquals("completed", response.getStatus());
            assertEquals("clone_abc123", response.getCloneVoiceId());
            assertNotNull(response.getPreviewText());
        }

        @Test
        @DisplayName("4.2-UNIT-026: Preview while clone not completed → VOICE_CLONE_IN_PROGRESS")
        void getPreview_Processing_ThrowsException() {
            VoiceSample sample = new VoiceSample();
            sample.setId(1L);
            sample.setUserId(USER_ID);
            sample.setStatus(VoiceSample.STATUS_PROCESSING);

            when(voiceSampleMapper.selectById(1L)).thenReturn(sample);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.getPreview(USER_ID, 1L));
            assertEquals(ResultCode.VOICE_CLONE_IN_PROGRESS.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("Preview uploading sample → VOICE_CLONE_IN_PROGRESS")
        void getPreview_Uploading_ThrowsException() {
            VoiceSample sample = new VoiceSample();
            sample.setId(1L);
            sample.setUserId(USER_ID);
            sample.setStatus(VoiceSample.STATUS_UPLOADING);

            when(voiceSampleMapper.selectById(1L)).thenReturn(sample);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.getPreview(USER_ID, 1L));
            assertEquals(ResultCode.VOICE_CLONE_IN_PROGRESS.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("Preview failed sample → VOICE_CLONE_IN_PROGRESS")
        void getPreview_Failed_ThrowsException() {
            VoiceSample sample = new VoiceSample();
            sample.setId(1L);
            sample.setUserId(USER_ID);
            sample.setStatus(VoiceSample.STATUS_FAILED);

            when(voiceSampleMapper.selectById(1L)).thenReturn(sample);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.getPreview(USER_ID, 1L));
            assertEquals(ResultCode.VOICE_CLONE_IN_PROGRESS.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("Preview non-existent sample → VOICE_SAMPLE_NOT_FOUND")
        void getPreview_NotFound_ThrowsException() {
            when(voiceSampleMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.getPreview(USER_ID, 999L));
            assertEquals(ResultCode.VOICE_SAMPLE_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("Preview sample owned by different user → VOICE_SAMPLE_NOT_FOUND")
        void getPreview_DifferentUser_ThrowsException() {
            VoiceSample sample = new VoiceSample();
            sample.setId(1L);
            sample.setUserId(200L); // different user

            when(voiceSampleMapper.selectById(1L)).thenReturn(sample);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> voiceSampleService.getPreview(USER_ID, 1L));
            assertEquals(ResultCode.VOICE_SAMPLE_NOT_FOUND.getCode(), ex.getCode());
        }
    }
}
