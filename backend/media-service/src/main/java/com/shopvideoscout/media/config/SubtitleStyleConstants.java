package com.shopvideoscout.media.config;

import java.util.Map;

/**
 * Subtitle style constants for ASS format (Story 4.3).
 * Defines 5 preset styles with ASS V4+ Style format strings.
 *
 * ASS color format: &HAABBGGRR (note: BGR order, not RGB)
 */
public final class SubtitleStyleConstants {

    private SubtitleStyleConstants() {
    }

    /**
     * Default style key.
     */
    public static final String DEFAULT_STYLE = "simple_white";

    /**
     * Style definitions in ASS V4+ format.
     * Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour,
     *         Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle,
     *         BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
     */
    public static final Map<String, StyleDefinition> STYLES = Map.of(
            "simple_white", new StyleDefinition(
                    "SimpleWhite",
                    "PingFang SC",
                    48,
                    "&H00FFFFFF", // White
                    "&H000000FF", // Red (secondary)
                    "&H00000000", // Black outline
                    "&H80000000", // Semi-transparent back
                    false, // Bold
                    2,     // Outline width
                    1      // Shadow depth
            ),
            "vibrant_yellow", new StyleDefinition(
                    "VibrantYellow",
                    "PingFang SC",
                    48,
                    "&H0000D7FF", // Yellow (BGR: FFD700)
                    "&H000000FF",
                    "&H00000000",
                    "&H80000000",
                    true,  // Bold
                    2,
                    1
            ),
            "xiaohongshu", new StyleDefinition(
                    "Xiaohongshu",
                    "PingFang SC",
                    48,
                    "&H00FFFFFF", // White
                    "&H000000FF",
                    "&H005747FF", // Red outline (BGR: FF4757)
                    "&H00000000", // No back
                    false,
                    3,
                    0      // No shadow
            ),
            "douyin_hot", new StyleDefinition(
                    "DouyinHot",
                    "PingFang SC",
                    48,
                    "&H00FFFFFF", // White
                    "&H000000FF",
                    "&H00000000", // Black outline
                    "&H80000000",
                    true,  // Bold
                    2,
                    2      // More shadow for blur effect
            ),
            "neon", new StyleDefinition(
                    "Neon",
                    "PingFang SC",
                    48,
                    "&H00FF88FF", // Pink/Purple (gradient simulated)
                    "&H0000FFFF", // Cyan secondary
                    "&H0000FFFF", // Cyan glow outline
                    "&H80000000",
                    true,  // Bold
                    3,
                    2
            )
    );

    /**
     * Get style by key, with fallback to default.
     */
    public static StyleDefinition getStyle(String styleKey) {
        if (styleKey == null || !STYLES.containsKey(styleKey)) {
            return STYLES.get(DEFAULT_STYLE);
        }
        return STYLES.get(styleKey);
    }

    /**
     * Get ASS format line for a style.
     */
    public static String toAssFormatLine(StyleDefinition style) {
        return String.format(
                "Style: %s,%s,%d,%s,%s,%s,%s,%d,0,0,0,100,100,0,0,1,%.1f,%.1f,2,10,10,60,1",
                style.getName(),
                style.getFontName(),
                style.getFontSize(),
                style.getPrimaryColor(),
                style.getSecondaryColor(),
                style.getOutlineColor(),
                style.getBackColor(),
                style.isBold() ? 1 : 0,
                style.getOutlineWidth(),
                style.getShadowDepth()
        );
    }

    /**
     * Get semi-transparent version of a style for dual-line effect.
     * Uses alpha channel to make text semi-transparent.
     */
    public static String toAssFormatLineTransparent(StyleDefinition style) {
        // Replace alpha channel in primary color with 80 (semi-transparent)
        String transparentPrimary = style.getPrimaryColor().substring(0, 2) + "80" +
                style.getPrimaryColor().substring(4);
        return String.format(
                "Style: %sNext,%s,%d,%s,%s,%s,%s,%d,0,0,0,100,100,0,0,1,%.1f,%.1f,2,10,10,60,1",
                style.getName(),
                style.getFontName(),
                style.getFontSize(),
                transparentPrimary,
                style.getSecondaryColor(),
                style.getOutlineColor(),
                style.getBackColor(),
                style.isBold() ? 1 : 0,
                style.getOutlineWidth(),
                style.getShadowDepth()
        );
    }

    /**
     * Style definition DTO.
     */
    public static class StyleDefinition {
        private final String name;
        private final String fontName;
        private final int fontSize;
        private final String primaryColor;
        private final String secondaryColor;
        private final String outlineColor;
        private final String backColor;
        private final boolean bold;
        private final double outlineWidth;
        private final double shadowDepth;

        public StyleDefinition(String name, String fontName, int fontSize,
                                String primaryColor, String secondaryColor,
                                String outlineColor, String backColor,
                                boolean bold, double outlineWidth, double shadowDepth) {
            this.name = name;
            this.fontName = fontName;
            this.fontSize = fontSize;
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
            this.outlineColor = outlineColor;
            this.backColor = backColor;
            this.bold = bold;
            this.outlineWidth = outlineWidth;
            this.shadowDepth = shadowDepth;
        }

        public String getName() { return name; }
        public String getFontName() { return fontName; }
        public int getFontSize() { return fontSize; }
        public String getPrimaryColor() { return primaryColor; }
        public String getSecondaryColor() { return secondaryColor; }
        public String getOutlineColor() { return outlineColor; }
        public String getBackColor() { return backColor; }
        public boolean isBold() { return bold; }
        public double getOutlineWidth() { return outlineWidth; }
        public double getShadowDepth() { return shadowDepth; }
    }
}
