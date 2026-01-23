package com.shopvideoscout.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.ai.dto.FrameAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for FrameAnalysisParser.
 * Covers test scenarios from QA Test Design:
 * - 2.3-UNIT-002: Parse Qwen-VL JSON response correctly
 * - 2.3-UNIT-003: Validate category enum
 * - 2.3-UNIT-004: Validate tag count ≤5
 * - 2.3-UNIT-005: Validate quality score range 0-100
 * - 2.3-BLIND-ERROR-001: Qwen-VL returns malformed JSON
 * - 2.3-BLIND-BOUNDARY-003: Quality score at boundaries (0 and 100)
 * - 2.3-BLIND-BOUNDARY-004: Max tags exceeded in Qwen-VL response (>5)
 */
class FrameAnalysisParserTest {

    private FrameAnalysisParser parser;

    @BeforeEach
    void setUp() {
        parser = new FrameAnalysisParser(new ObjectMapper());
    }

    @Nested
    @DisplayName("2.3-UNIT-002: Parse Qwen-VL JSON response correctly")
    class ParseValidJsonTests {

        @Test
        @DisplayName("Should parse valid JSON response with all fields")
        void shouldParseValidJsonWithAllFields() {
            String json = """
                {
                  "category": "food",
                  "tags": ["美食", "色彩丰富", "摆盘精美"],
                  "quality_score": 85,
                  "description": "一道精美的中式美食"
                }
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);

            assertThat(result.getCategory()).isEqualTo("food");
            assertThat(result.getTags()).containsExactly("美食", "色彩丰富", "摆盘精美");
            assertThat(result.getQualityScore()).isEqualTo(85);
            assertThat(result.getDescription()).isEqualTo("一道精美的中式美食");
        }

        @Test
        @DisplayName("Should parse JSON from markdown code block")
        void shouldParseJsonFromMarkdownCodeBlock() {
            String response = """
                ```json
                {
                  "category": "person",
                  "tags": ["人物", "户外"],
                  "quality_score": 70,
                  "description": "一个人在户外"
                }
                ```
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(response);

            assertThat(result.getCategory()).isEqualTo("person");
            assertThat(result.getTags()).hasSize(2);
            assertThat(result.getQualityScore()).isEqualTo(70);
        }

        @Test
        @DisplayName("Should handle missing optional fields with defaults")
        void shouldHandleMissingFields() {
            String json = """
                {
                  "category": "environment"
                }
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);

            assertThat(result.getCategory()).isEqualTo("environment");
            assertThat(result.getTags()).isEmpty();
            assertThat(result.getQualityScore()).isEqualTo(50); // Default
            assertThat(result.getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("2.3-UNIT-003: Validate category enum")
    class CategoryValidationTests {

        @Test
        @DisplayName("Should accept valid category: food")
        void shouldAcceptFoodCategory() {
            String json = """
                {"category": "food", "tags": [], "quality_score": 50}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getCategory()).isEqualTo("food");
        }

        @Test
        @DisplayName("Should accept valid category: person")
        void shouldAcceptPersonCategory() {
            String json = """
                {"category": "person", "tags": [], "quality_score": 50}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getCategory()).isEqualTo("person");
        }

        @Test
        @DisplayName("Should accept valid category: environment")
        void shouldAcceptEnvironmentCategory() {
            String json = """
                {"category": "environment", "tags": [], "quality_score": 50}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getCategory()).isEqualTo("environment");
        }

        @Test
        @DisplayName("Should accept valid category: other")
        void shouldAcceptOtherCategory() {
            String json = """
                {"category": "other", "tags": [], "quality_score": 50}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getCategory()).isEqualTo("other");
        }

        @Test
        @DisplayName("Should default to 'other' for invalid category")
        void shouldDefaultToOtherForInvalidCategory() {
            String json = """
                {"category": "invalid_category", "tags": [], "quality_score": 50}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getCategory()).isEqualTo("other");
        }

        @Test
        @DisplayName("Should default to 'other' for missing category")
        void shouldDefaultToOtherForMissingCategory() {
            String json = """
                {"tags": [], "quality_score": 50}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getCategory()).isEqualTo("other");
        }

        @Test
        @DisplayName("Should handle case-insensitive category")
        void shouldHandleCaseInsensitiveCategory() {
            String json = """
                {"category": "FOOD", "tags": [], "quality_score": 50}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getCategory()).isEqualTo("food");
        }
    }

    @Nested
    @DisplayName("2.3-UNIT-004 & 2.3-BLIND-BOUNDARY-004: Tag count validation")
    class TagValidationTests {

        @Test
        @DisplayName("Should accept exactly 5 tags")
        void shouldAcceptExactly5Tags() {
            String json = """
                {"category": "food", "tags": ["1", "2", "3", "4", "5"], "quality_score": 50}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getTags()).hasSize(5);
        }

        @Test
        @DisplayName("Should truncate tags to 5 when more provided")
        void shouldTruncateTagsTo5() {
            String json = """
                {"category": "food", "tags": ["1", "2", "3", "4", "5", "6", "7"], "quality_score": 50}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getTags()).hasSize(5);
            assertThat(result.getTags()).containsExactly("1", "2", "3", "4", "5");
        }

        @Test
        @DisplayName("Should handle empty tags array")
        void shouldHandleEmptyTags() {
            String json = """
                {"category": "food", "tags": [], "quality_score": 50}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getTags()).isEmpty();
        }

        @Test
        @DisplayName("Should filter out empty string tags")
        void shouldFilterEmptyStringTags() {
            String json = """
                {"category": "food", "tags": ["tag1", "", "tag2", "  ", "tag3"], "quality_score": 50}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getTags()).containsExactly("tag1", "tag2", "tag3");
        }
    }

    @Nested
    @DisplayName("2.3-UNIT-005 & 2.3-BLIND-BOUNDARY-003: Quality score validation")
    class QualityScoreTests {

        @Test
        @DisplayName("Should accept score at lower boundary (0)")
        void shouldAcceptScoreAtLowerBoundary() {
            String json = """
                {"category": "food", "tags": [], "quality_score": 0}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getQualityScore()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should accept score at upper boundary (100)")
        void shouldAcceptScoreAtUpperBoundary() {
            String json = """
                {"category": "food", "tags": [], "quality_score": 100}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getQualityScore()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should accept score at middle value (50)")
        void shouldAcceptScoreAtMiddle() {
            String json = """
                {"category": "food", "tags": [], "quality_score": 50}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getQualityScore()).isEqualTo(50);
        }

        @Test
        @DisplayName("Should clamp score below minimum to 0")
        void shouldClampScoreBelowMinimum() {
            String json = """
                {"category": "food", "tags": [], "quality_score": -10}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getQualityScore()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should clamp score above maximum to 100")
        void shouldClampScoreAboveMaximum() {
            String json = """
                {"category": "food", "tags": [], "quality_score": 150}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getQualityScore()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should default to 50 when score is missing")
        void shouldDefaultToMiddleWhenMissing() {
            String json = """
                {"category": "food", "tags": []}
                """;

            FrameAnalysisResult result = parser.parseAnalysisResponse(json);
            assertThat(result.getQualityScore()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("2.3-BLIND-ERROR-001: Malformed JSON handling")
    class MalformedJsonTests {

        @Test
        @DisplayName("Should throw exception for completely invalid JSON")
        void shouldThrowForInvalidJson() {
            String invalidJson = "this is not json";

            assertThatThrownBy(() -> parser.parseAnalysisResponse(invalidJson))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No valid JSON found");
        }

        @Test
        @DisplayName("Should throw exception for empty response")
        void shouldThrowForEmptyResponse() {
            assertThatThrownBy(() -> parser.parseAnalysisResponse(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw exception for null response")
        void shouldThrowForNullResponse() {
            assertThatThrownBy(() -> parser.parseAnalysisResponse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw exception for truncated JSON")
        void shouldThrowForTruncatedJson() {
            String truncated = """
                {"category": "food", "tags":
                """;

            assertThatThrownBy(() -> parser.parseAnalysisResponse(truncated))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
