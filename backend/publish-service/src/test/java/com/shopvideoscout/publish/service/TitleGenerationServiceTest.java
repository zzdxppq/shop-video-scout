package com.shopvideoscout.publish.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.common.doubao.DoubaoClient;
import com.shopvideoscout.publish.prompt.PublishAssistPromptBuilder;
import com.shopvideoscout.publish.service.impl.TitleGenerationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TitleGenerationService.
 * Story 5.3: 发布辅助服务 - AC2
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TitleGenerationService Tests")
class TitleGenerationServiceTest {

    @Mock
    private DoubaoClient doubaoClient;

    @Mock
    private PublishAssistPromptBuilder promptBuilder;

    private TitleGenerationServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new TitleGenerationServiceImpl(doubaoClient, promptBuilder, objectMapper);
    }

    @Nested
    @DisplayName("generateTitles")
    class GenerateTitles {

        @Test
        @DisplayName("should return titles from AI response")
        void shouldReturnTitlesFromAiResponse() {
            // Given
            String shopName = "海底捞";
            String shopType = "火锅";
            String scriptSummary = "测试脚本摘要";
            String aiResponse = """
                {
                    "titles": [
                        "海底捞探店｜必吃推荐来了，这几道菜绝不踩雷",
                        "发现海底捞的宝藏吃法，人均80吃到撑"
                    ]
                }
                """;

            when(promptBuilder.buildPrompt(shopName, shopType, scriptSummary))
                    .thenReturn("test prompt");
            when(doubaoClient.chat(eq("test prompt"), any()))
                    .thenReturn(aiResponse);

            // When
            List<String> titles = service.generateTitles(shopName, shopType, scriptSummary, null);

            // Then
            assertThat(titles).hasSize(2);
            assertThat(titles.get(0)).contains("海底捞");
        }

        @Test
        @DisplayName("should return default titles when AI fails")
        void shouldReturnDefaultTitlesWhenAiFails() {
            // Given
            String shopName = "测试店铺";
            when(promptBuilder.buildPrompt(any(), any(), any()))
                    .thenReturn("test prompt");
            when(doubaoClient.chat(any(), any()))
                    .thenThrow(new RuntimeException("API error"));

            // When
            List<String> titles = service.generateTitles(shopName, "类型", "摘要", null);

            // Then
            assertThat(titles).isNotEmpty();
            assertThat(titles.get(0)).contains("测试店铺");
        }

        @Test
        @DisplayName("should handle markdown code block wrapped JSON")
        void shouldHandleMarkdownCodeBlockWrappedJson() {
            // Given
            String aiResponse = """
                ```json
                {
                    "titles": ["测试标题需要达到二十个字符才能通过验证的"]
                }
                ```
                """;

            when(promptBuilder.buildPrompt(any(), any(), any())).thenReturn("prompt");
            when(doubaoClient.chat(any(), any())).thenReturn(aiResponse);

            // When
            List<String> titles = service.generateTitles("店名", "类型", "摘要", null);

            // Then
            assertThat(titles).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("parseTitles")
    class ParseTitles {

        @Test
        @DisplayName("should parse valid JSON with titles array")
        void shouldParseValidJsonWithTitlesArray() {
            // Given
            String json = """
                {"titles": [
                    "这是第一个测试标题需要足够长才能通过",
                    "这是第二个测试标题也需要足够长才行"
                ]}
                """;

            // When
            List<String> titles = service.parseTitles(json);

            // Then
            assertThat(titles).hasSize(2);
        }

        @Test
        @DisplayName("should return defaults for invalid JSON")
        void shouldReturnDefaultsForInvalidJson() {
            // When
            List<String> titles = service.parseTitles("not valid json");

            // Then
            assertThat(titles).isNotEmpty();
        }

        @Test
        @DisplayName("should return defaults when titles is not array")
        void shouldReturnDefaultsWhenTitlesIsNotArray() {
            // Given
            String json = """
                {"titles": "not an array"}
                """;

            // When
            List<String> titles = service.parseTitles(json);

            // Then
            assertThat(titles).isNotEmpty();
        }

        @Test
        @DisplayName("should limit to 5 titles")
        void shouldLimitToFiveTitles() {
            // Given - titles need to be at least 20 chars to pass validation
            String json = """
                {"titles": [
                    "标题一需要达到至少二十个字符才能通过验证",
                    "标题二需要达到至少二十个字符才能通过验证",
                    "标题三需要达到至少二十个字符才能通过验证",
                    "标题四需要达到至少二十个字符才能通过验证",
                    "标题五需要达到至少二十个字符才能通过验证",
                    "标题六需要达到至少二十个字符才能通过验证",
                    "标题七需要达到至少二十个字符才能通过验证"
                ]}
                """;

            // When
            List<String> titles = service.parseTitles(json);

            // Then
            assertThat(titles).hasSize(5);
        }

        @Test
        @DisplayName("should filter out titles shorter than 20 characters")
        void shouldFilterOutShortTitles() {
            // Given
            String json = """
                {"titles": [
                    "太短",
                    "这个标题足够长可以通过二十字符的验证规则"
                ]}
                """;

            // When
            List<String> titles = service.parseTitles(json);

            // Then
            assertThat(titles).hasSize(1);
            assertThat(titles.get(0)).contains("这个标题足够长");
        }
    }

    @Nested
    @DisplayName("sanitizeTitle")
    class SanitizeTitle {

        @Test
        @DisplayName("should return null for titles shorter than 20 characters")
        void shouldReturnNullForShortTitles() {
            // When
            String result = service.sanitizeTitle("太短了");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should keep titles within valid length range")
        void shouldKeepTitlesWithinValidRange() {
            // Given
            String title = "这是一个刚好在有效范围内的标题测试";

            // When
            String result = service.sanitizeTitle(title);

            // Then
            assertThat(result).isEqualTo(title);
            assertThat(result.length()).isGreaterThanOrEqualTo(20);
            assertThat(result.length()).isLessThanOrEqualTo(50);
        }

        @Test
        @DisplayName("should truncate titles longer than 50 characters")
        void shouldTruncateLongTitles() {
            // Given
            String longTitle = "这是一个非常非常非常非常非常非常非常非常非常非常非常非常非常非常长的标题需要被截断处理";

            // When
            String result = service.sanitizeTitle(longTitle);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.length()).isLessThanOrEqualTo(50);
            assertThat(result).endsWith("...");
        }

        @Test
        @DisplayName("should return null for empty input")
        void shouldReturnNullForEmptyInput() {
            // Then
            assertThat(service.sanitizeTitle(null)).isNull();
            assertThat(service.sanitizeTitle("")).isNull();
            assertThat(service.sanitizeTitle("   ")).isNull();
        }

        @Test
        @DisplayName("should trim whitespace")
        void shouldTrimWhitespace() {
            // Given
            String title = "   这是一个带有空格的标题需要修剪处理   ";

            // When
            String result = service.sanitizeTitle(title);

            // Then
            assertThat(result).doesNotStartWith(" ");
            assertThat(result).doesNotEndWith(" ");
        }
    }

    @Nested
    @DisplayName("getDefaultTitles")
    class GetDefaultTitles {

        @Test
        @DisplayName("should include shop name in default titles")
        void shouldIncludeShopNameInDefaultTitles() {
            // Given
            String shopName = "海底捞";

            // When
            List<String> defaults = service.getDefaultTitles(shopName);

            // Then
            assertThat(defaults).isNotEmpty();
            assertThat(defaults).allMatch(t -> t.contains(shopName));
        }

        @Test
        @DisplayName("should use fallback when shop name is null")
        void shouldUseFallbackWhenShopNameIsNull() {
            // When
            List<String> defaults = service.getDefaultTitles(null);

            // Then
            assertThat(defaults).isNotEmpty();
            assertThat(defaults).allMatch(t -> t.contains("这家店"));
        }
    }
}
