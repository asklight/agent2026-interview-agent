package com.agent2026.interview.identity.application;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ResourceOwnershipMapper {
    @Select("SELECT user_id FROM project_profile WHERE id=#{profileId}")
    Long selectProjectOwner(@Param("profileId") Long profileId);

    @Update("UPDATE project_profile SET user_id=#{userId} WHERE id=#{profileId} AND user_id IS NULL")
    int attachProject(@Param("profileId") Long profileId, @Param("userId") Long userId);

    @Select("SELECT user_id FROM interview_session WHERE id=#{sessionId}")
    Long selectInterviewOwner(@Param("sessionId") Long sessionId);

    @Update("UPDATE interview_session SET user_id=#{userId} WHERE id=#{sessionId} AND user_id IS NULL")
    int attachInterview(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
}
