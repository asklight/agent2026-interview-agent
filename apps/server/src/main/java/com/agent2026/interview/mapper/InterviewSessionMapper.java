package com.agent2026.interview.mapper;

import com.agent2026.interview.entity.InterviewSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InterviewSessionMapper extends BaseMapper<InterviewSession> {
    @Select("SELECT * FROM interview_session WHERE id=#{sessionId} FOR UPDATE")
    InterviewSession selectForUpdate(@Param("sessionId") Long sessionId);

    @Update("""
        UPDATE interview_session SET version=version+1
        WHERE id=#{sessionId} AND version=#{expectedVersion} AND status='IN_PROGRESS'
        """)
    int reserveTurn(@Param("sessionId") Long sessionId, @Param("expectedVersion") long expectedVersion);
}
