package com.shopvideoscout.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.shopvideoscout.ai.dto.ScriptContent;
import com.shopvideoscout.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Script entity mapping to 'scripts' table.
 * Stores AI-generated scripts for tasks.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "scripts", autoResultMap = true)
public class Script extends BaseEntity {

    /**
     * Associated task ID (unique constraint).
     */
    private Long taskId;

    /**
     * Script content as JSON (paragraphs array with metadata).
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private ScriptContent content;

    /**
     * Script version (increments on regeneration).
     */
    private Integer version;

    /**
     * Whether user has manually edited this script.
     */
    private Boolean isUserEdited;

    /**
     * Total script duration in seconds.
     */
    private Integer totalDurationSeconds;

    /**
     * Create a new script for a task.
     */
    public static Script createNew(Long taskId, ScriptContent content) {
        Script script = new Script();
        script.setTaskId(taskId);
        script.setContent(content);
        script.setVersion(1);
        script.setIsUserEdited(false);
        script.setTotalDurationSeconds(content.getTotalDuration());
        return script;
    }

    /**
     * Update content for regeneration (increment version).
     */
    public void regenerate(ScriptContent newContent) {
        this.content = newContent;
        this.version = this.version + 1;
        this.isUserEdited = false;
        this.totalDurationSeconds = newContent.getTotalDuration();
    }
}
