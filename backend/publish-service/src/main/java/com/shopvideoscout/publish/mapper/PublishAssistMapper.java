package com.shopvideoscout.publish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopvideoscout.publish.entity.PublishAssist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis mapper for publish_assists table.
 * Story 5.3: 发布辅助服务
 */
@Mapper
public interface PublishAssistMapper extends BaseMapper<PublishAssist> {

    /**
     * Find by task ID.
     */
    @Select("SELECT * FROM publish_assists WHERE task_id = #{taskId}")
    PublishAssist selectByTaskId(@Param("taskId") Long taskId);

    /**
     * Increment regenerate count.
     */
    @Update("UPDATE publish_assists SET regenerate_count = regenerate_count + 1 WHERE task_id = #{taskId}")
    int incrementRegenerateCount(@Param("taskId") Long taskId);
}
