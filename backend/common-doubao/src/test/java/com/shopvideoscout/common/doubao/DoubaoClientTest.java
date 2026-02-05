package com.shopvideoscout.common.doubao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DoubaoClient.
 * Story 5.3: 发布辅助服务
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DoubaoClient Tests")
class DoubaoClientTest {

    @Mock
    private RestTemplate restTemplate;

    private DoubaoClient client;
    private DoubaoProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new DoubaoProperties();
        properties.setApiKey("test-api-key");
        properties.setEndpoint("https://api.test.com/chat");
        properties.setModel("test-model");
        properties.setDefaultTemperature(0.7);

        objectMapper = new ObjectMapper();
        client = new DoubaoClient(properties, restTemplate, objectMapper);
    }

    @Nested
    @DisplayName("chat")
    class Chat {

        @Test
        @DisplayName("should send correct request format")
        void shouldSendCorrectRequestFormat() throws Exception {
            // Given
            String prompt = "测试提示词";
            String responseBody = """
                {
                    "choices": [{
                        "message": {
                            "content": "AI生成的内容"
                        }
                    }],
                    "usage": {
                        "total_tokens": 100
                    }
                }
                """;
            mockSuccessResponse(responseBody);

            // When
            client.chat(prompt, null);

            // Then
            ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(
                    eq(properties.getEndpoint()),
                    eq(HttpMethod.POST),
                    entityCaptor.capture(),
                    eq(String.class)
            );

            HttpEntity<String> entity = entityCaptor.getValue();
            assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
            assertThat(entity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                    .isEqualTo("Bearer test-api-key");

            String body = entity.getBody();
            assertThat(body).contains("\"model\":\"test-model\"");
            assertThat(body).contains("\"role\":\"user\"");
            assertThat(body).contains(prompt);
        }

        @Test
        @DisplayName("should use custom temperature when provided")
        void shouldUseCustomTemperature() throws Exception {
            // Given
            String responseBody = """
                {"choices":[{"message":{"content":"内容"}}]}
                """;
            mockSuccessResponse(responseBody);

            // When
            client.chat("prompt", 0.9);

            // Then
            ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(any(), any(), entityCaptor.capture(), eq(String.class));

            String body = entityCaptor.getValue().getBody();
            assertThat(body).contains("\"temperature\":0.9");
        }

        @Test
        @DisplayName("should use default temperature when not provided")
        void shouldUseDefaultTemperature() throws Exception {
            // Given
            String responseBody = """
                {"choices":[{"message":{"content":"内容"}}]}
                """;
            mockSuccessResponse(responseBody);

            // When
            client.chat("prompt", null);

            // Then
            ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(any(), any(), entityCaptor.capture(), eq(String.class));

            String body = entityCaptor.getValue().getBody();
            assertThat(body).contains("\"temperature\":0.7");
        }

        @Test
        @DisplayName("should return content from API response")
        void shouldReturnContentFromApiResponse() {
            // Given
            String expectedContent = "这是AI生成的内容";
            String responseBody = """
                {"choices":[{"message":{"content":"%s"}}]}
                """.formatted(expectedContent);
            mockSuccessResponse(responseBody);

            // When
            String result = client.chat("prompt", null);

            // Then
            assertThat(result).isEqualTo(expectedContent);
        }

        @Test
        @DisplayName("should throw DoubaoException for empty response")
        void shouldThrowForEmptyResponse() {
            // Given
            mockSuccessResponse(null);

            // When/Then
            assertThatThrownBy(() -> client.chat("prompt", null))
                    .isInstanceOf(DoubaoClient.DoubaoException.class)
                    .hasMessageContaining("Empty response");
        }

        @Test
        @DisplayName("should throw DoubaoException for 4xx client errors")
        void shouldThrowForClientErrors() {
            // Given
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

            // When/Then
            assertThatThrownBy(() -> client.chat("prompt", null))
                    .isInstanceOf(DoubaoClient.DoubaoException.class)
                    .hasMessageContaining("client error");
        }

        @Test
        @DisplayName("should rethrow 5xx server errors for retry")
        void shouldRethrowServerErrors() {
            // Given
            HttpServerErrorException serverError = new HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Server Error");
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                    .thenThrow(serverError);

            // When/Then
            assertThatThrownBy(() -> client.chat("prompt", null))
                    .isInstanceOf(HttpServerErrorException.class);
        }
    }

    @Nested
    @DisplayName("chatWithSystem")
    class ChatWithSystem {

        @Test
        @DisplayName("should include system and user messages")
        void shouldIncludeSystemAndUserMessages() throws Exception {
            // Given
            String responseBody = """
                {"choices":[{"message":{"content":"内容"}}]}
                """;
            mockSuccessResponse(responseBody);

            // When
            client.chatWithSystem("系统提示", "用户提示", null);

            // Then
            ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(any(), any(), entityCaptor.capture(), eq(String.class));

            String body = entityCaptor.getValue().getBody();
            assertThat(body).contains("\"role\":\"system\"");
            assertThat(body).contains("\"role\":\"user\"");
            assertThat(body).contains("系统提示");
            assertThat(body).contains("用户提示");
        }

        @Test
        @DisplayName("should return content from response")
        void shouldReturnContentFromResponse() {
            // Given
            String expectedContent = "带系统消息的响应";
            String responseBody = """
                {"choices":[{"message":{"content":"%s"}}]}
                """.formatted(expectedContent);
            mockSuccessResponse(responseBody);

            // When
            String result = client.chatWithSystem("system", "user", null);

            // Then
            assertThat(result).isEqualTo(expectedContent);
        }
    }

    @Nested
    @DisplayName("Response parsing")
    class ResponseParsing {

        @Test
        @DisplayName("should throw DoubaoException when response has no content")
        void shouldThrowWhenNoContent() {
            // Given
            String responseBody = """
                {"choices":[{"message":{}}]}
                """;
            mockSuccessResponse(responseBody);

            // When/Then
            assertThatThrownBy(() -> client.chat("prompt", null))
                    .isInstanceOf(DoubaoClient.DoubaoException.class)
                    .hasMessageContaining("No content");
        }

        @Test
        @DisplayName("should handle response with usage info")
        void shouldHandleResponseWithUsageInfo() {
            // Given
            String responseBody = """
                {
                    "choices":[{"message":{"content":"内容"}}],
                    "usage":{
                        "prompt_tokens": 50,
                        "completion_tokens": 50,
                        "total_tokens": 100
                    }
                }
                """;
            mockSuccessResponse(responseBody);

            // When
            String result = client.chat("prompt", null);

            // Then
            assertThat(result).isEqualTo("内容");
        }
    }

    private void mockSuccessResponse(String body) {
        ResponseEntity<String> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);
    }
}
