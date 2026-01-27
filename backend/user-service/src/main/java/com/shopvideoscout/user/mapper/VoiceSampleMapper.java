package com.shopvideoscout.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopvideoscout.user.entity.VoiceSample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * VoiceSample mapper for database operations.
 */
@Mapper
public interface VoiceSampleMapper extends BaseMapper<VoiceSample> {

    /**
     * Count voice samples for a user.
     */
    @Select("SELECT COUNT(*) FROM voice_samples WHERE user_id = #{userId}")
    int countByUserId(Long userId);

    /**
     * Count voice samples for a user with row-level lock (CONC-001: prevents TOCTOU race on sample limit).
     * Must be called within a transaction.
     */
    @Select("SELECT COUNT(*) FROM voice_samples WHERE user_id = #{userId} FOR UPDATE")
    int countByUserIdForUpdate(Long userId);
}
