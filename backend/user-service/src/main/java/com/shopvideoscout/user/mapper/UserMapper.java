package com.shopvideoscout.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopvideoscout.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * User mapper for database operations.
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * Find user by phone number.
     */
    @Select("SELECT * FROM users WHERE phone = #{phone}")
    User findByPhone(String phone);
}
