package com.shopvideoscout.task.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopvideoscout.task.dto.internal.ScriptDto;
import com.shopvideoscout.task.dto.internal.TaskDetailDto;
import com.shopvideoscout.task.entity.Task;
import com.shopvideoscout.task.mapper.ScriptMapper;
import com.shopvideoscout.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal API controller for cross-service calls.
 * Story 5.3: 发布辅助服务
 *
 * These endpoints are for internal service-to-service communication only.
 * Not exposed through gateway.
 */
@Slf4j
@RestController
@RequestMapping("/internal/tasks")
@RequiredArgsConstructor
public class InternalTaskController {

    private final TaskMapper taskMapper;
    private final ScriptMapper scriptMapper;
    private final ObjectMapper objectMapper;

    /**
     * Get task details.
     *
     * @param taskId task ID
     * @return task details (id, shop_name, shop_type, status, user_id)
     */
    @GetMapping("/{id}")
    public TaskDetailDto getTaskDetail(@PathVariable("id") Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }

        return TaskDetailDto.builder()
                .id(task.getId())
                .shopName(task.getShopName())
                .shopType(task.getShopType())
                .status(task.getStatus())
                .userId(task.getUserId())
                .build();
    }

    /**
     * Get task script content.
     *
     * @param taskId task ID
     * @return script with paragraphs
     */
    @GetMapping("/{id}/script")
    public ScriptDto getScript(@PathVariable("id") Long taskId) {
        String content = scriptMapper.findContentByTaskId(taskId);
        if (content == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Script not found");
        }

        List<ScriptDto.Paragraph> paragraphs = parseScriptContent(content);

        return ScriptDto.builder()
                .taskId(taskId)
                .paragraphs(paragraphs)
                .build();
    }

    /**
     * Parse script content JSON to paragraphs.
     */
    private List<ScriptDto.Paragraph> parseScriptContent(String content) {
        List<ScriptDto.Paragraph> paragraphs = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode paragraphsNode = root.get("paragraphs");

            if (paragraphsNode != null && paragraphsNode.isArray()) {
                for (JsonNode node : paragraphsNode) {
                    String id = node.has("id") ? node.get("id").asText() : null;
                    String text = node.has("text") ? node.get("text").asText() : "";

                    paragraphs.add(ScriptDto.Paragraph.builder()
                            .id(id)
                            .text(text)
                            .build());
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse script content JSON", e);
        }

        return paragraphs;
    }
}
