package com.agent2026.interview.interviewsimulation.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface SimulationStageMapper extends BaseMapper<SimulationStageEntity> {
 @Select("SELECT * FROM simulation_stage WHERE simulation_session_id=#{id} ORDER BY sequence_no") List<SimulationStageEntity> bySimulation(@Param("id") Long id);
 @Select("SELECT * FROM simulation_stage WHERE simulation_session_id=#{id} AND stage_type=#{type} LIMIT 1") SimulationStageEntity byType(@Param("id")Long id,@Param("type")String type);
 @Update("UPDATE simulation_stage SET status='COMPLETED', finished_at=CURRENT_TIMESTAMP(3) WHERE id=#{id} AND status='ACTIVE'") int complete(@Param("id")Long id);
 @Update("UPDATE simulation_stage SET status='ACTIVE', started_at=CURRENT_TIMESTAMP(3) WHERE id=#{id} AND status='PENDING'") int activate(@Param("id")Long id);
}
