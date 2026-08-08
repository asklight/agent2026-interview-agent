package com.agent2026.interview.interviewsimulation.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SimulationReportMapper extends BaseMapper<SimulationReportEntity> {
 @Select("SELECT * FROM simulation_report WHERE simulation_session_id=#{id} LIMIT 1") SimulationReportEntity bySimulation(@Param("id")Long id);
 @Insert("INSERT IGNORE INTO simulation_report(simulation_session_id,report_json,schema_version,generated_at) VALUES(#{simulationSessionId},#{reportJson},#{schemaVersion},#{generatedAt})") int insertIgnore(SimulationReportEntity entity);
}
