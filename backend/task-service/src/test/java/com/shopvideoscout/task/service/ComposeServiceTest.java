package com.shopvideoscout.task.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.mq.ComposeMessage;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.task.constant.TaskConstants;
import com.shopvideoscout.task.entity.Task;
import com.shopvideoscout.task.mapper.ScriptMapper;
import com.shopvideoscout.task.mapper.TaskMapper;
import com.shopvideoscout.task.mq.ComposeMessagePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ComposeService.
 */
@ExtendWith(MockitoExtension.class)
class ComposeServiceTest {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private ScriptMapper scriptMapper;

    @Mock
    private ComposeMessagePublisher composeMessagePublisher;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ComposeService composeService;

    private Task mockTask;

    private static final String VALID_SCRIPT_CONTENT = """
            {
                "paragraphs": [
                    {"id": "para_1", "section": "开场", "text": "家人们！今天给你们探一家超好吃的店", "estimated_duration": 10},
                    {"id": "para_2", "section": "环境", "text": "这家店的装修非常有特色，ins风", "estimated_duration": 8},
                    {"id": "para_3", "section": "推荐", "text": "他们家的招牌菜必须是这个红烧肉", "estimated_duration": 12}
                ],
                "total_duration": 30
            }
            """;

    @BeforeEach
    void setUp() {
        mockTask = new Task();
        mockTask.setId(1L);
        mockTask.setUserId(100L);
        mockTask.setStatus(TaskConstants.TaskStatus.SCRIPT_EDITED);
        mockTask.setVoiceType("xiaomei");
    }

    @Nested
    @DisplayName("AC1: Trigger Compose")
    class TriggerComposeTests {

        @Test
        @DisplayName("4.1-UNIT-006: Status guard - validate task status = script_edited")
        void validStatusScriptEdited_ShouldPublishMessage() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(scriptMapper.findContentByTaskId(1L)).thenReturn(VALID_SCRIPT_CONTENT);

            // When
            var response = composeService.triggerCompose(1L, 100L);

            // Then
            assertEquals(TaskConstants.TaskStatus.COMPOSING, response.getStatus());
            assertEquals(1L, response.getTaskId());
            verify(composeMessagePublisher).publish(any(ComposeMessage.class));
            verify(taskMapper).updateById(argThat(t ->
                    TaskConstants.TaskStatus.COMPOSING.equals(t.getStatus())));
        }

        @Test
        @DisplayName("4.1-UNIT-006b: Status guard - accept voice_set status")
        void validStatusVoiceSet_ShouldPublishMessage() {
            // Given
            mockTask.setStatus(TaskConstants.TaskStatus.VOICE_SET);
            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(scriptMapper.findContentByTaskId(1L)).thenReturn(VALID_SCRIPT_CONTENT);

            // When
            var response = composeService.triggerCompose(1L, 100L);

            // Then
            assertEquals(TaskConstants.TaskStatus.COMPOSING, response.getStatus());
            verify(composeMessagePublisher).publish(any(ComposeMessage.class));
        }

        @Test
        @DisplayName("4.1-BLIND-FLOW-001: Invalid status (draft) should return 400")
        void invalidStatusDraft_ShouldThrowException() {
            // Given
            mockTask.setStatus(TaskConstants.TaskStatus.CREATED);
            when(taskMapper.selectById(1L)).thenReturn(mockTask);

            // When/Then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> composeService.triggerCompose(1L, 100L));
            assertEquals(ResultCode.TASK_STATUS_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.1-BLIND-CONCURRENCY-001: Already composing should return 409")
        void alreadyComposing_ShouldReturn409() {
            // Given
            mockTask.setStatus(TaskConstants.TaskStatus.COMPOSING);
            when(taskMapper.selectById(1L)).thenReturn(mockTask);

            // When/Then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> composeService.triggerCompose(1L, 100L));
            assertEquals(ResultCode.TASK_ALREADY_COMPOSING.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.1-UNIT-007: ComposeMessage payload structure correctness")
        void composeMessage_ShouldHaveCorrectPayload() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(scriptMapper.findContentByTaskId(1L)).thenReturn(VALID_SCRIPT_CONTENT);

            // When
            composeService.triggerCompose(1L, 100L);

            // Then
            ArgumentCaptor<ComposeMessage> captor = ArgumentCaptor.forClass(ComposeMessage.class);
            verify(composeMessagePublisher).publish(captor.capture());

            ComposeMessage msg = captor.getValue();
            assertEquals(1L, msg.getTaskId());
            assertEquals(3, msg.getParagraphs().size());
            assertEquals("xiaomei", msg.getVoiceConfig().getVoiceId());
            assertEquals("standard", msg.getVoiceConfig().getType());
            assertEquals(0, msg.getParagraphs().get(0).getIndex());
        }

        @Test
        @DisplayName("4.1-BLIND-BOUNDARY-001: Null script should return 400")
        void nullScript_ShouldThrowException() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(scriptMapper.findContentByTaskId(1L)).thenReturn(null);

            // When/Then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> composeService.triggerCompose(1L, 100L));
            assertEquals(ResultCode.SCRIPT_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.1-BLIND-BOUNDARY-002: Script with zero paragraphs should return 400")
        void zeroParagraphs_ShouldThrowException() {
            // Given
            String emptyScript = """
                    {"paragraphs": [], "total_duration": 0}
                    """;
            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(scriptMapper.findContentByTaskId(1L)).thenReturn(emptyScript);

            // When/Then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> composeService.triggerCompose(1L, 100L));
            assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.1-BLIND-BOUNDARY-006: Non-existent task should return 404")
        void taskNotFound_ShouldThrow404() {
            // Given
            when(taskMapper.selectById(999L)).thenReturn(null);

            // When/Then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> composeService.triggerCompose(999L, 100L));
            assertEquals(ResultCode.TASK_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.1-BLIND-DATA-001: Status transition should be atomic")
        void statusTransition_ShouldBeAtomicWithMqPublish() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(mockTask);
            when(scriptMapper.findContentByTaskId(1L)).thenReturn(VALID_SCRIPT_CONTENT);

            // When
            composeService.triggerCompose(1L, 100L);

            // Then - DB update happens before MQ publish (in @Transactional)
            var inOrder = inOrder(taskMapper, composeMessagePublisher);
            inOrder.verify(taskMapper).updateById(any(Task.class));
            inOrder.verify(composeMessagePublisher).publish(any(ComposeMessage.class));
        }
    }

    @Nested
    @DisplayName("AC1: Voice Type Update")
    class VoiceTypeTests {

        @Test
        @DisplayName("4.1-UNIT-008: Accept valid voice type (xiaomei)")
        void validVoiceType_ShouldUpdate() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(mockTask);

            // When
            composeService.updateVoiceType(1L, 100L, "xiaomei", null);

            // Then
            verify(taskMapper).updateById(argThat(t -> {
                assertEquals("xiaomei", t.getVoiceType());
                assertEquals(TaskConstants.TaskStatus.VOICE_SET, t.getStatus());
                return true;
            }));
        }

        @Test
        @DisplayName("4.1-UNIT-008b: Accept valid voice type (yangguang)")
        void validVoiceTypeYangguang_ShouldUpdate() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(mockTask);

            // When
            composeService.updateVoiceType(1L, 100L, "yangguang", null);

            // Then
            verify(taskMapper).updateById(argThat(t ->
                    "yangguang".equals(t.getVoiceType())));
        }

        @Test
        @DisplayName("4.1-UNIT-009: Reject unknown voice type → 400 error")
        void invalidVoiceType_ShouldThrow400() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(mockTask);

            // When/Then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> composeService.updateVoiceType(1L, 100L, "unknown_voice", null));
            assertEquals(ResultCode.INVALID_VOICE_TYPE.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("4.1-INT-002: Voice type persistence in DB")
        void voiceType_ShouldPersistInDb() {
            // Given
            when(taskMapper.selectById(1L)).thenReturn(mockTask);

            // When
            composeService.updateVoiceType(1L, 100L, "zhixing", 42L);

            // Then
            verify(taskMapper).updateById(argThat(t -> {
                assertEquals("zhixing", t.getVoiceType());
                assertEquals(42L, t.getVoiceSampleId());
                return true;
            }));
        }

        @Test
        @DisplayName("Status transition: script_edited → voice_set on voice update")
        void statusTransition_ScriptEditedToVoiceSet() {
            // Given
            mockTask.setStatus(TaskConstants.TaskStatus.SCRIPT_EDITED);
            when(taskMapper.selectById(1L)).thenReturn(mockTask);

            // When
            composeService.updateVoiceType(1L, 100L, "xiaomei", null);

            // Then
            verify(taskMapper).updateById(argThat(t ->
                    TaskConstants.TaskStatus.VOICE_SET.equals(t.getStatus())));
        }
    }

    @Nested
    @DisplayName("Paragraph Parsing")
    class ParagraphParsingTests {

        @Test
        @DisplayName("Parse valid script with 3 paragraphs")
        void validScript_ShouldReturn3Paragraphs() {
            // When
            var paragraphs = composeService.parseParagraphs(VALID_SCRIPT_CONTENT);

            // Then
            assertEquals(3, paragraphs.size());
            assertEquals(0, paragraphs.get(0).getIndex());
            assertTrue(paragraphs.get(0).getText().contains("家人们"));
        }

        @Test
        @DisplayName("Parse script with empty text paragraphs - skip empty")
        void emptyTextParagraphs_ShouldBeSkipped() {
            // Given
            String scriptWithEmpty = """
                    {"paragraphs": [
                        {"id": "p1", "text": "有内容"},
                        {"id": "p2", "text": ""},
                        {"id": "p3", "text": "也有内容"}
                    ]}
                    """;

            // When
            var paragraphs = composeService.parseParagraphs(scriptWithEmpty);

            // Then
            assertEquals(2, paragraphs.size());
        }

        @Test
        @DisplayName("Parse malformed JSON should throw")
        void malformedJson_ShouldThrow() {
            // When/Then
            assertThrows(BusinessException.class,
                    () -> composeService.parseParagraphs("{invalid json"));
        }
    }
}
