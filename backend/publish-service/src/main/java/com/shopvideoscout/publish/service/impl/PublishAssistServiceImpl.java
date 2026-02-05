package com.shopvideoscout.publish.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.publish.client.TaskServiceClient;
import com.shopvideoscout.publish.dto.PublishAssistResponse;
import com.shopvideoscout.publish.dto.ScriptDto;
import com.shopvideoscout.publish.dto.TaskDetailDto;
import com.shopvideoscout.publish.entity.PublishAssist;
import com.shopvideoscout.publish.mapper.PublishAssistMapper;
import com.shopvideoscout.publish.service.PublishAssistService;
import com.shopvideoscout.publish.service.TitleGenerationService;
import com.shopvideoscout.publish.service.TopicGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * Implementation of publish assist orchestration service.
 * Story 5.3: 发布辅助服务 - AC1, AC2, AC3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublishAssistServiceImpl implements PublishAssistService {

    private static final String CACHE_KEY_PREFIX = "publish:assist:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final int MAX_REGENERATE_COUNT = 3;
    private static final double BASE_TEMPERATURE = 0.7;
    private static final double TEMPERATURE_INCREMENT = 0.1;

    private final TaskServiceClient taskServiceClient;
    private final TopicGenerationService topicGenerationService;
    private final TitleGenerationService titleGenerationService;
    private final PublishAssistMapper publishAssistMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public PublishAssistResponse getPublishAssist(Long taskId, Long userId) {
        // 1. Validate task
        TaskDetailDto task = validateAndGetTask(taskId, userId);

        // 2. Check Redis cache
        String cacheKey = CACHE_KEY_PREFIX + taskId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for task {}", taskId);
            return deserializeResponse(cached);
        }

        // 3. Check DB
        PublishAssist entity = publishAssistMapper.selectByTaskId(taskId);
        if (entity != null) {
            log.debug("DB hit for task {}", taskId);
            PublishAssistResponse response = buildResponse(entity);
            cacheResponse(cacheKey, response);
            return response;
        }

        // 4. Generate via AI
        log.info("Generating publish assist for task {}", taskId);
        return generateAndStore(taskId, task, null);
    }

    @Override
    @Transactional
    public PublishAssistResponse regenerate(Long taskId, Long userId) {
        // 1. Validate task
        TaskDetailDto task = validateAndGetTask(taskId, userId);

        // 2. Check regenerate count
        PublishAssist entity = publishAssistMapper.selectByTaskId(taskId);
        int currentCount = entity != null ? entity.getRegenerateCount() : 0;

        if (currentCount >= MAX_REGENERATE_COUNT) {
            throw new BusinessException(ResultCode.PUBLISH_ASSIST_LIMIT_EXCEEDED);
        }

        // 3. Clear cache
        String cacheKey = CACHE_KEY_PREFIX + taskId;
        redisTemplate.delete(cacheKey);

        // 4. Calculate temperature (increases with each regeneration)
        double temperature = BASE_TEMPERATURE + (currentCount * TEMPERATURE_INCREMENT);
        log.info("Regenerating publish assist for task {} with temperature {}", taskId, temperature);

        // 5. Generate with higher temperature
        PublishAssistResponse response = generateAndStore(taskId, task, temperature);

        // 6. Increment regenerate count
        if (entity != null) {
            publishAssistMapper.incrementRegenerateCount(taskId);
        }

        return response;
    }

    /**
     * Validate task exists, is done, and belongs to user.
     */
    private TaskDetailDto validateAndGetTask(Long taskId, Long userId) {
        TaskDetailDto task;
        try {
            task = taskServiceClient.getTask(taskId);
        } catch (Exception e) {
            log.error("Failed to fetch task {}", taskId, e);
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }

        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }

        // Validate ownership
        if (!userId.equals(task.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // Validate status is 'done' (or 'completed')
        String status = task.getStatus();
        if (!"done".equalsIgnoreCase(status) && !"completed".equalsIgnoreCase(status)) {
            throw new BusinessException(ResultCode.OUTPUT_NOT_READY);
        }

        return task;
    }

    /**
     * Generate topics and titles via AI and store to DB.
     */
    private PublishAssistResponse generateAndStore(Long taskId, TaskDetailDto task, Double temperature) {
        // Get script summary
        String scriptSummary = getScriptSummary(taskId);

        // Generate topics
        List<String> topics = topicGenerationService.generateTopics(
                task.getShopName(), task.getShopType(), scriptSummary, temperature);

        // Generate titles
        List<String> titles = titleGenerationService.generateTitles(
                task.getShopName(), task.getShopType(), scriptSummary, temperature);

        // Check if entity exists
        PublishAssist entity = publishAssistMapper.selectByTaskId(taskId);
        if (entity == null) {
            // Create new
            entity = PublishAssist.createNew(taskId, topics, titles);
            publishAssistMapper.insert(entity);
        } else {
            // Update existing
            entity.setTopics(topics);
            entity.setTitles(titles);
            publishAssistMapper.updateById(entity);
        }

        // Cache
        PublishAssistResponse response = buildResponse(entity);
        String cacheKey = CACHE_KEY_PREFIX + taskId;
        cacheResponse(cacheKey, response);

        return response;
    }

    /**
     * Get script summary from task service.
     */
    private String getScriptSummary(Long taskId) {
        try {
            ScriptDto script = taskServiceClient.getScript(taskId);
            if (script != null) {
                return script.getSummary();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch script for task {}", taskId, e);
        }
        return "";
    }

    /**
     * Build response from entity.
     */
    private PublishAssistResponse buildResponse(PublishAssist entity) {
        int remaining = MAX_REGENERATE_COUNT - (entity.getRegenerateCount() != null ? entity.getRegenerateCount() : 0);
        return PublishAssistResponse.builder()
                .topics(entity.getTopics())
                .titles(entity.getTitles())
                .regenerateRemaining(Math.max(0, remaining))
                .build();
    }

    /**
     * Cache response to Redis.
     */
    private void cacheResponse(String cacheKey, PublishAssistResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache publish assist response", e);
        }
    }

    /**
     * Deserialize response from cache.
     */
    private PublishAssistResponse deserializeResponse(String json) {
        try {
            return objectMapper.readValue(json, PublishAssistResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cached response", e);
            return null;
        }
    }
}
