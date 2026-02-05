package com.shopvideoscout.common.doubao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * HTTP client for Doubao (Volcengine) API.
 * Story 5.3: 发布辅助服务
 *
 * Features:
 * - Retry with exponential backoff (3 attempts)
 * - Configurable timeout
 * - Temperature parameter support
 * - JSON response parsing
 */
@Slf4j
@RequiredArgsConstructor
public class DoubaoClient {

    private final DoubaoProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Send a chat completion request to Doubao API.
     *
     * @param prompt      the user prompt
     * @param temperature temperature for generation (null uses default)
     * @return the generated content string
     * @throws DoubaoException if API call fails after retries
     */
    @Retryable(
            value = {HttpServerErrorException.class, ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public String chat(String prompt, Double temperature) {
        double temp = temperature != null ? temperature : properties.getDefaultTemperature();

        DoubaoRequest request = DoubaoRequest.builder()
                .model(properties.getModel())
                .messages(List.of(
                        DoubaoRequest.Message.builder()
                                .role("user")
                                .content(prompt)
                                .build()
                ))
                .temperature(temp)
                .maxTokens(2048)
                .build();

        return executeRequest(request);
    }

    /**
     * Send a chat completion request with system message.
     *
     * @param systemPrompt system message for context
     * @param userPrompt   user message
     * @param temperature  temperature for generation
     * @return the generated content string
     */
    @Retryable(
            value = {HttpServerErrorException.class, ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public String chatWithSystem(String systemPrompt, String userPrompt, Double temperature) {
        double temp = temperature != null ? temperature : properties.getDefaultTemperature();

        DoubaoRequest request = DoubaoRequest.builder()
                .model(properties.getModel())
                .messages(List.of(
                        DoubaoRequest.Message.builder()
                                .role("system")
                                .content(systemPrompt)
                                .build(),
                        DoubaoRequest.Message.builder()
                                .role("user")
                                .content(userPrompt)
                                .build()
                ))
                .temperature(temp)
                .maxTokens(2048)
                .build();

        return executeRequest(request);
    }

    /**
     * Execute HTTP request to Doubao API.
     */
    private String executeRequest(DoubaoRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Doubao request", e);
            throw new DoubaoException("Request serialization failed", e);
        }

        log.debug("Doubao API request: model={}, temperature={}",
                request.getModel(), request.getTemperature());

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getEndpoint(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getBody() == null) {
                log.warn("Doubao API returned empty response");
                throw new DoubaoException("Empty response from Doubao API");
            }

            DoubaoResponse doubaoResponse = objectMapper.readValue(
                    response.getBody(), DoubaoResponse.class);

            String content = doubaoResponse.getContent();
            if (content == null) {
                log.warn("Doubao API response has no content: {}", response.getBody());
                throw new DoubaoException("No content in Doubao API response");
            }

            log.debug("Doubao API response: tokens={}",
                    doubaoResponse.getUsage() != null ?
                            doubaoResponse.getUsage().getTotalTokens() : "unknown");

            return content;

        } catch (HttpClientErrorException e) {
            log.error("Doubao API client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new DoubaoException("Doubao API client error: " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            log.error("Doubao API server error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e; // Let retry handle this
        } catch (ResourceAccessException e) {
            log.error("Doubao API connection error", e);
            throw e; // Let retry handle this
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Doubao response", e);
            throw new DoubaoException("Response parsing failed", e);
        }
    }

    /**
     * Exception for Doubao API errors.
     */
    public static class DoubaoException extends RuntimeException {
        public DoubaoException(String message) {
            super(message);
        }

        public DoubaoException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
