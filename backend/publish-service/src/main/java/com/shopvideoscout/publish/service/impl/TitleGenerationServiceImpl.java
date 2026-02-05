package com.shopvideoscout.publish.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.common.doubao.DoubaoClient;
import com.shopvideoscout.publish.prompt.PublishAssistPromptBuilder;
import com.shopvideoscout.publish.service.TitleGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of title generation service.
 * Story 5.3: 发布辅助服务 - AC2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TitleGenerationServiceImpl implements TitleGenerationService {

    private static final int MIN_TITLE_LENGTH = 20;
    private static final int MAX_TITLE_LENGTH = 50;

    private final DoubaoClient doubaoClient;
    private final PublishAssistPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> generateTitles(String shopName, String shopType, String scriptSummary, Double temperature) {
        try {
            String prompt = promptBuilder.buildPrompt(shopName, shopType, scriptSummary);
            String response = doubaoClient.chat(prompt, temperature);
            return parseTitles(response);
        } catch (Exception e) {
            log.warn("Failed to generate titles via AI, using defaults", e);
            return getDefaultTitles(shopName);
        }
    }

    @Override
    public List<String> getDefaultTitles(String shopName) {
        String name = shopName != null ? shopName : "这家店";
        return List.of(
                name + "探店｜必吃推荐",
                "发现" + name + "的宝藏吃法",
                name + "真实测评，值不值得去？"
        );
    }

    /**
     * Parse titles from AI response JSON.
     */
    List<String> parseTitles(String response) {
        try {
            String json = extractJson(response);
            JsonNode root = objectMapper.readTree(json);
            JsonNode titlesNode = root.get("titles");

            if (titlesNode == null || !titlesNode.isArray()) {
                log.warn("Invalid titles response format: {}", response);
                return getDefaultTitles(null);
            }

            List<String> titles = new ArrayList<>();
            for (JsonNode node : titlesNode) {
                String title = sanitizeTitle(node.asText());
                if (title != null) {
                    titles.add(title);
                }
            }

            if (titles.isEmpty()) {
                return getDefaultTitles(null);
            }

            // Limit to 5 titles
            return titles.size() > 5 ? titles.subList(0, 5) : titles;

        } catch (JsonProcessingException e) {
            log.warn("Failed to parse titles JSON: {}", response, e);
            return getDefaultTitles(null);
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
     * Sanitize a single title.
     * - Filter titles that are too short
     * - Truncate titles that are too long
     */
    String sanitizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }

        String sanitized = title.trim();

        // Filter out titles that are too short
        if (sanitized.length() < MIN_TITLE_LENGTH) {
            log.debug("Title too short ({}): {}", sanitized.length(), sanitized);
            return null;
        }

        // Truncate titles that are too long
        if (sanitized.length() > MAX_TITLE_LENGTH) {
            // Try to find a good break point
            int breakPoint = MAX_TITLE_LENGTH - 3;
            // Look for a natural break (space, punctuation)
            for (int i = breakPoint; i > MAX_TITLE_LENGTH - 10; i--) {
                char c = sanitized.charAt(i);
                if (c == ' ' || c == '，' || c == '。' || c == '！' || c == '？') {
                    breakPoint = i;
                    break;
                }
            }
            sanitized = sanitized.substring(0, breakPoint) + "...";
        }

        return sanitized;
    }
}
