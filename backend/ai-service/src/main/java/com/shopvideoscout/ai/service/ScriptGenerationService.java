package com.shopvideoscout.ai.service;

import com.shopvideoscout.ai.client.DoubaoClient;
import com.shopvideoscout.ai.dto.ScriptContent;
import com.shopvideoscout.ai.dto.ScriptResponse;
import com.shopvideoscout.ai.dto.ShotSummary;
import com.shopvideoscout.ai.entity.Script;
import com.shopvideoscout.ai.entity.VideoFrame;
import com.shopvideoscout.ai.mapper.ScriptMapper;
import com.shopvideoscout.ai.mapper.VideoFrameMapper;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for AI-powered script generation using Doubao API.
 * Handles script creation, regeneration, and retrieval.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptGenerationService {

    private final DoubaoClient doubaoClient;
    private final ScriptPromptBuilder promptBuilder;
    private final ScriptResponseParser responseParser;
    private final ScriptMapper scriptMapper;
    private final VideoFrameMapper videoFrameMapper;

    /**
     * Maximum regeneration attempts per task (BR-2.1).
     */
    public static final int MAX_REGENERATE_COUNT = 5;

    /**
     * Maximum content validation retry attempts.
     */
    private static final int MAX_VALIDATION_RETRIES = 2;

    /**
     * Generate script for a task.
     *
     * @param taskId        Task ID
     * @param shopName      Shop name
     * @param shopType      Shop type
     * @param promotionText Promotion text
     * @param videoStyle    Video style
     * @param regenerateCount Current regeneration count (0 for first generation)
     * @return Generated script content
     */
    @Transactional
    public Script generateScript(Long taskId, String shopName, String shopType,
                                  String promotionText, String videoStyle,
                                  int regenerateCount) {
        log.info("Generating script for task {} (regenerateCount={})", taskId, regenerateCount);

        // Get shot summaries from analyzed video frames
        List<ShotSummary> shotSummaries = getShotSummaries(taskId);

        if (shotSummaries.isEmpty()) {
            log.warn("No analyzed frames found for task {}", taskId);
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "无可用镜头，请先上传并分析视频");
        }

        // Build prompt
        String prompt = promptBuilder.buildPrompt(shopName, shopType, promotionText, videoStyle, shotSummaries);

        // Generate with retry on invalid content
        ScriptContent content = generateWithValidation(prompt, regenerateCount);

        // Check if script exists (update vs insert)
        Script existing = scriptMapper.findByTaskId(taskId);
        if (existing != null) {
            existing.regenerate(content);
            scriptMapper.updateById(existing);
            log.info("Updated script for task {}, version={}", taskId, existing.getVersion());
            return existing;
        }

        // Create new script
        Script script = Script.createNew(taskId, content);
        scriptMapper.insert(script);
        log.info("Created new script for task {}, id={}", taskId, script.getId());

        return script;
    }

    /**
     * Regenerate script with increased temperature.
     *
     * @param taskId        Task ID
     * @param shopName      Shop name
     * @param shopType      Shop type
     * @param promotionText Promotion text
     * @param videoStyle    Video style
     * @param currentCount  Current regeneration count
     * @return Updated script
     */
    @Transactional
    public Script regenerateScript(Long taskId, String shopName, String shopType,
                                    String promotionText, String videoStyle,
                                    int currentCount) {
        // Check regeneration limit (BR-2.1)
        if (currentCount >= MAX_REGENERATE_COUNT) {
            log.warn("Regeneration limit reached for task {}: count={}", taskId, currentCount);
            throw new BusinessException(429, "重新生成次数已达上限，请手动编辑");
        }

        return generateScript(taskId, shopName, shopType, promotionText, videoStyle, currentCount);
    }

    /**
     * Get script by task ID.
     *
     * @param taskId Task ID
     * @return Script entity or null if not found
     */
    public Script getByTaskId(Long taskId) {
        return scriptMapper.findByTaskId(taskId);
    }

    /**
     * Check if regeneration is allowed for a task.
     */
    public boolean canRegenerate(int currentCount) {
        return currentCount < MAX_REGENERATE_COUNT;
    }

    /**
     * Calculate remaining regeneration attempts.
     */
    public int getRemainingRegenerations(int currentCount) {
        return Math.max(0, MAX_REGENERATE_COUNT - currentCount);
    }

    /**
     * Generate script content with validation retry.
     * Retries with increased temperature if content validation fails (BR: 50301).
     */
    private ScriptContent generateWithValidation(String prompt, int regenerateCount) {
        double baseTemperature = doubaoClient.calculateTemperature(regenerateCount);
        int retryCount = 0;

        while (retryCount <= MAX_VALIDATION_RETRIES) {
            double temperature = baseTemperature + (retryCount * 0.05);
            temperature = Math.min(temperature, 0.95);

            try {
                String responseText = doubaoClient.generateScript(prompt, temperature);
                ScriptContent content = responseParser.parse(responseText);

                // Validate business rules
                if (!content.isValid()) {
                    log.warn("Generated content invalid, retry {}/{}", retryCount + 1, MAX_VALIDATION_RETRIES);
                    retryCount++;
                    continue;
                }

                return content;

            } catch (BusinessException e) {
                if (e.getCode() == ResultCode.VALIDATION_ERROR.getCode() && retryCount < MAX_VALIDATION_RETRIES) {
                    log.warn("Content validation failed, retry {}/{}", retryCount + 1, MAX_VALIDATION_RETRIES);
                    retryCount++;
                } else {
                    throw e;
                }
            }
        }

        throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "多次生成均未通过验证，请稍后重试");
    }

    /**
     * Get shot summaries from analyzed video frames.
     */
    private List<ShotSummary> getShotSummaries(Long taskId) {
        List<VideoFrame> frames = videoFrameMapper.findAnalyzedByTaskId(taskId);

        return frames.stream()
                .map(frame -> ShotSummary.builder()
                        .shotId(frame.getId())
                        .category(frame.getCategory())
                        .tags(frame.getTags())
                        .qualityScore(frame.getQualityScore())
                        .recommended(Boolean.TRUE.equals(frame.getIsRecommended()))
                        .build())
                .collect(Collectors.toList());
    }
}
