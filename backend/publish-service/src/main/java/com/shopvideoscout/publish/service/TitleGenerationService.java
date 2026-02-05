package com.shopvideoscout.publish.service;

import java.util.List;

/**
 * Service for generating title recommendations.
 * Story 5.3: 发布辅助服务 - AC2
 */
public interface TitleGenerationService {

    /**
     * Generate title recommendations for a task.
     *
     * @param shopName      shop name
     * @param shopType      shop type
     * @param scriptSummary script summary
     * @param temperature   AI temperature (null for default)
     * @return list of titles (3-5 items, 20-50 chars each)
     */
    List<String> generateTitles(String shopName, String shopType, String scriptSummary, Double temperature);

    /**
     * Get default fallback titles when AI generation fails.
     *
     * @param shopName shop name to include in templates
     * @return default titles list
     */
    List<String> getDefaultTitles(String shopName);
}
