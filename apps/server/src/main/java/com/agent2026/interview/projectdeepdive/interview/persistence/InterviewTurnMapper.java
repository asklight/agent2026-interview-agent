package com.agent2026.interview.projectdeepdive.interview.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;

@Mapper
public interface InterviewTurnMapper extends BaseMapper<InterviewTurnEntity> {
    @Select("SELECT COALESCE(MAX(sequence_no),0) FROM interview_turn WHERE session_id=#{sessionId}")
    int maxSequence(@Param("sessionId") Long sessionId);

    @Select("SELECT COUNT(*) FROM interview_turn WHERE session_id=#{sessionId} AND role='CANDIDATE' AND processing_status IN ('PROCESSING','RETRYABLE_FAILED')")
    int countProcessingCandidates(@Param("sessionId") Long sessionId);

    @Update("""
        UPDATE interview_turn SET processing_status='PROCESSING', processing_started_at=#{now}
        WHERE id=#{turnId} AND processing_status IN ('RETRYABLE_FAILED','PROCESSING')
          AND (processing_status='RETRYABLE_FAILED' OR processing_started_at IS NULL OR processing_started_at < #{staleBefore})
        """)
    int claimRetry(@Param("turnId") Long turnId, @Param("now") LocalDateTime now,
                   @Param("staleBefore") LocalDateTime staleBefore);

    @Update("UPDATE interview_turn SET processing_status='RETRYABLE_FAILED' WHERE id=#{turnId} AND processing_status='PROCESSING'")
    int markRetryable(@Param("turnId") Long turnId);

    @Update("UPDATE interview_turn SET processing_status='COMPLETED', ended_at=#{now} WHERE id=#{turnId} AND processing_status='PROCESSING'")
    int markCompleted(@Param("turnId") Long turnId, @Param("now") LocalDateTime now);
}
