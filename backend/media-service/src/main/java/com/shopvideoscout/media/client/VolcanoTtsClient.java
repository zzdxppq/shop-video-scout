package com.shopvideoscout.media.client;

import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.media.config.VolcanoTtsProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client for Volcano Engine Seed-TTS API.
 * Handles single paragraph TTS conversion with retry and text segmentation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VolcanoTtsClient {

    private final VolcanoTtsProperties ttsProperties;
    private final RestTemplate restTemplate;

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    /**
     * Synthesize text to speech for a single text segment.
     * If text exceeds max length, splits into segments and synthesizes each.
     *
     * @param text      the text to synthesize
     * @param voiceType the voice type identifier
     * @return list of TTS results (one per segment)
     */
    public List<TtsResult> synthesize(String text, String voiceType) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "TTS文本不能为空");
        }

        List<String> segments = segmentText(text, ttsProperties.getMaxTextLength());
        List<TtsResult> results = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            TtsResult result = synthesizeWithRetry(segments.get(i), voiceType, i);
            results.add(result);
        }

        return results;
    }

    /**
     * Synthesize a single text segment with retry on 504 timeout.
     */
    TtsResult synthesizeWithRetry(String text, String voiceType, int segmentIndex) {
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (true) {
            attempt++;
            try {
                return callTtsApi(text, voiceType);
            } catch (BusinessException e) {
                if (e.getCode() == ResultCode.TTS_SERVICE_TIMEOUT.getCode() && attempt < MAX_RETRIES) {
                    log.warn("TTS timeout for segment {}, attempt {}/{}, retrying in {}ms",
                            segmentIndex, attempt, MAX_RETRIES, backoffMs);
                    sleep(backoffMs);
                    backoffMs *= 2;
                } else {
                    throw e;
                }
            } catch (ResourceAccessException e) {
                if (attempt < MAX_RETRIES) {
                    log.warn("TTS connection error for segment {}, attempt {}/{}, retrying in {}ms",
                            segmentIndex, attempt, MAX_RETRIES, backoffMs);
                    sleep(backoffMs);
                    backoffMs *= 2;
                } else {
                    throw new BusinessException(ResultCode.TTS_SERVICE_TIMEOUT,
                            "配音生成超时，已重试" + MAX_RETRIES + "次");
                }
            }
        }
    }

    /**
     * Call the Volcano TTS API.
     */
    @SuppressWarnings("unchecked")
    TtsResult callTtsApi(String text, String voiceType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer;" + ttsProperties.getAccessToken());

        Map<String, Object> requestBody = Map.of(
                "app", Map.of("appid", ttsProperties.getAppId()),
                "user", Map.of("uid", "shop-video-scout"),
                "audio", Map.of(
                        "voice_type", voiceType,
                        "encoding", ttsProperties.getFormat(),
                        "sample_rate", ttsProperties.getSampleRate()
                ),
                "request", Map.of(
                        "text", text,
                        "operation", "query"
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(
                    ttsProperties.getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
        } catch (ResourceAccessException e) {
            throw e;
        } catch (Exception e) {
            log.error("TTS API call failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.TTS_SERVICE_ERROR, "TTS服务调用失败: " + e.getMessage());
        }

        if (response.getStatusCode().value() == 504) {
            throw new BusinessException(ResultCode.TTS_SERVICE_TIMEOUT, "配音生成超时，正在重试");
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException(ResultCode.TTS_SERVICE_ERROR,
                    "TTS服务返回异常: " + response.getStatusCode());
        }

        Map<String, Object> body = response.getBody();
        String audioData = (String) body.get("data");
        Map<String, Object> addition = (Map<String, Object>) body.get("addition");

        double duration = 0.0;
        if (addition != null && addition.get("duration") != null) {
            duration = Double.parseDouble(addition.get("duration").toString());
        }

        return TtsResult.builder()
                .audioData(java.util.Base64.getDecoder().decode(audioData))
                .durationSeconds(duration)
                .build();
    }

    /**
     * Segment text into chunks not exceeding maxLength.
     * Splits at sentence boundaries (。！？) when possible.
     */
    List<String> segmentText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return List.of(text);
        }

        List<String> segments = new ArrayList<>();
        String remaining = text;

        while (!remaining.isEmpty()) {
            if (remaining.length() <= maxLength) {
                segments.add(remaining);
                break;
            }

            int splitPos = findSplitPosition(remaining, maxLength);
            segments.add(remaining.substring(0, splitPos));
            remaining = remaining.substring(splitPos);
        }

        return segments;
    }

    /**
     * Find the best position to split text, preferring sentence boundaries.
     */
    private int findSplitPosition(String text, int maxLength) {
        String chunk = text.substring(0, maxLength);

        // Try to split at sentence boundary (。！？)
        int lastSentenceEnd = -1;
        for (int i = chunk.length() - 1; i >= chunk.length() / 2; i--) {
            char c = chunk.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '.' || c == '!' || c == '?') {
                lastSentenceEnd = i + 1;
                break;
            }
        }

        if (lastSentenceEnd > 0) {
            return lastSentenceEnd;
        }

        // Try comma
        for (int i = chunk.length() - 1; i >= chunk.length() / 2; i--) {
            char c = chunk.charAt(i);
            if (c == '，' || c == ',' || c == '；' || c == ';') {
                return i + 1;
            }
        }

        // Hard split at maxLength
        return maxLength;
    }

    /**
     * Clone a voice using Volcano Seed-ICL API.
     * Sends the audio sample and receives a clone_voice_id.
     *
     * @param ossKey the OSS key of the audio sample
     * @return clone_voice_id from Seed-ICL
     */
    @SuppressWarnings("unchecked")
    public String cloneVoice(String ossKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer;" + ttsProperties.getAccessToken());

        Map<String, Object> requestBody = Map.of(
                "app", Map.of("appid", ttsProperties.getAppId()),
                "user", Map.of("uid", "shop-video-scout"),
                "audio", Map.of(
                        "audio_url", ossKey,
                        "format", "mp3",
                        "sample_rate", ttsProperties.getSampleRate()
                ),
                "request", Map.of("operation", "clone")
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    ttsProperties.getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(ResultCode.VOICE_CLONE_FAILED,
                        "声音克隆API返回异常: " + response.getStatusCode());
            }

            Map<String, Object> body = response.getBody();
            String cloneVoiceId = (String) body.get("voice_id");
            if (cloneVoiceId == null || cloneVoiceId.isBlank()) {
                throw new BusinessException(ResultCode.VOICE_CLONE_FAILED, "声音克隆未返回音色ID");
            }
            return cloneVoiceId;
        } catch (BusinessException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new BusinessException(ResultCode.VOICE_CLONE_FAILED, "声音克隆API超时: " + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.VOICE_CLONE_FAILED, "声音克隆API调用失败: " + e.getMessage());
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.TTS_SERVICE_ERROR, "TTS合成被中断");
        }
    }

    /**
     * Result from a single TTS API call.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TtsResult {
        private byte[] audioData;
        private double durationSeconds;
    }
}
