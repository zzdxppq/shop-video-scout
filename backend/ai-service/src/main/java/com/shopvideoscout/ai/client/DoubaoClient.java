package com.shopvideoscout.ai.client;

import com.shopvideoscout.ai.config.DoubaoConfig;
import com.shopvideoscout.ai.dto.DoubaoRequest;
import com.shopvideoscout.ai.dto.DoubaoResponse;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Client for Doubao (豆包) API.
 * Handles API calls with exponential backoff retry logic for timeouts.
 */
@Slf4j
@Component
public class DoubaoClient {

    private final WebClient webClient;
    private final DoubaoConfig config;

    public DoubaoClient(DoubaoConfig config) {
        this.config = config;
        this.webClient = WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .build();
    }

    /**
     * Generate script using Doubao API with retry logic.
     *
     * @param prompt      The formatted prompt for script generation
     * @param temperature Temperature parameter for response diversity
     * @return The generated text content
     * @throws BusinessException if API call fails after retries
     */
    public String generateScript(String prompt, double temperature) {
        log.debug("Calling Doubao API with temperature={}", temperature);

        DoubaoRequest request = DoubaoRequest.forScriptGeneration(
                config.getEndpointId(), prompt, temperature);

        try {
            DoubaoResponse response = callApiWithRetry(request);
            String content = response.getTextContent();

            if (content == null || content.isBlank()) {
                log.error("Doubao API returned empty content");
                throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI服务返回空内容");
            }

            log.debug("Doubao API returned content length: {}", content.length());
            return content;

        } catch (WebClientResponseException.GatewayTimeout e) {
            // 504 - Already retried, throw timeout exception
            log.error("Doubao API timeout after {} retries", config.getMaxRetryAttempts());
            throw new BusinessException(ResultCode.AI_SERVICE_TIMEOUT, "生成超时，正在重试");

        } catch (WebClientResponseException e) {
            // Other HTTP errors
            log.error("Doubao API error: {} - {}", e.getStatusCode(), e.getMessage());
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI服务暂时不可用");

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            log.error("Doubao API unexpected error: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI服务错误: " + e.getMessage());
        }
    }

    /**
     * Call Doubao API with exponential backoff retry logic.
     * Retries on 504 Gateway Timeout with 5s, 10s delays (configurable).
     */
    private DoubaoResponse callApiWithRetry(DoubaoRequest request) {
        return webClient.post()
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    if (response.statusCode() == HttpStatus.GATEWAY_TIMEOUT) {
                        return Mono.error(new WebClientResponseException.GatewayTimeout("Gateway timeout"));
                    }
                    return response.createException();
                })
                .bodyToMono(DoubaoResponse.class)
                .retryWhen(Retry.backoff(config.getMaxRetryAttempts(), Duration.ofMillis(config.getRetryDelayMs()))
                        .filter(throwable -> throwable instanceof WebClientResponseException.GatewayTimeout)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new WebClientResponseException.GatewayTimeout(
                                        "Gateway timeout after " + config.getMaxRetryAttempts() + " retries")))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();
    }

    /**
     * Calculate temperature for regeneration based on attempt count.
     * First attempt: 0.7, second: 0.8, third: 0.9, etc.
     */
    public double calculateTemperature(int regenerateCount) {
        double temp = config.getDefaultTemperature() + (regenerateCount * config.getTemperatureIncrement());
        return Math.min(temp, config.getMaxTemperature());
    }
}
