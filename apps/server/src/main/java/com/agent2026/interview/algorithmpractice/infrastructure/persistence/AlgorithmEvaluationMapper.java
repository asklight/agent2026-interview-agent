package com.agent2026.interview.algorithmpractice.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AlgorithmEvaluationMapper extends BaseMapper<AlgorithmEvaluationEntity> {
    @Select("SELECT * FROM algorithm_turn_evaluation WHERE session_id=#{sessionId} ORDER BY id")
    List<AlgorithmEvaluationEntity> selectBySession(@Param("sessionId") Long sessionId);
}
