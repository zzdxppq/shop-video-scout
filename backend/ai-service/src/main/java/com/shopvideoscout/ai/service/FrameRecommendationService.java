package com.shopvideoscout.ai.service;

import com.shopvideoscout.ai.dto.FrameAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for recommending best frames based on AI analysis results.
 * Business Rules:
 * - BR-2.1: Each category recommends maximum 2 frames
 * - BR-2.2: Recommended frames are marked with is_recommended=true
 */
@Slf4j
@Service
public class FrameRecommendationService {

    /**
     * Maximum recommended frames per category (BR-2.1).
     */
    private static final int MAX_RECOMMENDATIONS_PER_CATEGORY = 2;

    /**
     * Process analyzed frames and mark top 2 per category as recommended.
     *
     * Algorithm:
     * 1. Group frames by category
     * 2. Sort each group by quality score descending
     * 3. Mark top 2 in each category as recommended
     *
     * @param analysisResults List of frame analysis results
     * @return List of frame IDs that are recommended
     */
    public List<Long> markRecommendedFrames(List<FrameAnalysisResult> analysisResults) {
        if (analysisResults == null || analysisResults.isEmpty()) {
            log.debug("No frames to process for recommendations");
            return Collections.emptyList();
        }

        // Filter out failed analyses
        List<FrameAnalysisResult> successfulResults = analysisResults.stream()
                .filter(FrameAnalysisResult::isSuccess)
                .filter(r -> r.getFrameId() != null)
                .toList();

        if (successfulResults.isEmpty()) {
            log.debug("No successful frame analyses to recommend");
            return Collections.emptyList();
        }

        // Group by category
        Map<String, List<FrameAnalysisResult>> groupedByCategory = groupByCategory(successfulResults);

        // Select top 2 from each category
        List<Long> recommendedFrameIds = new ArrayList<>();

        for (Map.Entry<String, List<FrameAnalysisResult>> entry : groupedByCategory.entrySet()) {
            String category = entry.getKey();
            List<FrameAnalysisResult> categoryFrames = entry.getValue();

            // Sort by quality score descending, with deterministic tie-breaking by frame ID
            List<FrameAnalysisResult> sortedFrames = sortByQualityScore(categoryFrames);

            // Mark top 2 (or fewer if category has less than 2)
            int recommendCount = Math.min(MAX_RECOMMENDATIONS_PER_CATEGORY, sortedFrames.size());
            for (int i = 0; i < recommendCount; i++) {
                Long frameId = sortedFrames.get(i).getFrameId();
                recommendedFrameIds.add(frameId);
                log.debug("Recommended frame {} from category {} (rank {})", frameId, category, i + 1);
            }
        }

        log.info("Marked {} frames as recommended across {} categories",
                recommendedFrameIds.size(), groupedByCategory.size());

        return recommendedFrameIds;
    }

    /**
     * Group frames by category.
     *
     * @param results Analysis results to group
     * @return Map of category -> list of results
     */
    Map<String, List<FrameAnalysisResult>> groupByCategory(List<FrameAnalysisResult> results) {
        return results.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCategory() != null ? r.getCategory() : "other",
                        LinkedHashMap::new, // Preserve insertion order for deterministic results
                        Collectors.toList()
                ));
    }

    /**
     * Sort frames by quality score descending.
     * Uses frame ID as tie-breaker for deterministic ordering.
     *
     * @param frames List of frames to sort
     * @return Sorted list (new list, original unchanged)
     */
    List<FrameAnalysisResult> sortByQualityScore(List<FrameAnalysisResult> frames) {
        return frames.stream()
                .sorted(Comparator
                        // Primary sort: quality score descending
                        .comparingInt((FrameAnalysisResult r) ->
                                r.getQualityScore() != null ? r.getQualityScore() : 0)
                        .reversed()
                        // Tie-breaker: frame ID ascending (lower ID wins on tie)
                        .thenComparingLong(r -> r.getFrameId() != null ? r.getFrameId() : Long.MAX_VALUE))
                .toList();
    }
}
