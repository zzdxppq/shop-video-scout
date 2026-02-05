package com.shopvideoscout.publish.prompt;

import org.springframework.stereotype.Component;

/**
 * Builds prompts for publish assist generation.
 * Story 5.3: 发布辅助服务
 */
@Component
public class PublishAssistPromptBuilder {

    private static final String PROMPT_TEMPLATE = """
            基于以下探店视频信息，生成发布辅助内容：

            店铺：%s
            类型：%s
            脚本摘要：%s

            请生成：
            1. 8-10个相关话题标签（以#开头，符合抖音/小红书规范，每个不超过20字符）
            2. 4个吸引眼球的视频标题（20-50字，包含数字/疑问/惊叹等吸引元素）

            输出JSON：
            {
              "topics": ["#话题1", "#话题2", ...],
              "titles": ["标题1", "标题2", "标题3", "标题4"]
            }

            注意：
            - 话题必须以#开头，不含空格
            - 话题应包含店铺名相关话题
            - 话题应包含品类相关话题（如美食、火锅、咖啡等）
            - 标题要吸引人，符合短视频平台调性
            - 只输出JSON，不要其他内容
            """;

    /**
     * Build the prompt for topics and titles generation.
     *
     * @param shopName      shop name
     * @param shopType      shop type (food, beauty, etc.)
     * @param scriptSummary summary of the video script
     * @return formatted prompt
     */
    public String buildPrompt(String shopName, String shopType, String scriptSummary) {
        String typeLabel = mapShopType(shopType);
        return String.format(PROMPT_TEMPLATE, shopName, typeLabel, scriptSummary);
    }

    /**
     * Map shop type code to Chinese label.
     */
    private String mapShopType(String shopType) {
        if (shopType == null) {
            return "其他";
        }
        return switch (shopType.toLowerCase()) {
            case "food" -> "美食";
            case "beauty" -> "美容美发";
            case "entertainment" -> "休闲娱乐";
            default -> "其他";
        };
    }
}
