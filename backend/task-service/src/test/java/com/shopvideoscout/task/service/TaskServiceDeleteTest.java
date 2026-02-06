package com.shopvideoscout.task.service;

import com.shopvideoscout.common.exception.BusinessException;
import com.shopvideoscout.common.result.ResultCode;
import com.shopvideoscout.task.constant.TaskConstants;
import com.shopvideoscout.task.entity.Task;
import com.shopvideoscout.task.mapper.TaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskService.deleteTask method.
 * Story 5.5: 历史任务管理
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceDeleteTest {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private OssCleanupService ossCleanupService;

    @InjectMocks
    private TaskService taskService;

    private Task testTask;
    private final Long TASK_ID = 123L;
    private final Long OWNER_USER_ID = 100L;
    private final Long OTHER_USER_ID = 999L;

    @BeforeEach
    void setUp() {
        testTask = new Task();
        testTask.setId(TASK_ID);
        testTask.setUserId(OWNER_USER_ID);
        testTask.setShopName("Test Shop");
        testTask.setStatus(TaskConstants.TaskStatus.COMPLETED);
    }

    @Nested
    @DisplayName("Delete Task - Success Cases")
    class DeleteTaskSuccessTests {

        @Test
        @DisplayName("5.5-UNIT-001: Should delete completed task successfully")
        void deleteTask_CompletedTask_ShouldSucceed() {
            // Given
            when(taskMapper.selectById(TASK_ID)).thenReturn(testTask);
            when(taskMapper.deleteById(TASK_ID)).thenReturn(1);

            // When
            assertDoesNotThrow(() -> taskService.deleteTask(TASK_ID, OWNER_USER_ID));

            // Then
            verify(taskMapper).deleteById(TASK_ID);
            verify(ossCleanupService).cleanupTaskFiles(TASK_ID, OWNER_USER_ID);
        }

        @Test
        @DisplayName("5.5-UNIT-002: Should delete failed task successfully")
        void deleteTask_FailedTask_ShouldSucceed() {
            // Given
            testTask.setStatus(TaskConstants.TaskStatus.FAILED);
            when(taskMapper.selectById(TASK_ID)).thenReturn(testTask);
            when(taskMapper.deleteById(TASK_ID)).thenReturn(1);

            // When
            assertDoesNotThrow(() -> taskService.deleteTask(TASK_ID, OWNER_USER_ID));

            // Then
            verify(taskMapper).deleteById(TASK_ID);
        }

        @Test
        @DisplayName("5.5-UNIT-003: Should delete uploading task successfully")
        void deleteTask_UploadingTask_ShouldSucceed() {
            // Given
            testTask.setStatus(TaskConstants.TaskStatus.UPLOADING);
            when(taskMapper.selectById(TASK_ID)).thenReturn(testTask);
            when(taskMapper.deleteById(TASK_ID)).thenReturn(1);

            // When
            assertDoesNotThrow(() -> taskService.deleteTask(TASK_ID, OWNER_USER_ID));

            // Then
            verify(taskMapper).deleteById(TASK_ID);
        }

        @Test
        @DisplayName("5.5-UNIT-004: Should call OSS cleanup after database delete")
        void deleteTask_ShouldCallOssCleanup() {
            // Given
            when(taskMapper.selectById(TASK_ID)).thenReturn(testTask);
            when(taskMapper.deleteById(TASK_ID)).thenReturn(1);

            // When
            taskService.deleteTask(TASK_ID, OWNER_USER_ID);

            // Then
            verify(ossCleanupService).cleanupTaskFiles(TASK_ID, OWNER_USER_ID);
        }
    }

    @Nested
    @DisplayName("Delete Task - Error Cases")
    class DeleteTaskErrorTests {

        @Test
        @DisplayName("5.5-UNIT-005: Should throw exception when task not found")
        void deleteTask_TaskNotFound_ShouldThrowException() {
            // Given
            when(taskMapper.selectById(TASK_ID)).thenReturn(null);

            // When/Then
            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taskService.deleteTask(TASK_ID, OWNER_USER_ID)
            );

            assertEquals(ResultCode.TASK_NOT_FOUND.getCode(), exception.getCode());
            verify(taskMapper, never()).deleteById(any());
            verify(ossCleanupService, never()).cleanupTaskFiles(any(), any());
        }

        @Test
        @DisplayName("5.5-UNIT-006: Should throw exception when user is not owner")
        void deleteTask_NotOwner_ShouldThrowException() {
            // Given
            when(taskMapper.selectById(TASK_ID)).thenReturn(testTask);

            // When/Then
            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taskService.deleteTask(TASK_ID, OTHER_USER_ID)
            );

            assertEquals(ResultCode.TASK_DELETE_FORBIDDEN.getCode(), exception.getCode());
            verify(taskMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("5.5-UNIT-007: Should throw exception when task is analyzing")
        void deleteTask_AnalyzingTask_ShouldThrowException() {
            // Given
            testTask.setStatus(TaskConstants.TaskStatus.ANALYZING);
            when(taskMapper.selectById(TASK_ID)).thenReturn(testTask);

            // When/Then
            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taskService.deleteTask(TASK_ID, OWNER_USER_ID)
            );

            assertEquals(ResultCode.TASK_IN_PROGRESS.getCode(), exception.getCode());
            verify(taskMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("5.5-UNIT-008: Should throw exception when task is composing")
        void deleteTask_ComposingTask_ShouldThrowException() {
            // Given
            testTask.setStatus(TaskConstants.TaskStatus.COMPOSING);
            when(taskMapper.selectById(TASK_ID)).thenReturn(testTask);

            // When/Then
            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taskService.deleteTask(TASK_ID, OWNER_USER_ID)
            );

            assertEquals(ResultCode.TASK_IN_PROGRESS.getCode(), exception.getCode());
            verify(taskMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("5.5-UNIT-009: Should throw exception when database delete fails")
        void deleteTask_DatabaseDeleteFails_ShouldThrowException() {
            // Given
            when(taskMapper.selectById(TASK_ID)).thenReturn(testTask);
            when(taskMapper.deleteById(TASK_ID)).thenReturn(0);

            // When/Then
            BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taskService.deleteTask(TASK_ID, OWNER_USER_ID)
            );

            assertEquals(ResultCode.TASK_DELETE_FAILED.getCode(), exception.getCode());
            verify(ossCleanupService, never()).cleanupTaskFiles(any(), any());
        }
    }
}
