package com.shopvideoscout.task.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for FilenameUtils (Story 5.2).
 */
class FilenameUtilsTest {

    @Nested
    @DisplayName("sanitize")
    class SanitizeTests {

        @Test
        @DisplayName("should return sanitized name for normal input")
        void shouldSanitizeNormalName() {
            assertThat(FilenameUtils.sanitize("海底捞望京店")).isEqualTo("海底捞望京店");
        }

        @Test
        @DisplayName("should replace forward slash with hyphen")
        void shouldReplaceForwardSlash() {
            assertThat(FilenameUtils.sanitize("海底捞/望京店")).isEqualTo("海底捞-望京店");
        }

        @Test
        @DisplayName("should replace backslash with hyphen")
        void shouldReplaceBackslash() {
            assertThat(FilenameUtils.sanitize("海底捞\\望京店")).isEqualTo("海底捞-望京店");
        }

        @Test
        @DisplayName("should replace colon with hyphen")
        void shouldReplaceColon() {
            assertThat(FilenameUtils.sanitize("店名:测试")).isEqualTo("店名-测试");
        }

        @Test
        @DisplayName("should replace asterisk with hyphen")
        void shouldReplaceAsterisk() {
            assertThat(FilenameUtils.sanitize("店名*测试")).isEqualTo("店名-测试");
        }

        @Test
        @DisplayName("should replace multiple special chars")
        void shouldReplaceMultipleSpecialChars() {
            assertThat(FilenameUtils.sanitize("店名:*?test")).isEqualTo("店名-test");
        }

        @Test
        @DisplayName("should collapse multiple hyphens")
        void shouldCollapseMultipleHyphens() {
            assertThat(FilenameUtils.sanitize("店名//测试")).isEqualTo("店名-测试");
        }

        @Test
        @DisplayName("should trim whitespace")
        void shouldTrimWhitespace() {
            assertThat(FilenameUtils.sanitize("  店名  ")).isEqualTo("店名");
        }

        @Test
        @DisplayName("should truncate to 50 chars")
        void shouldTruncateTo50Chars() {
            String longName = "a".repeat(60);
            assertThat(FilenameUtils.sanitize(longName)).hasSize(50);
        }

        @Test
        @DisplayName("should remove trailing hyphen after truncation")
        void shouldRemoveTrailingHyphen() {
            assertThat(FilenameUtils.sanitize("test-")).isEqualTo("test");
        }

        @Test
        @DisplayName("should return unknown for null input")
        void shouldReturnUnknownForNull() {
            assertThat(FilenameUtils.sanitize(null)).isEqualTo("unknown");
        }

        @Test
        @DisplayName("should return unknown for blank input")
        void shouldReturnUnknownForBlank() {
            assertThat(FilenameUtils.sanitize("   ")).isEqualTo("unknown");
        }

        @Test
        @DisplayName("should return unknown for only special chars")
        void shouldReturnUnknownForOnlySpecialChars() {
            assertThat(FilenameUtils.sanitize("/:*?")).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("generateVideoFilename")
    class GenerateVideoFilenameTests {

        @Test
        @DisplayName("should generate correct format")
        void shouldGenerateCorrectFormat() {
            String filename = FilenameUtils.generateVideoFilename("海底捞");
            assertThat(filename).matches("海底捞-探店视频-\\d{8}\\.mp4");
        }

        @Test
        @DisplayName("should sanitize shop name in filename")
        void shouldSanitizeShopName() {
            String filename = FilenameUtils.generateVideoFilename("海底捞/望京店");
            assertThat(filename).matches("海底捞-望京店-探店视频-\\d{8}\\.mp4");
        }
    }

    @Nested
    @DisplayName("generateAssetPackFilename")
    class GenerateAssetPackFilenameTests {

        @Test
        @DisplayName("should generate correct format")
        void shouldGenerateCorrectFormat() {
            String filename = FilenameUtils.generateAssetPackFilename("海底捞");
            assertThat(filename).matches("海底捞-素材包-\\d{8}\\.zip");
        }
    }

    @Nested
    @DisplayName("generateShotFilename")
    class GenerateShotFilenameTests {

        @Test
        @DisplayName("should generate correct format with sequence")
        void shouldGenerateCorrectFormat() {
            assertThat(FilenameUtils.generateShotFilename(1, "美食")).isEqualTo("01-美食.mp4");
            assertThat(FilenameUtils.generateShotFilename(10, "环境")).isEqualTo("10-环境.mp4");
        }

        @Test
        @DisplayName("should handle null category")
        void shouldHandleNullCategory() {
            assertThat(FilenameUtils.generateShotFilename(1, null)).isEqualTo("01-未分类.mp4");
        }

        @Test
        @DisplayName("should sanitize category")
        void shouldSanitizeCategory() {
            assertThat(FilenameUtils.generateShotFilename(1, "美食/环境")).isEqualTo("01-美食-环境.mp4");
        }
    }
}
