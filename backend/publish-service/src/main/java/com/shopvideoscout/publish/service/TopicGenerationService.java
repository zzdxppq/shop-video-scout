package com.shopvideoscout.publish.service;

import java.util.List;

/**
 * Service for generating topic recommendations.
 * Story 5.3: 发布辅助服务 - AC1
 */
public interface TopicGenerationService {

    /**
     * Generate topic recommendations for a task.
     *
     * @param shopName      shop name
     * @param shopType      shop type
     * @param scriptSummary script summary
     * @param temperature   AI temperature (null for default)
     * @return list of topics (5-10 items, starting with #)
     */
    List<String> generateTopics(String shopName, String shopType, String scriptSummary, Double temperature);

    /**
     * Get default fallback topics when AI generation fails.
     *
     * @return default topics list
     */
    List<String> getDefaultTopics();
}
