package com.shopvideoscout.ai.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.ai.config.QwenVlConfig;
import com.shopvideoscout.ai.constant.QwenVlConstants;
import com.shopvideoscout.ai.dto.FrameAnalysisResult;
import com.shopvideoscout.ai.dto.QwenVlRequest;
import com.shopvideoscout.ai.dto.QwenVlResponse;
import com.shopvideoscout.ai.service.FrameAnalysisParser;
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
 * Client for Qwen-VL (DashScope) API.
 * Handles API calls with retry logic for timeouts (504) and skip logic for unprocessable content (422).
 */
@Slf4j
@Component
public class QwenVlClient {

    private final WebClient webClient;
    private final QwenVlConfig config;
    private final ObjectMapper objectMapper;
    private final FrameAnalysisParser parser;

    public QwenVlClient(QwenVlConfig config, ObjectMapper objectMapper, FrameAnalysisParser parser) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.parser = parser;
        this.webClient = WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .build();
    }

    /**
     * Analyze a single frame using Qwen-VL API.
     *
     * @param frameId   Frame ID for tracking
     * @param frameUrl  URL of the frame image to analyze
     * @return Analysis result (may be failed if API error occurs)
     */
    public FrameAnalysisResult analyzeFrame(Long frameId, String frameUrl) {
        log.debug("Analyzing frame {}: {}", frameId, frameUrl);

        String prompt = buildAnalysisPrompt();
        QwenVlRequest request = QwenVlRequest.createFrameAnalysisRequest(frameUrl, prompt);

        try {
            QwenVlResponse response = callApiWithRetry(request);
            return parseResponse(frameId, frameUrl, response);
        } catch (WebClientResponseException.UnprocessableEntity e) {
            // 422 - Skip this frame, continue with others (Error Handling spec)
            log.warn("Frame {} unprocessable (422), skipping: {}", frameId, e.getMessage());
            return FrameAnalysisResult.failed(frameId, frameUrl, "无法识别图片内容");
        } catch (WebClientResponseException.GatewayTimeout e) {
            // 504 - Already retried 3 times, fail gracefully
            log.error("Frame {} timeout after retries (504): {}", frameId, e.getMessage());
            return FrameAnalysisResult.failed(frameId, frameUrl, "AI分析超时");
        } catch (Exception e) {
            log.error("Frame {} analysis failed: {}", frameId, e.getMessage(), e);
            return FrameAnalysisResult.failed(frameId, frameUrl, "AI服务错误: " + e.getMessage());
        }
    }

    /**
     * Call Qwen-VL API with retry logic for 504 Gateway Timeout.
     * Retries up to 3 times on timeout (configurable).
     */
    private QwenVlResponse callApiWithRetry(QwenVlRequest request) {
        return webClient.post()
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    if (response.statusCode() == HttpStatus.GATEWAY_TIMEOUT) {
                        return Mono.error(new WebClientResponseException.GatewayTimeout("Gateway timeout"));
                    }
                    return response.createException();
                })
                .bodyToMono(QwenVlResponse.class)
                .retryWhen(Retry.backoff(config.getMaxRetryAttempts(), Duration.ofMillis(config.getRetryDelayMs()))
                        .filter(throwable -> throwable instanceof WebClientResponseException.GatewayTimeout)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new WebClientResponseException.GatewayTimeout("Gateway timeout after " +
                                        config.getMaxRetryAttempts() + " retries")))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();
    }

    /**
     * Parse API response into FrameAnalysisResult.
     */
    private FrameAnalysisResult parseResponse(Long frameId, String frameUrl, QwenVlResponse response) {
        if (response == null) {
            return FrameAnalysisResult.failed(frameId, frameUrl, "Empty response from AI service");
        }

        String textContent = response.getTextContent();
        if (textContent == null || textContent.isBlank()) {
            return FrameAnalysisResult.failed(frameId, frameUrl, "No content in AI response");
        }

        try {
            FrameAnalysisResult result = parser.parseAnalysisResponse(textContent);
            result.setFrameId(frameId);
            result.setFrameUrl(frameUrl);
            result.setSuccess(true);
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse AI response for frame {}: {}", frameId, e.getMessage());
            return FrameAnalysisResult.failed(frameId, frameUrl, "Failed to parse AI response");
        }
    }

    /**
     * Build the analysis prompt for Qwen-VL.
     * Designed to return structured JSON response.
     */
    private String buildAnalysisPrompt() {
        return """
            分析这张图片并返回JSON格式的结果。

            分析要求:
            1. category: 图片分类，必须是以下之一: food(食物), person(人物), environment(环境), other(其他)
            2. tags: 描述标签数组，最多5个，用于描述图片内容特征
            3. quality_score: 质量评分(0-100)，综合考虑清晰度、构图、光线、稳定性
            4. description: 一句话描述图片内容

            请只返回JSON，格式如下:
            {
              "category": "food",
              "tags": ["美食", "色彩丰富", "摆盘精美"],
              "quality_score": 85,
              "description": "一道精美的中式美食"
            }
            """;
    }
}
