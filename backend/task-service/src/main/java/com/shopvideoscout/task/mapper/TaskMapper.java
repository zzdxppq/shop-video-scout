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

    /**
     * Find all tasks for a user with pagination.
     * Story 5.5: 历史任务管理
     *
     * @param userId user ID
     * @param offset offset for pagination
     * @param limit max results
     * @return list of tasks
     */
    @Select("SELECT * FROM tasks WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Task> findTasksByUserWithPagination(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * Count all tasks for a user.
     * Story 5.5: 历史任务管理
     *
     * @param userId user ID
     * @return total count
     */
    @Select("SELECT COUNT(*) FROM tasks WHERE user_id = #{userId}")
    long countTasksByUser(@Param("userId") Long userId);
}
