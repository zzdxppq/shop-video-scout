package com.shopvideoscout.task.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.mq.ComposeMessage;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.task.constant.TaskConstants;
import com.shopvideoscout.task.constant.VoiceConstants;
import com.shopvideoscout.task.dto.ComposeResponse;
import com.shopvideoscout.task.entity.Task;
import com.shopvideoscout.task.mapper.ScriptMapper;
import com.shopvideoscout.task.mapper.TaskMapper;
import com.shopvideoscout.task.mq.ComposeMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for triggering compose (TTS synthesis) workflow.
 * Validates task state, builds compose message, publishes to MQ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComposeService {

    private final TaskMapper taskMapper;
    private final ScriptMapper scriptMapper;
    private final ComposeMessagePublisher composeMessagePublisher;
    private final ObjectMapper objectMapper;

    /**
     * Trigger compose for a task.
     * Validates status, loads script, publishes ComposeMessage to MQ.
     *
     * @param taskId task ID
     * @param userId user ID for ownership check
     * @return compose response with status
     */
    @Transactional
    public ComposeResponse triggerCompose(Long taskId, Long userId) {
        // Load and validate task
        Task task = getTaskAndValidateOwnership(taskId, userId);

        // Validate task status: accept both script_edited and voice_set
        String status = task.getStatus();
        if (!TaskConstants.TaskStatus.SCRIPT_EDITED.equals(status)
                && !TaskConstants.TaskStatus.VOICE_SET.equals(status)) {

            if (TaskConstants.TaskStatus.COMPOSING.equals(status)) {
                throw new BusinessException(ResultCode.TASK_ALREADY_COMPOSING);
            }
            throw new BusinessException(ResultCode.TASK_STATUS_INVALID,
                    "当前任务状态为'" + status + "'，无法开始合成");
        }

        // Load script content
        String scriptContent = scriptMapper.findContentByTaskId(taskId);
        if (scriptContent == null || scriptContent.isBlank()) {
            throw new BusinessException(ResultCode.SCRIPT_NOT_FOUND,
                    "任务脚本未找到，请先生成脚本");
        }

        // Parse paragraphs from script JSON
        List<ComposeMessage.Paragraph> paragraphs = parseParagraphs(scriptContent);
        if (paragraphs.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "脚本无段落内容");
        }

        // Build compose message with voice config
        String voiceType = task.getVoiceType() != null
                ? task.getVoiceType() : VoiceConstants.DEFAULT_VOICE_TYPE;

        ComposeMessage.VoiceConfig voiceConfig;
        if (task.getVoiceSampleId() != null) {
            // Clone voice: TtsSynthesisService will resolve clone_voice_id via DB lookup
            // SEC-002: Pass userId for ownership verification in media-service
            voiceConfig = ComposeMessage.VoiceConfig.builder()
                    .type("clone")
                    .voiceSampleId(task.getVoiceSampleId())
                    .userId(userId)
                    .build();
        } else {
            voiceConfig = ComposeMessage.VoiceConfig.builder()
                    .type("standard")
                    .voiceId(voiceType)
                    .build();
        }

        ComposeMessage message = ComposeMessage.builder()
                .taskId(taskId)
                .paragraphs(paragraphs)
                .voiceConfig(voiceConfig)
                .callbackUrl("http://task-service/internal/tasks/" + taskId + "/compose-complete")
                .build();

        // Update task status to composing (atomic)
        task.setStatus(TaskConstants.TaskStatus.COMPOSING);
        taskMapper.updateById(task);

        // Publish to MQ
        composeMessagePublisher.publish(message);

        log.info("Compose triggered for task {}, {} paragraphs, voice: {}",
                taskId, paragraphs.size(), voiceType);

        return ComposeResponse.builder()
                .status(TaskConstants.TaskStatus.COMPOSING)
                .taskId(taskId)
                .build();
    }

    /**
     * Update voice type for a task.
     *
     * @param taskId    task ID
     * @param userId    user ID
     * @param voiceType new voice type
     * @param voiceSampleId optional voice sample ID
     */
    @Transactional
    public void updateVoiceType(Long taskId, Long userId, String voiceType, Long voiceSampleId) {
        Task task = getTaskAndValidateOwnership(taskId, userId);

        // Validate voice type
        if (!VoiceConstants.VALID_VOICE_TYPES.contains(voiceType)) {
            throw new BusinessException(ResultCode.INVALID_VOICE_TYPE,
                    "不支持的音色类型: " + voiceType);
        }

        task.setVoiceType(voiceType);
        task.setVoiceSampleId(voiceSampleId);

        // Transition status to voice_set if currently script_edited
        if (TaskConstants.TaskStatus.SCRIPT_EDITED.equals(task.getStatus())) {
            task.setStatus(TaskConstants.TaskStatus.VOICE_SET);
        }

        taskMapper.updateById(task);
        log.info("Voice type updated for task {}: {}", taskId, voiceType);
    }

    private Task getTaskAndValidateOwnership(Long taskId, Long userId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问此任务");
        }
        return task;
    }

    /**
     * Parse paragraphs from script content JSON.
     * Expected format: {"paragraphs": [{"id": "...", "text": "...", ...}, ...]}
     */
    List<ComposeMessage.Paragraph> parseParagraphs(String scriptContent) {
        try {
            JsonNode root = objectMapper.readTree(scriptContent);
            JsonNode paragraphsNode = root.get("paragraphs");
            if (paragraphsNode == null || !paragraphsNode.isArray()) {
                return List.of();
            }

            List<ComposeMessage.Paragraph> paragraphs = new ArrayList<>();
            for (int i = 0; i < paragraphsNode.size(); i++) {
                JsonNode pNode = paragraphsNode.get(i);
                String text = pNode.has("text") ? pNode.get("text").asText() : "";
                if (!text.isBlank()) {
                    paragraphs.add(ComposeMessage.Paragraph.builder()
                            .index(i)
                            .text(text)
                            .build());
                }
            }
            return paragraphs;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse script content: {}", e.getMessage());
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "脚本内容解析失败");
        }
    }
}
