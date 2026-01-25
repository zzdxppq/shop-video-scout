package com.shopvideoscout.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.ai.dto.ScriptContent;
import com.shopvideoscout.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ScriptResponseParser.
 * Story 3.1 - AC1: Test Specs for response parsing.
 */
class ScriptResponseParserTest {

    private ScriptResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new ScriptResponseParser(new ObjectMapper());
    }

    // 3.1-UNIT-004: Parse valid JSON response into paragraphs array
    @Test
    @DisplayName("Should parse valid JSON response into paragraphs array")
    void parse_validJson_shouldReturnScriptContent() {
        String validJson = """
            {
              "paragraphs": [
                {
                  "id": "para_1",
                  "section": "开场",
                  "shot_id": 101,
                  "text": "家人们！今天给你们探一家超火的店...",
                  "estimated_duration": 8
                },
                {
                  "id": "para_2",
                  "section": "环境展示",
                  "shot_id": 102,
                  "text": "你看这环境多好...",
                  "estimated_duration": 10
                },
                {
                  "id": "para_3",
                  "section": "重点内容",
                  "shot_id": 103,
                  "text": "重点来了...",
                  "estimated_duration": 15
                },
                {
                  "id": "para_4",
                  "section": "优惠信息",
                  "shot_id": 104,
                  "text": "现在有活动...",
                  "estimated_duration": 10
                },
                {
                  "id": "para_5",
                  "section": "结尾互动",
                  "shot_id": 105,
                  "text": "喜欢的点个赞...",
                  "estimated_duration": 17
                }
              ],
              "total_duration": 60
            }
            """;

        ScriptContent content = parser.parse(validJson);

        assertThat(content).isNotNull();
        assertThat(content.getParagraphs()).hasSize(5);
        assertThat(content.getTotalDuration()).isEqualTo(60);
        assertThat(content.getParagraphs().get(0).getShotId()).isEqualTo(101L);
        assertThat(content.getParagraphs().get(0).getSection()).isEqualTo("开场");
    }

    @Test
    @DisplayName("Should parse JSON wrapped in markdown code blocks")
    void parse_markdownWrappedJson_shouldExtractAndParse() {
        String wrappedJson = """
            这是生成的脚本：

            ```json
            {
              "paragraphs": [
                {"id": "para_1", "section": "开场", "shot_id": 101, "text": "hello", "estimated_duration": 10},
                {"id": "para_2", "section": "内容", "shot_id": 102, "text": "world", "estimated_duration": 10},
                {"id": "para_3", "section": "内容2", "shot_id": 103, "text": "test", "estimated_duration": 10},
                {"id": "para_4", "section": "内容3", "shot_id": 104, "text": "test2", "estimated_duration": 10},
                {"id": "para_5", "section": "结尾", "shot_id": 105, "text": "bye", "estimated_duration": 20}
              ],
              "total_duration": 60
            }
            ```

            希望这个脚本对你有帮助！
            """;

        ScriptContent content = parser.parse(wrappedJson);

        assertThat(content).isNotNull();
        assertThat(content.getParagraphs()).hasSize(5);
    }

    // 3.1-BLIND-ERROR-001: Doubao API returns malformed JSON
    @Test
    @DisplayName("Should throw exception for malformed JSON")
    void parse_malformedJson_shouldThrowException() {
        String malformedJson = "{ invalid json here }";

        assertThatThrownBy(() -> parser.parse(malformedJson))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("生成内容异常");
    }

    // 3.1-BLIND-ERROR-002: Doubao API returns valid JSON missing required fields
    @Test
    @DisplayName("Should throw exception for JSON missing paragraphs")
    void parse_missingParagraphs_shouldThrowException() {
        String jsonMissingParagraphs = """
            {
              "total_duration": 60
            }
            """;

        assertThatThrownBy(() -> parser.parse(jsonMissingParagraphs))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("生成内容异常");
    }

    @Test
    @DisplayName("Should throw exception for empty paragraphs array")
    void parse_emptyParagraphs_shouldThrowException() {
        String jsonEmptyParagraphs = """
            {
              "paragraphs": [],
              "total_duration": 60
            }
            """;

        assertThatThrownBy(() -> parser.parse(jsonEmptyParagraphs))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("生成内容异常");
    }

    @Test
    @DisplayName("Should throw exception for empty response text")
    void parse_emptyText_shouldThrowException() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI返回空内容");
    }

    @Test
    @DisplayName("Should throw exception for null response text")
    void parse_nullText_shouldThrowException() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI返回空内容");
    }

    @Test
    @DisplayName("Should assign paragraph IDs if missing")
    void parse_missingParagraphIds_shouldAssignIds() {
        String jsonWithoutIds = """
            {
              "paragraphs": [
                {"section": "开场", "shot_id": 101, "text": "hello", "estimated_duration": 10},
                {"section": "内容", "shot_id": 102, "text": "world", "estimated_duration": 10},
                {"section": "内容2", "shot_id": 103, "text": "test", "estimated_duration": 10},
                {"section": "内容3", "shot_id": 104, "text": "test2", "estimated_duration": 10},
                {"section": "结尾", "shot_id": 105, "text": "bye", "estimated_duration": 20}
              ],
              "total_duration": 60
            }
            """;

        ScriptContent content = parser.parse(jsonWithoutIds);

        assertThat(content.getParagraphs().get(0).getId()).isEqualTo("para_1");
        assertThat(content.getParagraphs().get(4).getId()).isEqualTo("para_5");
    }

    @Test
    @DisplayName("Should extract JSON from text containing other content")
    void parse_jsonWithSurroundingText_shouldExtract() {
        String textWithJson = """
            好的，根据您提供的信息，我为您生成了以下口播脚本：

            {
              "paragraphs": [
                {"id": "para_1", "section": "开场", "shot_id": 101, "text": "hello", "estimated_duration": 10},
                {"id": "para_2", "section": "内容", "shot_id": 102, "text": "world", "estimated_duration": 10},
                {"id": "para_3", "section": "内容2", "shot_id": 103, "text": "test", "estimated_duration": 10},
                {"id": "para_4", "section": "内容3", "shot_id": 104, "text": "test2", "estimated_duration": 10},
                {"id": "para_5", "section": "结尾", "shot_id": 105, "text": "bye", "estimated_duration": 20}
              ],
              "total_duration": 60
            }

            希望这个脚本符合您的需求！
            """;

        ScriptContent content = parser.parse(textWithJson);

        assertThat(content).isNotNull();
        assertThat(content.getParagraphs()).hasSize(5);
    }
}
