package com.shopvideoscout.ai.service;

import com.shopvideoscout.ai.dto.FrameAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for FrameRecommendationService.
 * Covers test scenarios from QA Test Design:
 * - 2.3-UNIT-007: Group frames by category correctly
 * - 2.3-UNIT-008: Sort frames by quality score descending
 * - 2.3-UNIT-009: Mark exactly top 2 per category as recommended
 * - 2.3-UNIT-010: Handle tie-breaking when scores equal
 * - 2.3-UNIT-011: Handle category with <2 frames
 * - 2.3-BLIND-BOUNDARY-001: Empty frames array
 * - 2.3-BLIND-BOUNDARY-002: Single frame input
 */
class FrameRecommendationServiceTest {

    private FrameRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new FrameRecommendationService();
    }

    @Nested
    @DisplayName("2.3-UNIT-007: Group frames by category correctly")
    class GroupByCategoryTests {

        @Test
        @DisplayName("Should group mixed frames into 4 categories")
        void shouldGroupMixedFramesInto4Categories() {
            List<FrameAnalysisResult> frames = Arrays.asList(
                    createResult(1L, "food", 80),
                    createResult(2L, "person", 75),
                    createResult(3L, "environment", 70),
                    createResult(4L, "other", 65),
                    createResult(5L, "food", 85),
                    createResult(6L, "person", 90)
            );

            Map<String, List<FrameAnalysisResult>> grouped = service.groupByCategory(frames);

            assertThat(grouped).hasSize(4);
            assertThat(grouped.get("food")).hasSize(2);
            assertThat(grouped.get("person")).hasSize(2);
            assertThat(grouped.get("environment")).hasSize(1);
            assertThat(grouped.get("other")).hasSize(1);
        }

        @Test
        @DisplayName("Should handle frames with null category as 'other'")
        void shouldHandleNullCategoryAsOther() {
            List<FrameAnalysisResult> frames = Arrays.asList(
                    createResult(1L, null, 80),
                    createResult(2L, "food", 75)
            );

            Map<String, List<FrameAnalysisResult>> grouped = service.groupByCategory(frames);

            assertThat(grouped.get("other")).hasSize(1);
            assertThat(grouped.get("food")).hasSize(1);
        }
    }

    @Nested
    @DisplayName("2.3-UNIT-008: Sort frames by quality score descending")
    class SortByScoreTests {

        @Test
        @DisplayName("Should sort frames by quality score descending")
        void shouldSortByScoreDescending() {
            List<FrameAnalysisResult> frames = Arrays.asList(
                    createResult(1L, "food", 70),
                    createResult(2L, "food", 95),
                    createResult(3L, "food", 80)
            );

            List<FrameAnalysisResult> sorted = service.sortByQualityScore(frames);

            assertThat(sorted).extracting(FrameAnalysisResult::getQualityScore)
                    .containsExactly(95, 80, 70);
        }

        @Test
        @DisplayName("Should sort [80, 95, 70] to [95, 80, 70]")
        void shouldSortSpecificExample() {
            List<FrameAnalysisResult> frames = Arrays.asList(
                    createResult(1L, "food", 80),
                    createResult(2L, "food", 95),
                    createResult(3L, "food", 70)
            );

            List<FrameAnalysisResult> sorted = service.sortByQualityScore(frames);

            assertThat(sorted).extracting(FrameAnalysisResult::getQualityScore)
                    .containsExactly(95, 80, 70);
        }
    }

    @Nested
    @DisplayName("2.3-UNIT-009: Mark exactly top 2 per category as recommended (BR-2.1)")
    class Top2SelectionTests {

        @Test
        @DisplayName("Should mark exactly top 2 from 5 food frames")
        void shouldMarkTop2From5Frames() {
            List<FrameAnalysisResult> frames = Arrays.asList(
                    createResult(1L, "food", 70),
                    createResult(2L, "food", 80),
                    createResult(3L, "food", 90),
                    createResult(4L, "food", 60),
                    createResult(5L, "food", 85)
            );

            List<Long> recommended = service.markRecommendedFrames(frames);

            assertThat(recommended).hasSize(2);
            // Top 2 scores are 90 (frame 3) and 85 (frame 5)
            assertThat(recommended).containsExactlyInAnyOrder(3L, 5L);
        }

        @Test
        @DisplayName("Should mark top 2 from each category")
        void shouldMarkTop2FromEachCategory() {
            List<FrameAnalysisResult> frames = Arrays.asList(
                    // Food category
                    createResult(1L, "food", 70),
                    createResult(2L, "food", 90),
                    createResult(3L, "food", 80),
                    // Person category
                    createResult(4L, "person", 85),
                    createResult(5L, "person", 75),
                    createResult(6L, "person", 95)
            );

            List<Long> recommended = service.markRecommendedFrames(frames);

            assertThat(recommended).hasSize(4);
            // Food: 90 (frame 2), 80 (frame 3)
            // Person: 95 (frame 6), 85 (frame 4)
            assertThat(recommended).containsExactlyInAnyOrder(2L, 3L, 4L, 6L);
        }
    }

    @Nested
    @DisplayName("2.3-UNIT-010: Handle tie-breaking when scores equal")
    class TieBreakingTests {

        @Test
        @DisplayName("Should use frame ID for deterministic tie-breaking")
        void shouldUseDeterministicTieBreaking() {
            List<FrameAnalysisResult> frames = Arrays.asList(
                    createResult(3L, "food", 80),
                    createResult(1L, "food", 80),
                    createResult(2L, "food", 80)
            );

            List<Long> recommended = service.markRecommendedFrames(frames);

            assertThat(recommended).hasSize(2);
            // With equal scores, lower frame IDs should win
            assertThat(recommended).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("Should maintain deterministic order across multiple calls")
        void shouldMaintainDeterministicOrder() {
            List<FrameAnalysisResult> frames = Arrays.asList(
                    createResult(5L, "food", 80),
                    createResult(3L, "food", 80),
                    createResult(7L, "food", 80),
                    createResult(1L, "food", 80)
            );

            List<Long> firstCall = service.markRecommendedFrames(frames);
            List<Long> secondCall = service.markRecommendedFrames(frames);

            assertThat(firstCall).isEqualTo(secondCall);
        }
    }

    @Nested
    @DisplayName("2.3-UNIT-011: Handle category with <2 frames")
    class LessThan2FramesTests {

        @Test
        @DisplayName("Should mark 1 frame when category has only 1 frame")
        void shouldMark1FrameWhenCategoryHas1() {
            List<FrameAnalysisResult> frames = Arrays.asList(
                    createResult(1L, "food", 80)
            );

            List<Long> recommended = service.markRecommendedFrames(frames);

            assertThat(recommended).hasSize(1);
            assertThat(recommended).containsExactly(1L);
        }

        @Test
        @DisplayName("Should handle mixed: some categories with 1, some with more")
        void shouldHandleMixedCategorySizes() {
            List<FrameAnalysisResult> frames = Arrays.asList(
                    createResult(1L, "food", 80),
                    createResult(2L, "person", 70),
                    createResult(3L, "person", 90),
                    createResult(4L, "person", 85)
            );

            List<Long> recommended = service.markRecommendedFrames(frames);

            assertThat(recommended).hasSize(3);
            // Food: 1 frame (frame 1)
            // Person: top 2 (frames 3 and 4 with scores 90 and 85)
            assertThat(recommended).containsExactlyInAnyOrder(1L, 3L, 4L);
        }
    }

    @Nested
    @DisplayName("2.3-BLIND-BOUNDARY-001: Empty frames array")
    class EmptyFramesTests {

        @Test
        @DisplayName("Should return empty list for empty input")
        void shouldReturnEmptyForEmptyInput() {
            List<Long> recommended = service.markRecommendedFrames(Collections.emptyList());

            assertThat(recommended).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list for null input")
        void shouldReturnEmptyForNullInput() {
            List<Long> recommended = service.markRecommendedFrames(null);

            assertThat(recommended).isEmpty();
        }

        @Test
        @DisplayName("Should handle all failed analyses")
        void shouldHandleAllFailedAnalyses() {
            List<FrameAnalysisResult> frames = Arrays.asList(
                    FrameAnalysisResult.failed(1L, "url1", "error"),
                    FrameAnalysisResult.failed(2L, "url2", "error")
            );

            List<Long> recommended = service.markRecommendedFrames(frames);

            assertThat(recommended).isEmpty();
        }
    }

    @Nested
    @DisplayName("2.3-BLIND-BOUNDARY-002: Single frame input")
    class SingleFrameTests {

        @Test
        @DisplayName("Should recommend single frame in single category")
        void shouldRecommendSingleFrame() {
            List<FrameAnalysisResult> frames = Arrays.asList(
                    createResult(1L, "food", 80)
            );

            List<Long> recommended = service.markRecommendedFrames(frames);

            assertThat(recommended).containsExactly(1L);
        }
    }

    /**
     * Helper to create a successful FrameAnalysisResult.
     */
    private FrameAnalysisResult createResult(Long frameId, String category, int qualityScore) {
        return FrameAnalysisResult.builder()
                .frameId(frameId)
                .frameUrl("https://example.com/frame" + frameId + ".jpg")
                .category(category)
                .qualityScore(qualityScore)
                .success(true)
                .build();
    }
}
