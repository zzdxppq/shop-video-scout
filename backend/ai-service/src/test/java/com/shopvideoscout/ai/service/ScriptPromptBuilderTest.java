package com.shopvideoscout.ai.service;

import com.shopvideoscout.ai.dto.ShotSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ScriptPromptBuilder.
 * Story 3.1 - AC1: Test Specs for prompt building.
 */
class ScriptPromptBuilderTest {

    private ScriptPromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new ScriptPromptBuilder();
    }

    // 3.1-UNIT-001: Prompt includes shop_name, shop_type, promotion_text
    @Test
    @DisplayName("Prompt should include shop_name, shop_type, promotion_text")
    void buildPrompt_shouldIncludeShopInfo() {
        List<ShotSummary> shots = createSampleShots();

        String prompt = promptBuilder.buildPrompt(
                "海底捞望京店",
                "food",
                "满300减50",
                "recommend",
                shots
        );

        assertThat(prompt).contains("海底捞望京店");
        assertThat(prompt).contains("food");
        assertThat(prompt).contains("满300减50");
    }

    // 3.1-UNIT-002: Prompt includes shot_summaries from analyzed videos
    @Test
    @DisplayName("Prompt should include shot summaries with shot_id")
    void buildPrompt_shouldIncludeShotSummaries() {
        List<ShotSummary> shots = createSampleShots();

        String prompt = promptBuilder.buildPrompt(
                "店铺名称",
                "food",
                "优惠信息",
                "recommend",
                shots
        );

        // Should include shot_id references
        assertThat(prompt).contains("shot_id: 101");
        assertThat(prompt).contains("shot_id: 102");
        // Should include categories and tags
        assertThat(prompt).contains("food");
        assertThat(prompt).contains("美食");
        assertThat(prompt).contains("[推荐]");
    }

    // 3.1-UNIT-003: Prompt template varies by video_style
    @Test
    @DisplayName("Prompt should vary by video_style - recommend")
    void buildPrompt_shouldVaryByVideoStyle_recommend() {
        String prompt = promptBuilder.buildPrompt(
                "店铺名称", "food", "优惠", "recommend", createSampleShots()
        );

        assertThat(prompt).contains("recommend");
        assertThat(prompt).contains("种草安利型");
    }

    @Test
    @DisplayName("Prompt should vary by video_style - review")
    void buildPrompt_shouldVaryByVideoStyle_review() {
        String prompt = promptBuilder.buildPrompt(
                "店铺名称", "food", "优惠", "review", createSampleShots()
        );

        assertThat(prompt).contains("review");
        assertThat(prompt).contains("真实测评型");
    }

    @Test
    @DisplayName("Prompt should vary by video_style - vlog")
    void buildPrompt_shouldVaryByVideoStyle_vlog() {
        String prompt = promptBuilder.buildPrompt(
                "店铺名称", "food", "优惠", "vlog", createSampleShots()
        );

        assertThat(prompt).contains("vlog");
        assertThat(prompt).contains("探店vlog型");
    }

    // 3.1-BLIND-BOUNDARY-001: Empty shop_name in prompt building
    @Test
    @DisplayName("Prompt should handle empty shop_name gracefully")
    void buildPrompt_shouldHandleEmptyShopName() {
        String prompt = promptBuilder.buildPrompt(
                null, "food", "优惠", "recommend", createSampleShots()
        );

        assertThat(prompt).contains("未知店铺");
    }

    // 3.1-BLIND-BOUNDARY-002: Empty shot_summaries
    @Test
    @DisplayName("Prompt should handle empty shot_summaries")
    void buildPrompt_shouldHandleEmptyShotSummaries() {
        String prompt = promptBuilder.buildPrompt(
                "店铺名称", "food", "优惠", "recommend", Collections.emptyList()
        );

        assertThat(prompt).contains("无可用镜头");
    }

    @Test
    @DisplayName("Prompt should handle null shot_summaries")
    void buildPrompt_shouldHandleNullShotSummaries() {
        String prompt = promptBuilder.buildPrompt(
                "店铺名称", "food", "优惠", "recommend", null
        );

        assertThat(prompt).contains("无可用镜头");
    }

    @Test
    @DisplayName("Prompt should handle null promotion_text")
    void buildPrompt_shouldHandleNullPromotionText() {
        String prompt = promptBuilder.buildPrompt(
                "店铺名称", "food", null, "recommend", createSampleShots()
        );

        assertThat(prompt).contains("无特别优惠");
    }

    // Helper method to create sample shots
    private List<ShotSummary> createSampleShots() {
        return Arrays.asList(
                ShotSummary.builder()
                        .shotId(101L)
                        .category("food")
                        .tags(Arrays.asList("美食", "色彩丰富"))
                        .qualityScore(85)
                        .recommended(true)
                        .build(),
                ShotSummary.builder()
                        .shotId(102L)
                        .category("environment")
                        .tags(Arrays.asList("室内", "明亮"))
                        .qualityScore(78)
                        .recommended(false)
                        .build()
        );
    }
}
