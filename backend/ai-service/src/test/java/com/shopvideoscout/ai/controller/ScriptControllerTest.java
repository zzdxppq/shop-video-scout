package com.shopvideoscout.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.ai.client.TaskServiceClient;
import com.shopvideoscout.ai.dto.ScriptContent;
import com.shopvideoscout.ai.entity.Script;
import com.shopvideoscout.ai.service.ScriptGenerationService;
import com.shopvideoscout.common.result.R;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for ScriptController.
 * Story 3.1 - T3: API endpoint tests.
 */
@WebMvcTest(ScriptController.class)
class ScriptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScriptGenerationService scriptGenerationService;

    @MockBean
    private TaskServiceClient taskServiceClient;

    private TaskServiceClient.TaskInfo taskInfo;
    private Script script;
    private ScriptContent scriptContent;

    @BeforeEach
    void setUp() {
        // Setup task info
        taskInfo = new TaskServiceClient.TaskInfo();
        taskInfo.setId(1L);
        taskInfo.setShopName("测试店铺");
        taskInfo.setShopType("food");
        taskInfo.setPromotionText("满100减20");
        taskInfo.setVideoStyle("recommend");
        taskInfo.setScriptRegenerateCount(0);

        // Setup script content
        scriptContent = ScriptContent.builder()
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

        // Setup script
        script = Script.createNew(1L, scriptContent);
        script.setId(100L);
    }

    // 3.1-INT-012: GET /tasks/{id}/script → return script content
    @Test
    @DisplayName("GET /tasks/{taskId}/script should return script content")
    void getScript_success_shouldReturnScript() throws Exception {
        when(taskServiceClient.getTaskInfo(1L)).thenReturn(R.ok(taskInfo));
        when(scriptGenerationService.getByTaskId(1L)).thenReturn(script);
        when(scriptGenerationService.getRemainingRegenerations(0)).thenReturn(5);

        mockMvc.perform(get("/api/v1/tasks/1/script"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.task_id").value(1))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.regenerate_remaining").value(5))
                .andExpect(jsonPath("$.data.paragraphs").isArray())
                .andExpect(jsonPath("$.data.paragraphs.length()").value(5));
    }

    // 3.1-INT-013: GET /tasks/{id}/script with invalid id → 404
    @Test
    @DisplayName("GET /tasks/{taskId}/script with invalid task should return 404")
    void getScript_taskNotFound_shouldReturn404() throws Exception {
        when(taskServiceClient.getTaskInfo(999L)).thenReturn(R.error(404, "Task not found"));

        mockMvc.perform(get("/api/v1/tasks/999/script"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002)); // TASK_NOT_FOUND
    }

    // 3.1-INT-014: GET /tasks/{id}/script before generation → 404
    @Test
    @DisplayName("GET /tasks/{taskId}/script before generation should return 404")
    void getScript_scriptNotGenerated_shouldReturn404() throws Exception {
        when(taskServiceClient.getTaskInfo(1L)).thenReturn(R.ok(taskInfo));
        when(scriptGenerationService.getByTaskId(1L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/tasks/1/script"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // 3.1-INT-008: POST regenerate → new version
    @Test
    @DisplayName("POST /tasks/{taskId}/regenerate-script should regenerate script")
    void regenerateScript_success_shouldReturnNewVersion() throws Exception {
        Script regeneratedScript = Script.createNew(1L, scriptContent);
        regeneratedScript.setId(100L);
        regeneratedScript.setVersion(2);

        when(taskServiceClient.getTaskInfo(1L)).thenReturn(R.ok(taskInfo));
        when(scriptGenerationService.canRegenerate(0)).thenReturn(true);
        when(scriptGenerationService.regenerateScript(eq(1L), anyString(), anyString(), anyString(), anyString(), eq(0)))
                .thenReturn(regeneratedScript);
        when(taskServiceClient.incrementRegenerateCount(1L)).thenReturn(R.ok());
        when(scriptGenerationService.getRemainingRegenerations(1)).thenReturn(4);

        mockMvc.perform(post("/api/v1/tasks/1/regenerate-script"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.regenerate_remaining").value(4));
    }

    // 3.1-INT-015: POST regenerate with invalid task_id → 404
    @Test
    @DisplayName("POST /tasks/{taskId}/regenerate-script with invalid task should return 404")
    void regenerateScript_taskNotFound_shouldReturn404() throws Exception {
        when(taskServiceClient.getTaskInfo(999L)).thenReturn(R.error(404, "Task not found"));

        mockMvc.perform(post("/api/v1/tasks/999/regenerate-script"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));
    }

    // 3.1-INT-011: POST regenerate 429 when count >= 5
    @Test
    @DisplayName("POST /tasks/{taskId}/regenerate-script should return 429 when limit reached")
    void regenerateScript_limitReached_shouldReturn429() throws Exception {
        taskInfo.setScriptRegenerateCount(5);
        when(taskServiceClient.getTaskInfo(1L)).thenReturn(R.ok(taskInfo));
        when(scriptGenerationService.canRegenerate(5)).thenReturn(false);

        mockMvc.perform(post("/api/v1/tasks/1/regenerate-script"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(429))
                .andExpect(jsonPath("$.message").value("重新生成次数已达上限，请手动编辑"));
    }

    // 3.1-INT-016: Response includes regenerate_remaining field
    @Test
    @DisplayName("Script response should include regenerate_remaining field")
    void getScript_shouldIncludeRegenerateRemaining() throws Exception {
        taskInfo.setScriptRegenerateCount(3);
        when(taskServiceClient.getTaskInfo(1L)).thenReturn(R.ok(taskInfo));
        when(scriptGenerationService.getByTaskId(1L)).thenReturn(script);
        when(scriptGenerationService.getRemainingRegenerations(3)).thenReturn(2);

        mockMvc.perform(get("/api/v1/tasks/1/script"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.regenerate_remaining").value(2));
    }
}
