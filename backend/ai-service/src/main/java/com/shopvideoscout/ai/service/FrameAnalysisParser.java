package com.shopvideoscout.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.ai.constant.QwenVlConstants;
import com.shopvideoscout.ai.dto.FrameAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Qwen-VL frame analysis responses.
 * Handles JSON parsing with validation and normalization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FrameAnalysisParser {

    private final ObjectMapper objectMapper;

    // Pattern to extract JSON from markdown code blocks
    private static final Pattern JSON_PATTERN = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```|```\\s*([\\s\\S]*?)\\s*```|\\{[\\s\\S]*\\}");

    /**
     * Parse AI response text into FrameAnalysisResult.
     * Handles JSON extraction from markdown blocks and validates fields.
     *
     * @param responseText Raw text response from Qwen-VL
     * @return Parsed and validated FrameAnalysisResult
     * @throws IllegalArgumentException if response cannot be parsed
     */
    public FrameAnalysisResult parseAnalysisResponse(String responseText) {
        String jsonStr = extractJson(responseText);
        if (jsonStr == null) {
            throw new IllegalArgumentException("No valid JSON found in response");
        }

        try {
            JsonNode root = objectMapper.readTree(jsonStr);
            return parseJsonNode(root);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage(), e);
        }
    }

    /**
     * Extract JSON from response text (may be wrapped in markdown code blocks).
     */
    private String extractJson(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        // Try to find JSON in markdown code blocks first
        Matcher matcher = JSON_PATTERN.matcher(text.trim());
        if (matcher.find()) {
            String json = matcher.group(1);
            if (json == null) json = matcher.group(2);
            if (json == null) json = matcher.group(0);
            return json.trim();
        }

        // If the whole text looks like JSON, use it directly
        String trimmed = text.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        return null;
    }

    /**
     * Parse JSON node into FrameAnalysisResult with validation.
     */
    private FrameAnalysisResult parseJsonNode(JsonNode root) {
        FrameAnalysisResult result = new FrameAnalysisResult();

        // Parse and validate category (BR-1.1)
        String category = parseCategory(root);
        result.setCategory(category);

        // Parse and truncate tags (BR-1.2: max 5 tags)
        List<String> tags = parseTags(root);
        result.setTags(tags);

        // Parse and clamp quality score (BR-1.3: 0-100)
        int qualityScore = parseQualityScore(root);
        result.setQualityScore(qualityScore);

        // Parse description
        String description = parseDescription(root);
        result.setDescription(description);

        return result;
    }

    /**
     * Parse category field with validation.
     * Returns "other" if invalid category.
     */
    private String parseCategory(JsonNode root) {
        JsonNode categoryNode = root.get("category");
        if (categoryNode == null || categoryNode.isNull()) {
            return QwenVlConstants.Category.OTHER;
        }

        String category = categoryNode.asText().toLowerCase().trim();
        if (QwenVlConstants.Category.isValid(category)) {
            return category;
        }

        log.warn("Invalid category '{}', defaulting to 'other'", category);
        return QwenVlConstants.Category.OTHER;
    }

    /**
     * Parse tags array with truncation to max 5 (BR-1.2).
     */
    private List<String> parseTags(JsonNode root) {
        List<String> tags = new ArrayList<>();
        JsonNode tagsNode = root.get("tags");

        if (tagsNode == null || !tagsNode.isArray()) {
            return tags;
        }

        int count = 0;
        for (JsonNode tagNode : tagsNode) {
            if (count >= QwenVlConstants.MAX_TAGS) {
                log.debug("Truncating tags to {} (original had more)", QwenVlConstants.MAX_TAGS);
                break;
            }
            String tag = tagNode.asText().trim();
            if (!tag.isEmpty()) {
                tags.add(tag);
                count++;
            }
        }

        return tags;
    }

    /**
     * Parse quality score with clamping to 0-100 range (BR-1.3).
     */
    private int parseQualityScore(JsonNode root) {
        JsonNode scoreNode = root.get("quality_score");
        if (scoreNode == null || scoreNode.isNull()) {
            return 50; // Default to middle score
        }

        int score = scoreNode.asInt(50);

        // Clamp to valid range
        if (score < QwenVlConstants.MIN_QUALITY_SCORE) {
            log.debug("Quality score {} below minimum, clamping to {}", score, QwenVlConstants.MIN_QUALITY_SCORE);
            return QwenVlConstants.MIN_QUALITY_SCORE;
        }
        if (score > QwenVlConstants.MAX_QUALITY_SCORE) {
            log.debug("Quality score {} above maximum, clamping to {}", score, QwenVlConstants.MAX_QUALITY_SCORE);
            return QwenVlConstants.MAX_QUALITY_SCORE;
        }

        return score;
    }

    /**
     * Parse description field.
     */
    private String parseDescription(JsonNode root) {
        JsonNode descNode = root.get("description");
        if (descNode == null || descNode.isNull()) {
            return null;
        }
        return descNode.asText().trim();
    }
}
