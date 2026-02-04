package com.shopvideoscout.task.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for filename sanitization (Story 5.2).
 */
public final class FilenameUtils {

    private static final int MAX_SHOP_NAME_LENGTH = 50;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private FilenameUtils() {
        // Utility class
    }

    /**
     * Sanitize shop name for use in filename.
     * Replaces special characters (/\:*?"<>|) with hyphen.
     * Trims whitespace and limits length to 50 chars.
     *
     * @param shopName original shop name
     * @return sanitized shop name safe for filename
     */
    public static String sanitize(String shopName) {
        if (shopName == null || shopName.isBlank()) {
            return "unknown";
        }

        // Replace special characters with hyphen
        String sanitized = shopName
                .replaceAll("[/\\\\:*?\"<>|]", "-")
                .replaceAll("-+", "-")  // Collapse multiple hyphens
                .trim();

        // Limit length
        if (sanitized.length() > MAX_SHOP_NAME_LENGTH) {
            sanitized = sanitized.substring(0, MAX_SHOP_NAME_LENGTH);
        }

        // Remove trailing hyphen
        if (sanitized.endsWith("-")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }

        return sanitized.isEmpty() ? "unknown" : sanitized;
    }

    /**
     * Generate video download filename.
     * Format: {sanitizedShopName}-探店视频-{yyyyMMdd}.mp4
     *
     * @param shopName shop name
     * @return formatted filename
     */
    public static String generateVideoFilename(String shopName) {
        String sanitized = sanitize(shopName);
        String date = LocalDate.now().format(DATE_FORMATTER);
        return String.format("%s-探店视频-%s.mp4", sanitized, date);
    }

    /**
     * Generate assets pack ZIP filename.
     * Format: {sanitizedShopName}-素材包-{yyyyMMdd}.zip
     *
     * @param shopName shop name
     * @return formatted filename
     */
    public static String generateAssetPackFilename(String shopName) {
        String sanitized = sanitize(shopName);
        String date = LocalDate.now().format(DATE_FORMATTER);
        return String.format("%s-素材包-%s.zip", sanitized, date);
    }

    /**
     * Generate inner ZIP file name for a video shot.
     * Format: {sequence}-{category}.mp4
     *
     * @param sequence 1-based sequence number
     * @param category video category
     * @return formatted filename
     */
    public static String generateShotFilename(int sequence, String category) {
        String safeCategory = sanitize(category != null ? category : "未分类");
        return String.format("%02d-%s.mp4", sequence, safeCategory);
    }
}
