package com.shopvideoscout.publish.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.shopvideoscout.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Entity for publish_assists table.
 * Story 5.3: 发布辅助服务
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "publish_assists", autoResultMap = true)
public class PublishAssist extends BaseEntity {

    /**
     * Associated task ID.
     */
    private Long taskId;

    /**
     * Recommended topics (JSON array).
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> topics;

    /**
     * Recommended titles (JSON array).
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> titles;

    /**
     * Number of regeneration attempts.
     */
    private Integer regenerateCount;

    /**
     * Create a new PublishAssist with default values.
     */
    public static PublishAssist createNew(Long taskId, List<String> topics, List<String> titles) {
        PublishAssist entity = new PublishAssist();
        entity.setTaskId(taskId);
        entity.setTopics(topics);
        entity.setTitles(titles);
        entity.setRegenerateCount(0);
        return entity;
    }
}
