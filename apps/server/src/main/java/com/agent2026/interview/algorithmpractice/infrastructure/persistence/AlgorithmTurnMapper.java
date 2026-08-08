package com.agent2026.interview.algorithmpractice.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AlgorithmTurnMapper extends BaseMapper<AlgorithmTurnEntity> {
    @Select("SELECT * FROM algorithm_turn WHERE session_id=#{sessionId} ORDER BY sequence_no")
    List<AlgorithmTurnEntity> selectBySession(@Param("sessionId") Long sessionId);

    @Select("SELECT * FROM algorithm_turn WHERE session_id=#{sessionId} AND client_turn_id=#{clientTurnId} LIMIT 1")
    AlgorithmTurnEntity selectByClientTurnId(@Param("sessionId") Long sessionId,
                                              @Param("clientTurnId") String clientTurnId);

    @Select("SELECT COALESCE(MAX(sequence_no), 0) + 1 FROM algorithm_turn WHERE session_id=#{sessionId}")
    int nextSequence(@Param("sessionId") Long sessionId);

    @Update("""
            UPDATE algorithm_turn SET processing_status='PROCESSING', processing_started_at=#{startedAt}
            WHERE id=#{turnId} AND processing_status='RETRYABLE_FAILED'
            """)
    int claimRetry(@Param("turnId") Long turnId, @Param("startedAt") LocalDateTime startedAt);

    @Update("""
            UPDATE algorithm_turn SET processing_status='RETRYABLE_FAILED'
            WHERE id=#{turnId} AND processing_status='PROCESSING'
            """)
    int markRetryable(@Param("turnId") Long turnId);

    @Update("""
            UPDATE algorithm_turn SET processing_status='COMPLETED'
            WHERE id=#{turnId} AND processing_status='PROCESSING'
            """)
    int markCompleted(@Param("turnId") Long turnId);
}
