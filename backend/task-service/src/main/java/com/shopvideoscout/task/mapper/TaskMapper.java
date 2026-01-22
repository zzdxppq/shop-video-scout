package com.shopvideoscout.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopvideoscout.task.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for Task entity.
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    /**
     * Count in-progress tasks for a user (BR-2.2: max 5).
     *
     * @param userId user ID
     * @param statuses list of in-progress status values
     * @return count of in-progress tasks
     */
    @Select({
        "<script>",
        "SELECT COUNT(*) FROM tasks WHERE user_id = #{userId}",
        "AND status IN",
        "<foreach item='status' collection='statuses' open='(' separator=',' close=')'>",
        "#{status}",
        "</foreach>",
        "</script>"
    })
    int countInProgressTasks(@Param("userId") Long userId, @Param("statuses") String[] statuses);

    /**
     * Find in-progress tasks for a user.
     *
     * @param userId user ID
     * @param statuses list of in-progress status values
     * @return list of in-progress tasks
     */
    @Select({
        "<script>",
        "SELECT * FROM tasks WHERE user_id = #{userId}",
        "AND status IN",
        "<foreach item='status' collection='statuses' open='(' separator=',' close=')'>",
        "#{status}",
        "</foreach>",
        "ORDER BY created_at DESC",
        "</script>"
    })
    List<Task> findInProgressTasks(@Param("userId") Long userId, @Param("statuses") String[] statuses);
}
