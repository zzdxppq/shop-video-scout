package com.shopvideoscout.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.mq.VoiceCloneMessage;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.user.config.OssConfig;
import com.shopvideoscout.user.dto.*;
import com.shopvideoscout.user.entity.VoiceSample;
import com.shopvideoscout.user.mapper.VoiceSampleMapper;
import com.shopvideoscout.user.mq.VoiceCloneMessagePublisher;
import com.shopvideoscout.user.service.VoiceSampleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of VoiceSampleService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceSampleServiceImpl implements VoiceSampleService {

    private final VoiceSampleMapper voiceSampleMapper;
    private final VoiceCloneMessagePublisher voiceCloneMessagePublisher;
    private final OssConfig ossConfig;

    private static final Set<String> ALLOWED_AUDIO_EXTENSIONS = Set.of("mp3", "wav", "m4a");
    private static final int PRESIGNED_URL_EXPIRATION_SECONDS = 900; // 15 minutes

    @Override
    public VoiceUploadUrlResponse generateUploadUrl(Long userId, VoiceUploadUrlRequest request) {
        String filename = request.getFilename();
        if (filename == null || filename.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件名不能为空");
        }

        String extension = extractExtension(filename);
        validateAudioFormat(extension);

        String uuid = UUID.randomUUID().toString().replace("-", "");
        String ossKey = String.format("voice/%d/%s.%s", userId, uuid, extension.toLowerCase());

        String uploadUrl = ossConfig.generatePresignedUploadUrl(ossKey, extension);

        return VoiceUploadUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .ossKey(ossKey)
                .expiresIn(PRESIGNED_URL_EXPIRATION_SECONDS)
                .build();
    }

    @Override
    @Transactional
    public VoiceSampleResponse createVoiceSample(Long userId, CreateVoiceSampleRequest request) {
        // Validate ossKey
        if (request.getOssKey() == null || request.getOssKey().isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "OSS路径不能为空");
        }

        // Validate audio format from ossKey extension
        String extension = extractExtension(request.getOssKey());
        validateAudioFormat(extension);

        // Validate duration (BR-1.2: 5s - 2min)
        validateDuration(request.getDurationSeconds());

        // CONC-001: Use FOR UPDATE lock to prevent TOCTOU race on concurrent sample limit checks
        int existingCount = voiceSampleMapper.countByUserIdForUpdate(userId);
        if (existingCount >= VoiceSample.MAX_SAMPLES_PER_USER) {
            throw new BusinessException(ResultCode.VOICE_SAMPLE_LIMIT_EXCEEDED);
        }

        // Create voice sample record
        VoiceSample sample = new VoiceSample();
        sample.setUserId(userId);
        sample.setName(request.getName());
        sample.setOssKey(request.getOssKey());
        sample.setDurationSeconds(request.getDurationSeconds());
        sample.setStatus(VoiceSample.STATUS_UPLOADING);

        voiceSampleMapper.insert(sample);
        log.info("Created voice sample {} for user {}", sample.getId(), userId);

        // TX-001: Publish MQ message after transaction commits to prevent dual-write risk.
        // If TX rolls back, the message is never sent. If MQ publish fails after commit,
        // the sample remains in 'uploading' status (recoverable via retry/reconciliation).
        VoiceCloneMessage mqMessage = VoiceCloneMessage.builder()
                .voiceSampleId(sample.getId())
                .userId(userId)
                .ossKey(request.getOssKey())
                .durationSeconds(request.getDurationSeconds())
                .build();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                voiceCloneMessagePublisher.publish(mqMessage);
            }
        });

        return VoiceSampleResponse.fromEntity(sample);
    }

    @Override
    public List<VoiceSampleResponse> listByUserId(Long userId) {
        LambdaQueryWrapper<VoiceSample> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoiceSample::getUserId, userId)
                .orderByDesc(VoiceSample::getCreatedAt);
        List<VoiceSample> samples = voiceSampleMapper.selectList(wrapper);
        return samples.stream()
                .map(VoiceSampleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public VoiceSampleResponse getById(Long userId, Long sampleId) {
        VoiceSample sample = findSampleWithOwnershipCheck(userId, sampleId);
        return VoiceSampleResponse.fromEntity(sample);
    }

    @Override
    @Transactional
    public void deleteById(Long userId, Long sampleId) {
        VoiceSample sample = findSampleWithOwnershipCheck(userId, sampleId);
        voiceSampleMapper.deleteById(sample.getId());
        log.info("Deleted voice sample {} for user {}", sampleId, userId);
    }

    @Override
    @Transactional
    public void updateCloneResult(Long sampleId, String cloneVoiceId, String status, String errorMessage) {
        VoiceSample sample = voiceSampleMapper.selectById(sampleId);
        if (sample == null) {
            log.warn("Voice sample {} not found for clone result update", sampleId);
            return;
        }

        // MISC-003: Idempotency check — skip update if status already matches
        if (status != null && status.equals(sample.getStatus())) {
            log.info("Voice sample {} already has status {}, skipping duplicate callback", sampleId, status);
            return;
        }

        sample.setCloneVoiceId(cloneVoiceId);
        sample.setStatus(status);
        sample.setErrorMessage(errorMessage);
        voiceSampleMapper.updateById(sample);
        log.info("Updated voice sample {} status to {}", sampleId, status);
    }

    @Override
    public VoicePreviewResponse getPreview(Long userId, Long sampleId) {
        VoiceSample sample = findSampleWithOwnershipCheck(userId, sampleId);

        if (!VoiceSample.STATUS_COMPLETED.equals(sample.getStatus())) {
            throw new BusinessException(ResultCode.VOICE_CLONE_IN_PROGRESS);
        }

        // MISC-002: Hardcoded preview text is an intentional MVP simplification.
        // Future: generate actual TTS audio preview using cloned voice.
        return VoicePreviewResponse.builder()
                .sampleId(sample.getId())
                .previewText("你好，这是我的声音克隆效果预览。")
                .status(sample.getStatus())
                .cloneVoiceId(sample.getCloneVoiceId())
                .build();
    }

    private VoiceSample findSampleWithOwnershipCheck(Long userId, Long sampleId) {
        VoiceSample sample = voiceSampleMapper.selectById(sampleId);
        if (sample == null) {
            throw new BusinessException(ResultCode.VOICE_SAMPLE_NOT_FOUND);
        }
        if (!sample.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.VOICE_SAMPLE_NOT_FOUND);
        }
        return sample;
    }

    private void validateAudioFormat(String extension) {
        if (!ALLOWED_AUDIO_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException(ResultCode.INVALID_AUDIO_FORMAT);
        }
    }

    private void validateDuration(Integer durationSeconds) {
        if (durationSeconds == null
                || durationSeconds < VoiceSample.MIN_DURATION_SECONDS
                || durationSeconds > VoiceSample.MAX_DURATION_SECONDS) {
            throw new BusinessException(ResultCode.AUDIO_DURATION_INVALID);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BusinessException(ResultCode.INVALID_AUDIO_FORMAT);
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
