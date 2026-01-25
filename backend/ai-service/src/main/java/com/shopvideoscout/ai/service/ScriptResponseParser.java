package com.shopvideoscout.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.ai.dto.ScriptContent;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Doubao API responses into ScriptContent objects.
 * Handles extraction of JSON from AI-generated text responses.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScriptResponseParser {

    private final ObjectMapper objectMapper;

    // Pattern to extract JSON from markdown code blocks or raw JSON
    private static final Pattern JSON_PATTERN = Pattern.compile(
            "```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```|^\\s*(\\{[\\s\\S]*\\})\\s*$",
            Pattern.MULTILINE
    );

    /**
     * Parse AI response text into ScriptContent.
     *
     * @param responseText Raw text from Doubao API
     * @return Parsed ScriptContent
     * @throws BusinessException if parsing fails
     */
    public ScriptContent parse(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            log.error("Empty response text from AI");
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI返回空内容");
        }

        // Extract JSON from response (may be wrapped in markdown code blocks)
        String jsonStr = extractJson(responseText);
        if (jsonStr == null) {
            log.error("No JSON found in AI response: {}", truncate(responseText, 200));
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "生成内容异常，正在重新生成");
        }

        try {
            ScriptContent content = objectMapper.readValue(jsonStr, ScriptContent.class);

            // Validate required fields
            if (content.getParagraphs() == null || content.getParagraphs().isEmpty()) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "生成内容异常，正在重新生成");
            }

            // Ensure IDs are set for paragraphs
            ensureParagraphIds(content);

            log.debug("Parsed script with {} paragraphs, duration={}s",
                    content.getParagraphs().size(), content.getTotalDuration());

            return content;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON from AI response: {}", e.getMessage());
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "生成内容异常，正在重新生成");
        }
    }

    /**
     * Extract JSON from AI response text.
     * Handles markdown code blocks and raw JSON.
     */
    private String extractJson(String text) {
        // First try regex to extract from code blocks
        Matcher matcher = JSON_PATTERN.matcher(text.trim());
        if (matcher.find()) {
            String json = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (json != null && !json.isBlank()) {
                return json.trim();
            }
        }

        // Try to find JSON object directly
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        return null;
    }

    /**
     * Ensure all paragraphs have IDs assigned.
     */
    private void ensureParagraphIds(ScriptContent content) {
        List<ScriptContent.ScriptParagraph> paragraphs = content.getParagraphs();
        for (int i = 0; i < paragraphs.size(); i++) {
            ScriptContent.ScriptParagraph p = paragraphs.get(i);
            if (p.getId() == null || p.getId().isBlank()) {
                p.setId("para_" + (i + 1));
            }
        }
    }

    /**
     * Truncate string for logging.
     */
    private String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
