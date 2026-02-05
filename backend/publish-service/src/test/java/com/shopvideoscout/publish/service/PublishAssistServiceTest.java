package com.shopvideoscout.publish.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.publish.client.TaskServiceClient;
import com.shopvideoscout.publish.dto.PublishAssistResponse;
import com.shopvideoscout.publish.dto.ScriptDto;
import com.shopvideoscout.publish.dto.TaskDetailDto;
import com.shopvideoscout.publish.entity.PublishAssist;
import com.shopvideoscout.publish.mapper.PublishAssistMapper;
import com.shopvideoscout.publish.service.impl.PublishAssistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PublishAssistService.
 * Story 5.3: 发布辅助服务 - AC1, AC2, AC3
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PublishAssistService Tests")
class PublishAssistServiceTest {

    @Mock
    private TaskServiceClient taskServiceClient;

    @Mock
    private TopicGenerationService topicGenerationService;

    @Mock
    private TitleGenerationService titleGenerationService;

    @Mock
    private PublishAssistMapper publishAssistMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Captor
    private ArgumentCaptor<String> cacheKeyCaptor;

    @Captor
    private ArgumentCaptor<String> cacheValueCaptor;

    private PublishAssistServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new PublishAssistServiceImpl(
                taskServiceClient,
                topicGenerationService,
                titleGenerationService,
                publishAssistMapper,
                redisTemplate,
                objectMapper
        );

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("getPublishAssist")
    class GetPublishAssist {

        private final Long taskId = 1L;
        private final Long userId = 100L;

        @Test
        @DisplayName("should return cached response when available")
        void shouldReturnCachedResponse() throws Exception {
            // Given
            mockValidTask();
            String cachedJson = """
                {"topics":["#缓存话题"],"titles":["缓存的标题测试至少二十个字符"],"regenerateRemaining":3}
                """;
            when(valueOperations.get("publish:assist:" + taskId)).thenReturn(cachedJson);

            // When
            PublishAssistResponse response = service.getPublishAssist(taskId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTopics()).contains("#缓存话题");
            verify(publishAssistMapper, never()).selectByTaskId(any());
        }

        @Test
        @DisplayName("should return DB record and cache when not in Redis")
        void shouldReturnDbRecordAndCache() {
            // Given
            mockValidTask();
            when(valueOperations.get(anyString())).thenReturn(null);

            PublishAssist entity = PublishAssist.createNew(
                    taskId,
                    List.of("#数据库话题"),
                    List.of("数据库标题测试需要至少二十个字符")
            );
            when(publishAssistMapper.selectByTaskId(taskId)).thenReturn(entity);

            // When
            PublishAssistResponse response = service.getPublishAssist(taskId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTopics()).contains("#数据库话题");
            verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("should generate new content when not cached or in DB")
        void shouldGenerateNewContent() {
            // Given
            mockValidTask();
            when(valueOperations.get(anyString())).thenReturn(null);
            when(publishAssistMapper.selectByTaskId(taskId)).thenReturn(null);

            ScriptDto script = new ScriptDto();
            script.setSummary("脚本摘要");
            when(taskServiceClient.getScript(taskId)).thenReturn(script);

            List<String> topics = List.of("#生成话题");
            List<String> titles = List.of("生成的标题测试需要至少二十个字符");
            when(topicGenerationService.generateTopics(any(), any(), any(), any())).thenReturn(topics);
            when(titleGenerationService.generateTitles(any(), any(), any(), any())).thenReturn(titles);

            // When
            PublishAssistResponse response = service.getPublishAssist(taskId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTopics()).contains("#生成话题");
            verify(publishAssistMapper).insert(any(PublishAssist.class));
        }

        @Test
        @DisplayName("should throw TASK_NOT_FOUND when task does not exist")
        void shouldThrowWhenTaskNotFound() {
            // Given
            when(taskServiceClient.getTask(taskId)).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> service.getPublishAssist(taskId, userId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getCode()).isEqualTo(ResultCode.TASK_NOT_FOUND.getCode());
                    });
        }

        @Test
        @DisplayName("should throw FORBIDDEN when user is not task owner")
        void shouldThrowForbiddenWhenNotOwner() {
            // Given
            TaskDetailDto task = new TaskDetailDto();
            task.setUserId(999L); // Different user
            task.setStatus("done");
            when(taskServiceClient.getTask(taskId)).thenReturn(task);

            // When/Then
            assertThatThrownBy(() -> service.getPublishAssist(taskId, userId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getCode()).isEqualTo(ResultCode.FORBIDDEN.getCode());
                    });
        }

        @Test
        @DisplayName("should throw OUTPUT_NOT_READY when task is not done")
        void shouldThrowWhenTaskNotDone() {
            // Given
            TaskDetailDto task = new TaskDetailDto();
            task.setUserId(userId);
            task.setStatus("processing");
            when(taskServiceClient.getTask(taskId)).thenReturn(task);

            // When/Then
            assertThatThrownBy(() -> service.getPublishAssist(taskId, userId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getCode()).isEqualTo(ResultCode.OUTPUT_NOT_READY.getCode());
                    });
        }

        private void mockValidTask() {
            TaskDetailDto task = new TaskDetailDto();
            task.setId(taskId);
            task.setUserId(userId);
            task.setShopName("测试店铺");
            task.setShopType("美食");
            task.setStatus("done");
            when(taskServiceClient.getTask(taskId)).thenReturn(task);
        }
    }

    @Nested
    @DisplayName("regenerate")
    class Regenerate {

        private final Long taskId = 1L;
        private final Long userId = 100L;

        @Test
        @DisplayName("should regenerate with higher temperature")
        void shouldRegenerateWithHigherTemperature() {
            // Given
            mockValidTask();

            PublishAssist entity = PublishAssist.createNew(taskId, List.of(), List.of());
            entity.setRegenerateCount(1);
            when(publishAssistMapper.selectByTaskId(taskId)).thenReturn(entity);

            ScriptDto script = new ScriptDto();
            script.setSummary("摘要");
            when(taskServiceClient.getScript(taskId)).thenReturn(script);

            List<String> newTopics = List.of("#新话题");
            List<String> newTitles = List.of("新标题测试需要至少二十个字符通过验证");
            when(topicGenerationService.generateTopics(any(), any(), any(), eq(0.8))).thenReturn(newTopics);
            when(titleGenerationService.generateTitles(any(), any(), any(), eq(0.8))).thenReturn(newTitles);

            // When
            PublishAssistResponse response = service.regenerate(taskId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTopics()).contains("#新话题");
            verify(redisTemplate).delete("publish:assist:" + taskId);
            verify(publishAssistMapper).incrementRegenerateCount(taskId);
        }

        @Test
        @DisplayName("should throw PUBLISH_ASSIST_LIMIT_EXCEEDED when regenerate limit reached")
        void shouldThrowWhenRegenerateLimitReached() {
            // Given
            mockValidTask();

            PublishAssist entity = PublishAssist.createNew(taskId, List.of(), List.of());
            entity.setRegenerateCount(3); // Already at limit
            when(publishAssistMapper.selectByTaskId(taskId)).thenReturn(entity);

            // When/Then
            assertThatThrownBy(() -> service.regenerate(taskId, userId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getCode()).isEqualTo(ResultCode.PUBLISH_ASSIST_LIMIT_EXCEEDED.getCode());
                    });
        }

        @Test
        @DisplayName("should allow regenerate when no existing record")
        void shouldAllowRegenerateWhenNoExistingRecord() {
            // Given
            mockValidTask();
            when(publishAssistMapper.selectByTaskId(taskId)).thenReturn(null);

            ScriptDto script = new ScriptDto();
            script.setSummary("摘要");
            when(taskServiceClient.getScript(taskId)).thenReturn(script);

            List<String> topics = List.of("#话题");
            List<String> titles = List.of("标题测试需要至少二十个字符");
            when(topicGenerationService.generateTopics(any(), any(), any(), any())).thenReturn(topics);
            when(titleGenerationService.generateTitles(any(), any(), any(), any())).thenReturn(titles);

            // When
            PublishAssistResponse response = service.regenerate(taskId, userId);

            // Then
            assertThat(response).isNotNull();
            verify(publishAssistMapper).insert(any(PublishAssist.class));
        }

        private void mockValidTask() {
            TaskDetailDto task = new TaskDetailDto();
            task.setId(taskId);
            task.setUserId(userId);
            task.setShopName("测试店铺");
            task.setShopType("美食");
            task.setStatus("done");
            when(taskServiceClient.getTask(taskId)).thenReturn(task);
        }
    }

    @Nested
    @DisplayName("Cache behavior")
    class CacheBehavior {

        @Test
        @DisplayName("should cache response with 24h TTL")
        void shouldCacheResponseWith24hTtl() {
            // Given
            Long taskId = 1L;
            Long userId = 100L;

            TaskDetailDto task = new TaskDetailDto();
            task.setId(taskId);
            task.setUserId(userId);
            task.setShopName("店铺");
            task.setShopType("美食");
            task.setStatus("done");
            when(taskServiceClient.getTask(taskId)).thenReturn(task);

            when(valueOperations.get(anyString())).thenReturn(null);
            when(publishAssistMapper.selectByTaskId(taskId)).thenReturn(null);

            ScriptDto script = new ScriptDto();
            when(taskServiceClient.getScript(taskId)).thenReturn(script);

            List<String> topics = List.of("#话题");
            List<String> titles = List.of("标题测试需要至少二十个字符");
            when(topicGenerationService.generateTopics(any(), any(), any(), any())).thenReturn(topics);
            when(titleGenerationService.generateTitles(any(), any(), any(), any())).thenReturn(titles);

            // When
            service.getPublishAssist(taskId, userId);

            // Then
            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
            verify(valueOperations).set(
                    eq("publish:assist:" + taskId),
                    anyString(),
                    ttlCaptor.capture()
            );
            assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofHours(24));
        }
    }

    @Nested
    @DisplayName("Response building")
    class ResponseBuilding {

        @Test
        @DisplayName("should calculate remaining regenerations correctly")
        void shouldCalculateRemainingRegenerations() {
            // Given
            Long taskId = 1L;
            Long userId = 100L;

            TaskDetailDto task = new TaskDetailDto();
            task.setId(taskId);
            task.setUserId(userId);
            task.setShopName("店铺");
            task.setShopType("美食");
            task.setStatus("done");
            when(taskServiceClient.getTask(taskId)).thenReturn(task);

            when(valueOperations.get(anyString())).thenReturn(null);

            PublishAssist entity = PublishAssist.createNew(taskId, List.of("#话题"), List.of("标题测试"));
            entity.setRegenerateCount(2);
            when(publishAssistMapper.selectByTaskId(taskId)).thenReturn(entity);

            // When
            PublishAssistResponse response = service.getPublishAssist(taskId, userId);

            // Then
            assertThat(response.getRegenerateRemaining()).isEqualTo(1);
        }
    }
}
