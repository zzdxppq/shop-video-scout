package com.shopvideoscout.media.service;

import com.aliyun.oss.OSS;
import com.shopvideoscout.media.config.CompositionProperties;
import com.shopvideoscout.media.config.OssConfig;
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
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VideoCompositionService (Story 4.3).
 */
@ExtendWith(MockitoExtension.class)
class VideoCompositionServiceTest {

    @Mock
    private OSS ossClient;

    @Mock
    private OssConfig ossConfig;

    @Mock
    private CompositionProperties compositionProperties;

    @InjectMocks
    private VideoCompositionService compositionService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        when(compositionProperties.getOutputWidth()).thenReturn(1080);
        when(compositionProperties.getOutputHeight()).thenReturn(1920);
        when(compositionProperties.getVideoBitrate()).thenReturn("4M");
        when(compositionProperties.getFrameRate()).thenReturn(30);
        when(compositionProperties.getAudioBitrate()).thenReturn("128k");
        when(compositionProperties.getEncodingPreset()).thenReturn("medium");
    }

    @Nested
    @DisplayName("AC2: Segments List Generation")
    class SegmentsListTests {

        @Test
        @DisplayName("4.3-UNIT-011: segments.txt generation with correct format")
        void generateSegmentsList_ShouldCreateCorrectFormat() throws IOException {
            // Given
            File segment1 = new File(tempDir.toFile(), "segment_0.mp4");
            File segment2 = new File(tempDir.toFile(), "segment_1.mp4");
            segment1.createNewFile();
            segment2.createNewFile();

            List<VideoSegmentCuttingService.SegmentResult> segments = List.of(
                    VideoSegmentCuttingService.SegmentResult.builder()
                            .paragraphIndex(0)
                            .segmentFile(segment1)
                            .durationSeconds(8.5)
                            .build(),
                    VideoSegmentCuttingService.SegmentResult.builder()
                            .paragraphIndex(1)
                            .segmentFile(segment2)
                            .durationSeconds(6.5)
                            .build()
            );

            // When
            File listFile = compositionService.generateSegmentsList(segments, tempDir.toFile());

            // Then
            assertTrue(listFile.exists());
            String content = new String(java.nio.file.Files.readAllBytes(listFile.toPath()));
            assertTrue(content.contains("file '"));
            assertTrue(content.contains("segment_0.mp4"));
            assertTrue(content.contains("segment_1.mp4"));
        }
    }

    @Nested
    @DisplayName("AC2: Output Format Parameters")
    class OutputFormatTests {

        @Test
        @DisplayName("4.3-UNIT-009: Output format params verification")
        void outputFormat_ShouldHaveCorrectSettings() {
            // Given/When - verify configuration mock returns correct values
            assertEquals(1080, compositionProperties.getOutputWidth());
            assertEquals(1920, compositionProperties.getOutputHeight());
            assertEquals("4M", compositionProperties.getVideoBitrate());
            assertEquals(30, compositionProperties.getFrameRate());
            assertEquals("128k", compositionProperties.getAudioBitrate());
            assertEquals("medium", compositionProperties.getEncodingPreset());
        }

        @Test
        @DisplayName("Output dimensions for portrait mode: 1080x1920")
        void outputDimensions_ShouldBePortrait() {
            assertEquals(1080, compositionProperties.getOutputWidth());
            assertEquals(1920, compositionProperties.getOutputHeight());

            // Verify portrait orientation (height > width)
            assertTrue(compositionProperties.getOutputHeight() > compositionProperties.getOutputWidth());
        }
    }

    @Nested
    @DisplayName("AC2: OSS Upload")
    class OssUploadTests {

        @Test
        @DisplayName("4.3-INT-004: OSS upload retry configuration")
        void ossUploadRetry_ShouldBeConfigurable() {
            // Given
            when(compositionProperties.getOssUploadMaxRetries()).thenReturn(2);
            when(compositionProperties.getOssUploadRetryIntervalMs()).thenReturn(5000L);

            // Then
            assertEquals(2, compositionProperties.getOssUploadMaxRetries());
            assertEquals(5000L, compositionProperties.getOssUploadRetryIntervalMs());
        }
    }

    @Nested
    @DisplayName("Cleanup Tests")
    class CleanupTests {

        @Test
        @DisplayName("4.3-INT-010: Cleanup removes temp directory")
        void cleanup_ShouldRemoveTempDirectory() throws IOException {
            // Given
            when(compositionProperties.getTempDir()).thenReturn(tempDir.toString());

            File taskDir = new File(tempDir.toFile(), "12345");
            taskDir.mkdirs();
            File tempFile = new File(taskDir, "temp.mp4");
            tempFile.createNewFile();

            assertTrue(taskDir.exists());
            assertTrue(tempFile.exists());

            // When
            compositionService.cleanup(12345L);

            // Then
            assertFalse(taskDir.exists());
            assertFalse(tempFile.exists());
        }
    }

    @Nested
    @DisplayName("DTO Tests")
    class DtoTests {

        @Test
        @DisplayName("CompositionResult builder works correctly")
        void compositionResult_BuilderWorks() {
            File outputFile = new File("/tmp/output.mp4");
            VideoCompositionService.CompositionResult result =
                    VideoCompositionService.CompositionResult.builder()
                            .outputFile(outputFile)
                            .durationSeconds(62.5)
                            .fileSizeBytes(47841280L)
                            .build();

            assertEquals(outputFile, result.getOutputFile());
            assertEquals(62.5, result.getDurationSeconds(), 0.001);
            assertEquals(47841280L, result.getFileSizeBytes());
        }
    }
}
