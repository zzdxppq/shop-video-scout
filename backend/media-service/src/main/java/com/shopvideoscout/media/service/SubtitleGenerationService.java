package com.shopvideoscout.media.service;

import com.aliyun.oss.OSS;
import com.shopvideoscout.media.config.CompositionProperties;
import com.shopvideoscout.media.config.OssConfig;
import com.shopvideoscout.media.config.SubtitleStyleConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Service for generating ASS format subtitles (Story 4.3).
 * Generates subtitles with cumulative time axis and dual-line scrolling effect.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleGenerationService {

    private static final int MAX_CHARS_PER_LINE = 20;

    private final CompositionProperties compositionProperties;
    private final OSS ossClient;
    private final OssConfig ossConfig;

    /**
     * Generate ASS subtitle file from paragraph durations.
     *
     * @param paragraphs    list of paragraph durations from TTS
     * @param subtitleStyle style key (simple_white, vibrant_yellow, etc.)
     * @param taskId        task ID
     * @return generated ASS file
     */
    public File generateSubtitle(List<VideoSegmentCuttingService.ParagraphDuration> paragraphs,
                                  String subtitleStyle,
                                  Long taskId) {
        log.info("Generating subtitles for task {}: {} paragraphs, style={}",
                taskId, paragraphs.size(), subtitleStyle);

        File tempDir = new File(compositionProperties.getTempDir(), taskId.toString());
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            log.warn("Failed to create temp directory for subtitles");
            return null;
        }

        File assFile = new File(tempDir, "subtitle.ass");

        try (PrintWriter writer = new PrintWriter(assFile, StandardCharsets.UTF_8)) {
            // Write ASS header
            writeScriptInfo(writer);

            // Write styles
            writeStyles(writer, subtitleStyle);

            // Write events (subtitle lines)
            writeEvents(writer, paragraphs, subtitleStyle);

            log.info("Subtitle file generated: {}", assFile.getAbsolutePath());
            return assFile;

        } catch (IOException e) {
            log.error("Failed to generate subtitle file: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Upload ASS file to OSS.
     *
     * @param assFile ASS file
     * @param taskId  task ID
     * @return OSS key
     */
    public String uploadToOss(File assFile, Long taskId) {
        if (assFile == null || !assFile.exists()) {
            return null;
        }

        try {
            String ossKey = "output/" + taskId + "/subtitle.ass";
            byte[] content = java.nio.file.Files.readAllBytes(assFile.toPath());
            ossClient.putObject(ossConfig.getBucketName(), ossKey,
                    new ByteArrayInputStream(content));
            log.info("Uploaded subtitle to OSS: {}", ossKey);
            return ossKey;
        } catch (IOException e) {
            log.error("Failed to upload subtitle to OSS: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Write ASS [Script Info] section.
     */
    private void writeScriptInfo(PrintWriter writer) {
        writer.println("[Script Info]");
        writer.println("Title: Shop Video Scout Subtitles");
        writer.println("ScriptType: v4.00+");
        writer.println("PlayResX: 1080");
        writer.println("PlayResY: 1920");
        writer.println("ScaledBorderAndShadow: yes");
        writer.println();
    }

    /**
     * Write ASS [V4+ Styles] section.
     */
    private void writeStyles(PrintWriter writer, String styleKey) {
        SubtitleStyleConstants.StyleDefinition style = SubtitleStyleConstants.getStyle(styleKey);

        writer.println("[V4+ Styles]");
        writer.println("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding");
        writer.println(SubtitleStyleConstants.toAssFormatLine(style));
        writer.println(SubtitleStyleConstants.toAssFormatLineTransparent(style));
        writer.println();
    }

    /**
     * Write ASS [Events] section with dual-line scrolling effect.
     * Current line uses normal style, next line uses transparent style.
     */
    private void writeEvents(PrintWriter writer,
                              List<VideoSegmentCuttingService.ParagraphDuration> paragraphs,
                              String styleKey) {
        SubtitleStyleConstants.StyleDefinition style = SubtitleStyleConstants.getStyle(styleKey);
        String styleName = style.getName();
        String styleNameNext = styleName + "Next";

        writer.println("[Events]");
        writer.println("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text");

        double cumulativeTime = 0.0;

        for (int i = 0; i < paragraphs.size(); i++) {
            VideoSegmentCuttingService.ParagraphDuration pd = paragraphs.get(i);
            double startTime = cumulativeTime;
            double endTime = cumulativeTime + pd.getActualDurationSeconds();

            String text = pd.getText();
            if (text == null || text.isBlank()) {
                cumulativeTime = endTime;
                continue;
            }

            // Auto-wrap long text
            String wrappedText = wrapText(text);

            // Write current line (normal style)
            writer.printf("Dialogue: 0,%s,%s,%s,,0,0,0,,%s%n",
                    formatTime(startTime),
                    formatTime(endTime),
                    styleName,
                    wrappedText);

            // Write next line preview (semi-transparent) if not last paragraph
            if (i < paragraphs.size() - 1) {
                VideoSegmentCuttingService.ParagraphDuration nextPd = paragraphs.get(i + 1);
                String nextText = nextPd.getText();
                if (nextText != null && !nextText.isBlank()) {
                    String nextWrappedText = wrapText(nextText);
                    // Show next line during current paragraph with position offset
                    writer.printf("Dialogue: 1,%s,%s,%s,,0,0,120,,%s%n",
                            formatTime(startTime),
                            formatTime(endTime),
                            styleNameNext,
                            nextWrappedText);
                }
            }

            cumulativeTime = endTime;
        }
    }

    /**
     * Format time in ASS format: H:MM:SS.CC
     */
    String formatTime(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        double secs = seconds % 60;
        return String.format("%d:%02d:%05.2f", hours, minutes, secs);
    }

    /**
     * Wrap text at MAX_CHARS_PER_LINE characters using ASS line break.
     */
    String wrapText(String text) {
        if (text == null || text.length() <= MAX_CHARS_PER_LINE) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int currentLineLength = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            result.append(c);
            currentLineLength++;

            if (currentLineLength >= MAX_CHARS_PER_LINE && i < text.length() - 1) {
                result.append("\\N");
                currentLineLength = 0;
            }
        }

        return result.toString();
    }

    /**
     * Calculate cumulative start time for a paragraph.
     */
    public double calculateCumulativeStartTime(List<VideoSegmentCuttingService.ParagraphDuration> paragraphs,
                                                 int paragraphIndex) {
        double cumulativeTime = 0.0;
        for (int i = 0; i < paragraphIndex && i < paragraphs.size(); i++) {
            cumulativeTime += paragraphs.get(i).getActualDurationSeconds();
        }
        return cumulativeTime;
    }
}
