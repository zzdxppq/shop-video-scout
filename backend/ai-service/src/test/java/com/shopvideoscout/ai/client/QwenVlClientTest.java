package com.shopvideoscout.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.ai.config.QwenVlConfig;
import com.shopvideoscout.ai.dto.FrameAnalysisResult;
import com.shopvideoscout.ai.service.FrameAnalysisParser;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for QwenVlClient.
 * Uses MockWebServer to simulate Qwen-VL API responses.
 * Covers test scenarios from QA Test Design:
 * - 2.3-INT-001: Qwen-VL API call with valid frame returns expected structure
 * - 2.3-INT-002: Retry logic: 3 retries on 504 timeout
 * - 2.3-INT-003: Skip frame on 422, continue processing others
 * - 2.3-UNIT-006: Handle malformed response
 */
class QwenVlClientTest {

    private MockWebServer mockServer;
    private QwenVlClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();

        objectMapper = new ObjectMapper();

        QwenVlConfig config = new QwenVlConfig();
        config.setBaseUrl(mockServer.url("/").toString());
        config.setApiKey("test-api-key");
        config.setTimeoutSeconds(5);
        config.setMaxRetryAttempts(3);
        config.setRetryDelayMs(100); // Short delay for tests

        FrameAnalysisParser parser = new FrameAnalysisParser(objectMapper);
        client = new QwenVlClient(config, objectMapper, parser);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Nested
    @DisplayName("2.3-INT-001: Valid API call returns expected structure")
    class ValidApiCallTests {

        @Test
        @DisplayName("Should return valid analysis result for successful API call")
        void shouldReturnValidAnalysisForSuccessfulCall() {
            String apiResponse = """
                {
                  "request_id": "test-123",
                  "output": {
                    "choices": [{
                      "finish_reason": "stop",
                      "message": {
                        "role": "assistant",
                        "content": [{
                          "text": "{\\"category\\": \\"food\\", \\"tags\\": [\\"美食\\", \\"精美\\"], \\"quality_score\\": 85, \\"description\\": \\"美食图片\\"}"
                        }]
                      }
                    }]
                  },
                  "usage": {"input_tokens": 100, "output_tokens": 50}
                }
                """;

            mockServer.enqueue(new MockResponse()
                    .setBody(apiResponse)
                    .setHeader("Content-Type", "application/json"));

            FrameAnalysisResult result = client.analyzeFrame(1L, "https://example.com/frame.jpg");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getFrameId()).isEqualTo(1L);
            assertThat(result.getFrameUrl()).isEqualTo("https://example.com/frame.jpg");
            assertThat(result.getCategory()).isEqualTo("food");
            assertThat(result.getTags()).containsExactly("美食", "精美");
            assertThat(result.getQualityScore()).isEqualTo(85);
        }
    }

    @Nested
    @DisplayName("2.3-INT-002: Retry logic on 504 timeout")
    class RetryLogicTests {

        @Test
        @DisplayName("Should retry 3 times on 504 and succeed on third attempt")
        void shouldRetryAndSucceedOnThirdAttempt() {
            // First two attempts fail with 504
            mockServer.enqueue(new MockResponse().setResponseCode(504));
            mockServer.enqueue(new MockResponse().setResponseCode(504));

            // Third attempt succeeds
            String successResponse = """
                {
                  "output": {
                    "choices": [{
                      "message": {
                        "content": [{
                          "text": "{\\"category\\": \\"food\\", \\"tags\\": [], \\"quality_score\\": 80}"
                        }]
                      }
                    }]
                  }
                }
                """;
            mockServer.enqueue(new MockResponse()
                    .setBody(successResponse)
                    .setHeader("Content-Type", "application/json"));

            FrameAnalysisResult result = client.analyzeFrame(1L, "https://example.com/frame.jpg");

            assertThat(result.isSuccess()).isTrue();
            assertThat(mockServer.getRequestCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should fail gracefully after 3 retries on persistent 504")
        void shouldFailAfterMaxRetries() {
            // All attempts fail with 504
            mockServer.enqueue(new MockResponse().setResponseCode(504));
            mockServer.enqueue(new MockResponse().setResponseCode(504));
            mockServer.enqueue(new MockResponse().setResponseCode(504));
            mockServer.enqueue(new MockResponse().setResponseCode(504));

            FrameAnalysisResult result = client.analyzeFrame(1L, "https://example.com/frame.jpg");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("AI分析超时");
        }
    }

    @Nested
    @DisplayName("2.3-INT-003: Skip frame on 422")
    class Skip422Tests {

        @Test
        @DisplayName("Should skip frame and return failed result on 422")
        void shouldSkipFrameOn422() {
            mockServer.enqueue(new MockResponse().setResponseCode(422).setBody("Unprocessable Entity"));

            FrameAnalysisResult result = client.analyzeFrame(1L, "https://example.com/invalid-frame.jpg");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFrameId()).isEqualTo(1L);
            assertThat(result.getErrorMessage()).contains("无法识别图片内容");
        }

        @Test
        @DisplayName("Should not retry on 422")
        void shouldNotRetryOn422() {
            mockServer.enqueue(new MockResponse().setResponseCode(422));

            client.analyzeFrame(1L, "https://example.com/invalid-frame.jpg");

            // Should only make one request, no retries
            assertThat(mockServer.getRequestCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("2.3-UNIT-006: Malformed response handling")
    class MalformedResponseTests {

        @Test
        @DisplayName("Should handle empty response body")
        void shouldHandleEmptyResponse() {
            mockServer.enqueue(new MockResponse()
                    .setBody("")
                    .setHeader("Content-Type", "application/json"));

            FrameAnalysisResult result = client.analyzeFrame(1L, "https://example.com/frame.jpg");

            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Should handle response with no content")
        void shouldHandleNoContent() {
            String response = """
                {
                  "output": {
                    "choices": []
                  }
                }
                """;
            mockServer.enqueue(new MockResponse()
                    .setBody(response)
                    .setHeader("Content-Type", "application/json"));

            FrameAnalysisResult result = client.analyzeFrame(1L, "https://example.com/frame.jpg");

            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Should handle 500 internal server error")
        void shouldHandle500Error() {
            mockServer.enqueue(new MockResponse().setResponseCode(500));

            FrameAnalysisResult result = client.analyzeFrame(1L, "https://example.com/frame.jpg");

            assertThat(result.isSuccess()).isFalse();
        }
    }
}
