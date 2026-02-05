package com.shopvideoscout.publish.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.common.doubao.DoubaoClient;
import com.shopvideoscout.publish.prompt.PublishAssistPromptBuilder;
import com.shopvideoscout.publish.service.impl.TopicGenerationServiceImpl;
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
 * Unit tests for TopicGenerationService.
 * Story 5.3: 发布辅助服务 - AC1
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TopicGenerationService Tests")
class TopicGenerationServiceTest {

    @Mock
    private DoubaoClient doubaoClient;

    @Mock
    private PublishAssistPromptBuilder promptBuilder;

    private TopicGenerationServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new TopicGenerationServiceImpl(doubaoClient, promptBuilder, objectMapper);
    }

    @Nested
    @DisplayName("generateTopics")
    class GenerateTopics {

        @Test
        @DisplayName("should return topics from AI response")
        void shouldReturnTopicsFromAiResponse() {
            // Given
            String shopName = "海底捞";
            String shopType = "火锅";
            String scriptSummary = "测试脚本摘要";
            String aiResponse = """
                {
                    "topics": ["#海底捞", "#火锅推荐", "#美食探店"]
                }
                """;

            when(promptBuilder.buildPrompt(shopName, shopType, scriptSummary))
                    .thenReturn("test prompt");
            when(doubaoClient.chat(eq("test prompt"), any()))
                    .thenReturn(aiResponse);

            // When
            List<String> topics = service.generateTopics(shopName, shopType, scriptSummary, null);

            // Then
            assertThat(topics).hasSize(3);
            assertThat(topics).contains("#海底捞", "#火锅推荐", "#美食探店");
        }

        @Test
        @DisplayName("should return default topics when AI fails")
        void shouldReturnDefaultTopicsWhenAiFails() {
            // Given
            when(promptBuilder.buildPrompt(any(), any(), any()))
                    .thenReturn("test prompt");
            when(doubaoClient.chat(any(), any()))
                    .thenThrow(new RuntimeException("API error"));

            // When
            List<String> topics = service.generateTopics("店名", "类型", "摘要", null);

            // Then
            assertThat(topics).isNotEmpty();
            assertThat(topics).contains("#探店", "#美食探店");
        }

        @Test
        @DisplayName("should handle markdown code block wrapped JSON")
        void shouldHandleMarkdownCodeBlockWrappedJson() {
            // Given
            String aiResponse = """
                ```json
                {
                    "topics": ["#测试话题"]
                }
                ```
                """;

            when(promptBuilder.buildPrompt(any(), any(), any())).thenReturn("prompt");
            when(doubaoClient.chat(any(), any())).thenReturn(aiResponse);

            // When
            List<String> topics = service.generateTopics("店名", "类型", "摘要", null);

            // Then
            assertThat(topics).contains("#测试话题");
        }
    }

    @Nested
    @DisplayName("parseTopics")
    class ParseTopics {

        @Test
        @DisplayName("should parse valid JSON with topics array")
        void shouldParseValidJsonWithTopicsArray() {
            // Given
            String json = """
                {"topics": ["#话题一", "#话题二", "#话题三"]}
                """;

            // When
            List<String> topics = service.parseTopics(json);

            // Then
            assertThat(topics).hasSize(3);
            assertThat(topics).containsExactly("#话题一", "#话题二", "#话题三");
        }

        @Test
        @DisplayName("should return defaults for invalid JSON")
        void shouldReturnDefaultsForInvalidJson() {
            // When
            List<String> topics = service.parseTopics("not valid json");

            // Then
            assertThat(topics).isNotEmpty();
            assertThat(topics.get(0)).startsWith("#");
        }

        @Test
        @DisplayName("should return defaults when topics is not array")
        void shouldReturnDefaultsWhenTopicsIsNotArray() {
            // Given
            String json = """
                {"topics": "not an array"}
                """;

            // When
            List<String> topics = service.parseTopics(json);

            // Then
            assertThat(topics).isNotEmpty();
        }

        @Test
        @DisplayName("should limit to 10 topics")
        void shouldLimitToTenTopics() {
            // Given
            String json = """
                {"topics": ["#1", "#2", "#3", "#4", "#5", "#6", "#7", "#8", "#9", "#10", "#11", "#12"]}
                """;

            // When
            List<String> topics = service.parseTopics(json);

            // Then
            assertThat(topics).hasSize(10);
        }
    }

    @Nested
    @DisplayName("sanitizeTopic")
    class SanitizeTopic {

        @Test
        @DisplayName("should add # prefix if missing")
        void shouldAddHashPrefixIfMissing() {
            // When
            String result = service.sanitizeTopic("美食探店");

            // Then
            assertThat(result).isEqualTo("#美食探店");
        }

        @Test
        @DisplayName("should keep existing # prefix")
        void shouldKeepExistingHashPrefix() {
            // When
            String result = service.sanitizeTopic("#美食探店");

            // Then
            assertThat(result).isEqualTo("#美食探店");
        }

        @Test
        @DisplayName("should remove spaces")
        void shouldRemoveSpaces() {
            // When
            String result = service.sanitizeTopic("美食 探店");

            // Then
            assertThat(result).isEqualTo("#美食探店");
        }

        @Test
        @DisplayName("should truncate to max 20 characters")
        void shouldTruncateToMaxLength() {
            // When
            String result = service.sanitizeTopic("这是一个非常非常长的话题标签需要被截断");

            // Then
            assertThat(result).hasSize(20);
        }

        @Test
        @DisplayName("should return null for empty input")
        void shouldReturnNullForEmptyInput() {
            // Then
            assertThat(service.sanitizeTopic(null)).isNull();
            assertThat(service.sanitizeTopic("")).isNull();
            assertThat(service.sanitizeTopic("   ")).isNull();
        }
    }

    @Nested
    @DisplayName("getDefaultTopics")
    class GetDefaultTopics {

        @Test
        @DisplayName("should return non-empty default list")
        void shouldReturnNonEmptyDefaultList() {
            // When
            List<String> defaults = service.getDefaultTopics();

            // Then
            assertThat(defaults).isNotEmpty();
            assertThat(defaults).allMatch(t -> t.startsWith("#"));
        }

        @Test
        @DisplayName("should return a new list each time")
        void shouldReturnNewListEachTime() {
            // When
            List<String> list1 = service.getDefaultTopics();
            List<String> list2 = service.getDefaultTopics();

            // Then
            assertThat(list1).isNotSameAs(list2);
        }
    }
}
