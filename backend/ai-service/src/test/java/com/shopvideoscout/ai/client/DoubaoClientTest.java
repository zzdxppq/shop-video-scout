package com.shopvideoscout.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.ai.config.DoubaoConfig;
import com.shopvideoscout.common.exception.BusinessException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for DoubaoClient.
 * Story 3.1 - AC1: Doubao API integration with retry logic.
 */
class DoubaoClientTest {

    private MockWebServer mockWebServer;
    private DoubaoClient doubaoClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        DoubaoConfig config = new DoubaoConfig();
        config.setBaseUrl(mockWebServer.url("/").toString());
        config.setApiKey("test-api-key");
        config.setEndpointId("ep-test");
        config.setTimeoutSeconds(5);
        config.setMaxRetryAttempts(2);
        config.setRetryDelayMs(100); // Short delay for testing
        config.setDefaultTemperature(0.7);
        config.setMaxTemperature(0.9);
        config.setTemperatureIncrement(0.1);

        doubaoClient = new DoubaoClient(config);
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Should generate script successfully")
    void generateScript_success_shouldReturnContent() {
        String responseJson = """
            {
              "id": "chatcmpl-123",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "{\\"paragraphs\\": [], \\"total_duration\\": 60}"
                  },
                  "finish_reason": "stop"
                }
              ],
              "usage": {
                "prompt_tokens": 100,
                "completion_tokens": 50,
                "total_tokens": 150
              }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json"));

        String result = doubaoClient.generateScript("test prompt", 0.7);

        assertThat(result).isNotNull();
        assertThat(result).contains("paragraphs");
    }

    @Test
    @DisplayName("Should throw exception when API returns empty content")
    void generateScript_emptyContent_shouldThrowException() {
        String responseJson = """
            {
              "id": "chatcmpl-123",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": ""
                  }
                }
              ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> doubaoClient.generateScript("test prompt", 0.7))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI服务返回空内容");
    }

    // 3.1-UNIT-010: Temperature increases on regeneration
    @Test
    @DisplayName("Should calculate temperature correctly for regeneration")
    void calculateTemperature_shouldIncreaseWithCount() {
        assertThat(doubaoClient.calculateTemperature(0)).isEqualTo(0.7);
        assertThat(doubaoClient.calculateTemperature(1)).isEqualTo(0.8);
        assertThat(doubaoClient.calculateTemperature(2)).isEqualTo(0.9);
        // Should cap at max temperature
        assertThat(doubaoClient.calculateTemperature(3)).isEqualTo(0.9);
        assertThat(doubaoClient.calculateTemperature(10)).isEqualTo(0.9);
    }

    // 3.1-INT-004: Doubao API returns error → task.status='failed'
    @Test
    @DisplayName("Should throw exception on API 500 error")
    void generateScript_serverError_shouldThrowException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\": \"Internal server error\"}"));

        assertThatThrownBy(() -> doubaoClient.generateScript("test prompt", 0.7))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI服务");
    }

    @Test
    @DisplayName("Should throw exception on API 401 error")
    void generateScript_unauthorized_shouldThrowException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\": \"Unauthorized\"}"));

        assertThatThrownBy(() -> doubaoClient.generateScript("test prompt", 0.7))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI服务");
    }

    @Test
    @DisplayName("Should handle null choices in response")
    void generateScript_nullChoices_shouldThrowException() {
        String responseJson = """
            {
              "id": "chatcmpl-123",
              "choices": null
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> doubaoClient.generateScript("test prompt", 0.7))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Should handle empty choices in response")
    void generateScript_emptyChoices_shouldThrowException() {
        String responseJson = """
            {
              "id": "chatcmpl-123",
              "choices": []
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> doubaoClient.generateScript("test prompt", 0.7))
                .isInstanceOf(BusinessException.class);
    }
}
