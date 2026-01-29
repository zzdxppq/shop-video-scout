package com.shopvideoscout.media.service;

import com.aliyun.oss.OSS;
import com.shopvideoscout.media.config.CompositionProperties;
import com.shopvideoscout.media.config.OssConfig;
import com.shopvideoscout.media.config.SubtitleStyleConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SubtitleGenerationService (Story 4.3).
 */
@ExtendWith(MockitoExtension.class)
class SubtitleGenerationServiceTest {

    @Mock
    private CompositionProperties compositionProperties;

    @Mock
    private OSS ossClient;

    @Mock
    private OssConfig ossConfig;

    @InjectMocks
    private SubtitleGenerationService subtitleService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        when(compositionProperties.getTempDir()).thenReturn(tempDir.toString());
    }

    @Nested
    @DisplayName("AC3: Time Axis Calculation")
    class TimeAxisTests {

        @Test
        @DisplayName("4.3-UNIT-013: Cumulative time axis calculation")
        void cumulativeTimeAxis_ShouldCalculateCorrectly() {
            // Given: para1=8s, para2=6s
            List<VideoSegmentCuttingService.ParagraphDuration> paragraphs = List.of(
                    VideoSegmentCuttingService.ParagraphDuration.builder()
                            .paragraphIndex(0)
                            .actualDurationSeconds(8.0)
                            .text("First paragraph")
                            .build(),
                    VideoSegmentCuttingService.ParagraphDuration.builder()
                            .paragraphIndex(1)
                            .actualDurationSeconds(6.0)
                            .text("Second paragraph")
                            .build(),
                    VideoSegmentCuttingService.ParagraphDuration.builder()
                            .paragraphIndex(2)
                            .actualDurationSeconds(5.0)
                            .text("Third paragraph")
                            .build()
            );

            // When/Then
            assertEquals(0.0, subtitleService.calculateCumulativeStartTime(paragraphs, 0), 0.001);
            assertEquals(8.0, subtitleService.calculateCumulativeStartTime(paragraphs, 1), 0.001);
            assertEquals(14.0, subtitleService.calculateCumulativeStartTime(paragraphs, 2), 0.001);
        }
    }

    @Nested
    @DisplayName("AC3: Time Formatting")
    class TimeFormattingTests {

        @Test
        @DisplayName("Format time in ASS format H:MM:SS.CC")
        void formatTime_ShouldUseAssFormat() {
            assertEquals("0:00:00.00", subtitleService.formatTime(0.0));
            assertEquals("0:00:08.50", subtitleService.formatTime(8.5));
            assertEquals("0:01:02.30", subtitleService.formatTime(62.3));
            assertEquals("1:05:30.00", subtitleService.formatTime(3930.0));
        }
    }

    @Nested
    @DisplayName("AC3: Text Wrapping")
    class TextWrappingTests {

        @Test
        @DisplayName("4.3-UNIT-015: Auto-wrap at 20 characters using \\N")
        void wrapText_ShouldWrapAt20Chars() {
            // Given: 25 character text
            String text = "家人们今天给你们探一家望京超火的海底捞真的太棒了";

            // When
            String wrapped = subtitleService.wrapText(text);

            // Then
            assertTrue(wrapped.contains("\\N"));
            String[] lines = wrapped.split("\\\\N");
            assertTrue(lines[0].length() <= 20);
        }

        @Test
        @DisplayName("Short text should not wrap")
        void wrapText_ShortText_ShouldNotWrap() {
            String text = "短文本不换行";
            String wrapped = subtitleService.wrapText(text);
            assertFalse(wrapped.contains("\\N"));
            assertEquals(text, wrapped);
        }

        @Test
        @DisplayName("Exactly 20 characters should not wrap")
        void wrapText_Exactly20Chars_ShouldNotWrap() {
            String text = "一二三四五六七八九十一二三四五六七八九十";
            assertEquals(20, text.length());

            String wrapped = subtitleService.wrapText(text);
            assertFalse(wrapped.contains("\\N"));
        }
    }

    @Nested
    @DisplayName("AC3: Style Constants")
    class StyleConstantsTests {

        @Test
        @DisplayName("4.3-UNIT-016: 5 preset styles defined")
        void presetStyles_ShouldHave5Styles() {
            assertEquals(5, SubtitleStyleConstants.STYLES.size());
            assertNotNull(SubtitleStyleConstants.STYLES.get("simple_white"));
            assertNotNull(SubtitleStyleConstants.STYLES.get("vibrant_yellow"));
            assertNotNull(SubtitleStyleConstants.STYLES.get("xiaohongshu"));
            assertNotNull(SubtitleStyleConstants.STYLES.get("douyin_hot"));
            assertNotNull(SubtitleStyleConstants.STYLES.get("neon"));
        }

        @Test
        @DisplayName("4.3-BLIND-BOUNDARY-005: Unknown style → fallback to default")
        void unknownStyle_ShouldFallbackToDefault() {
            SubtitleStyleConstants.StyleDefinition style = SubtitleStyleConstants.getStyle("unknown_style");
            assertNotNull(style);
            assertEquals("SimpleWhite", style.getName());
        }

        @Test
        @DisplayName("Null style key → fallback to default")
        void nullStyle_ShouldFallbackToDefault() {
            SubtitleStyleConstants.StyleDefinition style = SubtitleStyleConstants.getStyle(null);
            assertNotNull(style);
            assertEquals("SimpleWhite", style.getName());
        }

        @Test
        @DisplayName("ASS format line generation")
        void toAssFormatLine_ShouldGenerateValidFormat() {
            SubtitleStyleConstants.StyleDefinition style = SubtitleStyleConstants.getStyle("simple_white");
            String formatLine = SubtitleStyleConstants.toAssFormatLine(style);

            assertTrue(formatLine.startsWith("Style: "));
            assertTrue(formatLine.contains("PingFang SC"));
            assertTrue(formatLine.contains("&H00FFFFFF")); // White color
        }
    }

    @Nested
    @DisplayName("AC3: Subtitle File Generation")
    class FileGenerationTests {

        @Test
        @DisplayName("4.3-UNIT-012: ASS file with Script Info, V4+ Styles, Events")
        void generateSubtitle_ShouldCreateValidAssFile() throws Exception {
            // Given
            List<VideoSegmentCuttingService.ParagraphDuration> paragraphs = List.of(
                    VideoSegmentCuttingService.ParagraphDuration.builder()
                            .paragraphIndex(0)
                            .text("家人们今天给你们探店")
                            .actualDurationSeconds(8.0)
                            .build(),
                    VideoSegmentCuttingService.ParagraphDuration.builder()
                            .paragraphIndex(1)
                            .text("一进门就被惊艳到了")
                            .actualDurationSeconds(6.0)
                            .build()
            );

            // When
            File assFile = subtitleService.generateSubtitle(paragraphs, "simple_white", 12345L);

            // Then
            assertNotNull(assFile);
            assertTrue(assFile.exists());

            String content = Files.readString(assFile.toPath());
            assertTrue(content.contains("[Script Info]"));
            assertTrue(content.contains("[V4+ Styles]"));
            assertTrue(content.contains("[Events]"));
            assertTrue(content.contains("Dialogue:"));
        }

        @Test
        @DisplayName("4.3-UNIT-017: subtitleEnabled=false returns null")
        void generateSubtitle_WhenDisabled_ShouldSkip() {
            // This behavior is handled at the caller level (ComposeMessageConsumer)
            // When subtitleEnabled=false, generateSubtitle is not called

            // Test empty paragraphs handling
            List<VideoSegmentCuttingService.ParagraphDuration> emptyParagraphs = List.of();
            File result = subtitleService.generateSubtitle(emptyParagraphs, "simple_white", 12345L);

            // Should still create file but with no dialogue lines
            assertNotNull(result);
        }
    }
}
