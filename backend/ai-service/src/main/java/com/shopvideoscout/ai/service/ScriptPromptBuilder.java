package com.shopvideoscout.ai.service;

import com.shopvideoscout.ai.dto.ShotSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds prompts for Doubao API script generation.
 * Includes shop information and shot summaries from analyzed videos.
 */
@Slf4j
@Component
public class ScriptPromptBuilder {

    private static final String PROMPT_TEMPLATE = """
        请根据以下信息生成探店口播脚本：

        店铺信息：
        - 名称：%s
        - 类型：%s
        - 特色/优惠：%s

        视频风格：%s（%s）

        可用镜头：
        %s

        要求：
        1. 生成5-7个段落
        2. 每段标注对应使用的镜头shot_id
        3. 包含：开场hook、环境介绍、重点内容、优惠信息、结尾互动
        4. 总时长控制在60秒左右
        5. 语言风格符合%s调性

        输出JSON格式：
        {
          "paragraphs": [
            {
              "id": "para_1",
              "section": "开场",
              "shot_id": xxx,
              "text": "...",
              "estimated_duration": 8
            },
            ...
          ],
          "total_duration": 60
        }
        """;

    /**
     * Build prompt for script generation.
     *
     * @param shopName      Shop name
     * @param shopType      Shop type (food, beauty, etc.)
     * @param promotionText Promotion/discount text
     * @param videoStyle    Video style (recommend, review, vlog)
     * @param shotSummaries List of shot summaries from analyzed videos
     * @return Formatted prompt string
     */
    public String buildPrompt(String shopName, String shopType, String promotionText,
                               String videoStyle, List<ShotSummary> shotSummaries) {

        String styleDescription = getStyleDescription(videoStyle);
        String shotsText = formatShotSummaries(shotSummaries);

        String prompt = String.format(PROMPT_TEMPLATE,
                shopName != null ? shopName : "未知店铺",
                shopType != null ? shopType : "其他",
                promotionText != null ? promotionText : "无特别优惠",
                videoStyle,
                styleDescription,
                shotsText,
                videoStyle);

        log.debug("Built prompt with {} characters, {} shots", prompt.length(), shotSummaries.size());
        return prompt;
    }

    /**
     * Get human-readable description for video style.
     */
    private String getStyleDescription(String videoStyle) {
        return switch (videoStyle) {
            case "recommend" -> "种草安利型 - 热情推荐，突出亮点，带动购买欲";
            case "review" -> "真实测评型 - 客观评价，优缺点都说，建立信任";
            case "vlog" -> "探店vlog型 - 轻松随意，记录体验，有个人风格";
            default -> "探店介绍型";
        };
    }

    /**
     * Format shot summaries for prompt inclusion.
     */
    private String formatShotSummaries(List<ShotSummary> shots) {
        if (shots == null || shots.isEmpty()) {
            return "无可用镜头";
        }

        return shots.stream()
                .map(shot -> String.format(
                        "- shot_id: %d, 分类: %s, 标签: %s, 质量分: %d%s",
                        shot.getShotId(),
                        shot.getCategory(),
                        String.join("/", shot.getTags()),
                        shot.getQualityScore(),
                        shot.isRecommended() ? " [推荐]" : ""
                ))
                .collect(Collectors.joining("\n"));
    }
}
