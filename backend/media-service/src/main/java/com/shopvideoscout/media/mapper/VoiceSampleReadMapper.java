package com.shopvideoscout.media.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * Read-only mapper for voice_samples table in media-service (monolith: shared DB).
 * Used by TtsSynthesisService to resolve clone_voice_id from voice_sample_id.
 */
@Mapper
public interface VoiceSampleReadMapper {

    /**
     * Get clone_voice_id for a voice sample.
     *
     * @param sampleId voice sample ID
     * @return clone_voice_id or null
     */
    @Select("SELECT clone_voice_id FROM voice_samples WHERE id = #{sampleId} AND status = 'completed'")
    String getCloneVoiceId(Long sampleId);

    /**
     * Get status for a voice sample.
     *
     * @param sampleId voice sample ID
     * @return status or null if not found
     */
    @Select("SELECT status FROM voice_samples WHERE id = #{sampleId}")
    String getStatus(Long sampleId);

    /**
     * Get user_id for a voice sample (SEC-002: ownership verification).
     *
     * @param sampleId voice sample ID
     * @return user_id or null if not found
     */
    @Select("SELECT user_id FROM voice_samples WHERE id = #{sampleId}")
    Long getUserId(Long sampleId);
}
