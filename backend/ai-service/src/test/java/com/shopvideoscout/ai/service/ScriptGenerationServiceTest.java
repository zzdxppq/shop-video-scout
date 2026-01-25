package com.shopvideoscout.ai.service;

import com.shopvideoscout.ai.client.DoubaoClient;
import com.shopvideoscout.ai.dto.ScriptContent;
import com.shopvideoscout.ai.entity.Script;
import com.shopvideoscout.ai.entity.VideoFrame;
import com.shopvideoscout.ai.mapper.ScriptMapper;
import com.shopvideoscout.ai.mapper.VideoFrameMapper;
import com.shopvideoscout.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ScriptGenerationService.
 * Story 3.1 - AC1 & AC2: Script generation and regeneration.
 */
@ExtendWith(MockitoExtension.class)
class ScriptGenerationServiceTest {

    @Mock
    private DoubaoClient doubaoClient;

    @Mock
    private ScriptPromptBuilder promptBuilder;

    @Mock
    private ScriptResponseParser responseParser;

    @Mock
    private ScriptMapper scriptMapper;

    @Mock
    private VideoFrameMapper videoFrameMapper;

    @InjectMocks
    private ScriptGenerationService scriptGenerationService;

    private ScriptContent validScriptContent;
    private List<VideoFrame> sampleFrames;

    @BeforeEach
    void setUp() {
        // Setup valid script content
        validScriptContent = ScriptContent.builder()
                .paragraphs(Arrays.asList(
                        ScriptContent.ScriptParagraph.builder()
                                .id("para_1").section("开场").shotId(101L).text("Hello").estimatedDuration(10).build(),
                        ScriptContent.ScriptParagraph.builder()
                                .id("para_2").section("内容").shotId(102L).text("World").estimatedDuration(10).build(),
                        ScriptContent.ScriptParagraph.builder()
                                .id("para_3").section("内容2").shotId(103L).text("Test").estimatedDuration(10).build(),
                        ScriptContent.ScriptParagraph.builder()
                                .id("para_4").section("内容3").shotId(104L).text("Test2").estimatedDuration(10).build(),
                        ScriptContent.ScriptParagraph.builder()
                                .id("para_5").section("结尾").shotId(105L).text("Bye").estimatedDuration(20).build()
                ))
                .totalDuration(60)
                .build();

        // Setup sample video frames
        VideoFrame frame1 = new VideoFrame();
        frame1.setId(101L);
        frame1.setCategory("food");
        frame1.setTags(Arrays.asList("美食", "色彩丰富"));
        frame1.setQualityScore(85);
        frame1.setIsRecommended(true);

        VideoFrame frame2 = new VideoFrame();
        frame2.setId(102L);
        frame2.setCategory("environment");
        frame2.setTags(Arrays.asList("室内", "明亮"));
        frame2.setQualityScore(78);
        frame2.setIsRecommended(false);

        sampleFrames = Arrays.asList(frame1, frame2);
    }

    // 3.1-UNIT-008: Regenerate count check (count < 5 allows)
    @Test
    @DisplayName("Should allow regeneration when count < 5")
    void canRegenerate_countLessThan5_shouldReturnTrue() {
        assertThat(scriptGenerationService.canRegenerate(0)).isTrue();
        assertThat(scriptGenerationService.canRegenerate(1)).isTrue();
        assertThat(scriptGenerationService.canRegenerate(4)).isTrue();
    }

    // 3.1-UNIT-009: Regenerate count check (count >= 5 rejects)
    @Test
    @DisplayName("Should reject regeneration when count >= 5")
    void canRegenerate_countGreaterOrEqual5_shouldReturnFalse() {
        assertThat(scriptGenerationService.canRegenerate(5)).isFalse();
        assertThat(scriptGenerationService.canRegenerate(6)).isFalse();
        assertThat(scriptGenerationService.canRegenerate(100)).isFalse();
    }

    // 3.1-BLIND-BOUNDARY-003: Regenerate count exactly at limit
    @Test
    @DisplayName("Should allow regeneration at count=4, reject at count=5")
    void canRegenerate_atBoundary_shouldBehaveCorrectly() {
        assertThat(scriptGenerationService.canRegenerate(4)).isTrue();
        assertThat(scriptGenerationService.canRegenerate(5)).isFalse();
    }

    @Test
    @DisplayName("Should calculate remaining regenerations correctly")
    void getRemainingRegenerations_shouldCalculateCorrectly() {
        assertThat(scriptGenerationService.getRemainingRegenerations(0)).isEqualTo(5);
        assertThat(scriptGenerationService.getRemainingRegenerations(3)).isEqualTo(2);
        assertThat(scriptGenerationService.getRemainingRegenerations(5)).isEqualTo(0);
        assertThat(scriptGenerationService.getRemainingRegenerations(10)).isEqualTo(0);
    }

    // 3.1-INT-001: Generate script successfully → insert to scripts table
    @Test
    @DisplayName("Should generate script and insert to database")
    void generateScript_success_shouldInsertScript() {
        // Setup mocks
        when(videoFrameMapper.findAnalyzedByTaskId(1L)).thenReturn(sampleFrames);
        when(promptBuilder.buildPrompt(anyString(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn("test prompt");
        when(doubaoClient.calculateTemperature(0)).thenReturn(0.7);
        when(doubaoClient.generateScript(anyString(), anyDouble())).thenReturn("json response");
        when(responseParser.parse(anyString())).thenReturn(validScriptContent);
        when(scriptMapper.findByTaskId(1L)).thenReturn(null);
        when(scriptMapper.insert(any(Script.class))).thenReturn(1);

        Script result = scriptGenerationService.generateScript(
                1L, "店铺名", "food", "优惠", "recommend", 0
        );

        assertThat(result).isNotNull();
        assertThat(result.getTaskId()).isEqualTo(1L);
        assertThat(result.getVersion()).isEqualTo(1);
        verify(scriptMapper).insert(any(Script.class));
    }

    // 3.1-INT-008: POST regenerate → new version, count++
    @Test
    @DisplayName("Should regenerate script and update version")
    void regenerateScript_existingScript_shouldUpdateVersion() {
        // Setup existing script
        Script existingScript = Script.createNew(1L, validScriptContent);
        existingScript.setId(100L);

        when(videoFrameMapper.findAnalyzedByTaskId(1L)).thenReturn(sampleFrames);
        when(promptBuilder.buildPrompt(anyString(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn("test prompt");
        when(doubaoClient.calculateTemperature(1)).thenReturn(0.8);
        when(doubaoClient.generateScript(anyString(), anyDouble())).thenReturn("json response");
        when(responseParser.parse(anyString())).thenReturn(validScriptContent);
        when(scriptMapper.findByTaskId(1L)).thenReturn(existingScript);
        when(scriptMapper.updateById(any(Script.class))).thenReturn(1);

        Script result = scriptGenerationService.regenerateScript(
                1L, "店铺名", "food", "优惠", "recommend", 1
        );

        assertThat(result.getVersion()).isEqualTo(2);
        verify(scriptMapper).updateById(any(Script.class));
        verify(scriptMapper, never()).insert(any(Script.class));
    }

    // 3.1-INT-011: Count=5 → return 429
    @Test
    @DisplayName("Should throw 429 when regeneration limit reached")
    void regenerateScript_limitReached_shouldThrow429() {
        assertThatThrownBy(() ->
                scriptGenerationService.regenerateScript(
                        1L, "店铺名", "food", "优惠", "recommend", 5
                ))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getCode()).isEqualTo(429);
                    assertThat(be.getMessage()).contains("重新生成次数已达上限");
                });
    }

    // 3.1-BLIND-BOUNDARY-002: Empty shot_summaries (no analyzed videos)
    @Test
    @DisplayName("Should throw exception when no analyzed frames")
    void generateScript_noFrames_shouldThrowException() {
        when(videoFrameMapper.findAnalyzedByTaskId(1L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() ->
                scriptGenerationService.generateScript(
                        1L, "店铺名", "food", "优惠", "recommend", 0
                ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无可用镜头");
    }

    @Test
    @DisplayName("Should return script by task ID")
    void getByTaskId_shouldReturnScript() {
        Script script = Script.createNew(1L, validScriptContent);
        when(scriptMapper.findByTaskId(1L)).thenReturn(script);

        Script result = scriptGenerationService.getByTaskId(1L);

        assertThat(result).isNotNull();
        assertThat(result.getTaskId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should return null when script not found")
    void getByTaskId_notFound_shouldReturnNull() {
        when(scriptMapper.findByTaskId(999L)).thenReturn(null);

        Script result = scriptGenerationService.getByTaskId(999L);

        assertThat(result).isNull();
    }
}
