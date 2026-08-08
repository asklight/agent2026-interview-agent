package com.agent2026.interview.algorithmpractice.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AlgorithmSessionMapper extends BaseMapper<AlgorithmSessionEntity> {
    @Select("SELECT * FROM algorithm_session WHERE id=#{sessionId} FOR UPDATE")
    AlgorithmSessionEntity selectForUpdate(@Param("sessionId") Long sessionId);

    @Update("UPDATE algorithm_session SET simulation_id=#{simulationId} WHERE id=#{sessionId}")
    int attachSimulation(@Param("sessionId") Long sessionId, @Param("simulationId") Long simulationId);
}
