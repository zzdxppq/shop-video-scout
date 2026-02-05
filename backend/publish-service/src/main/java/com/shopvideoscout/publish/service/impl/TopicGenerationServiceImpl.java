package com.shopvideoscout.publish.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.common.doubao.DoubaoClient;
import com.shopvideoscout.publish.prompt.PublishAssistPromptBuilder;
import com.shopvideoscout.publish.service.TopicGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of topic generation service.
 * Story 5.3: 发布辅助服务 - AC1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopicGenerationServiceImpl implements TopicGenerationService {

    private static final int MAX_TOPIC_LENGTH = 20;
    private static final List<String> DEFAULT_TOPICS = List.of(
            "#探店", "#美食探店", "#必吃榜", "#探店vlog",
            "#美食推荐", "#周末去哪吃", "#吃货日常", "#本地美食"
    );

    private final DoubaoClient doubaoClient;
    private final PublishAssistPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> generateTopics(String shopName, String shopType, String scriptSummary, Double temperature) {
        try {
            String prompt = promptBuilder.buildPrompt(shopName, shopType, scriptSummary);
            String response = doubaoClient.chat(prompt, temperature);
            return parseTopics(response);
        } catch (Exception e) {
            log.warn("Failed to generate topics via AI, using defaults", e);
            return getDefaultTopics();
        }
    }

    @Override
    public List<String> getDefaultTopics() {
        return new ArrayList<>(DEFAULT_TOPICS);
    }

    /**
     * Parse topics from AI response JSON.
     */
    List<String> parseTopics(String response) {
        try {
            // Extract JSON from response (may be wrapped in markdown code blocks)
            String json = extractJson(response);
            JsonNode root = objectMapper.readTree(json);
            JsonNode topicsNode = root.get("topics");

            if (topicsNode == null || !topicsNode.isArray()) {
                log.warn("Invalid topics response format: {}", response);
                return getDefaultTopics();
            }

            List<String> topics = new ArrayList<>();
            for (JsonNode node : topicsNode) {
                String topic = sanitizeTopic(node.asText());
                if (topic != null && !topic.isEmpty()) {
                    topics.add(topic);
                }
            }

            if (topics.isEmpty()) {
                return getDefaultTopics();
            }

            // Limit to 10 topics
            return topics.size() > 10 ? topics.subList(0, 10) : topics;

        } catch (JsonProcessingException e) {
            log.warn("Failed to parse topics JSON: {}", response, e);
            return getDefaultTopics();
        }
    }

    /**
     * Extract JSON from response that may contain markdown code blocks.
     */
    private String extractJson(String response) {
        if (response == null) {
            return "{}";
        }
        String trimmed = response.trim();

        // Remove markdown code blocks if present
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        return trimmed.trim();
    }

    /**
     * Sanitize a single topic.
     * - Ensure # prefix
     * - Remove spaces
     * - Truncate to max length
     */
    String sanitizeTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return null;
        }

        String sanitized = topic.trim().replace(" ", "");

        // Ensure # prefix
        if (!sanitized.startsWith("#")) {
            sanitized = "#" + sanitized;
        }

        // Truncate to max length
        if (sanitized.length() > MAX_TOPIC_LENGTH) {
            sanitized = sanitized.substring(0, MAX_TOPIC_LENGTH);
        }

        return sanitized;
    }
}
