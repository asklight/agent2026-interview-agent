package com.agent2026.interview.interviewsimulation.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SimulationSessionMapper extends BaseMapper<SimulationSessionEntity> {
 @Insert("""
 INSERT IGNORE INTO simulation_session(user_id, client_request_id, project_profile_id, algorithm_problem_id,
                                       status, current_stage, version)
 VALUES(#{userId}, #{clientRequestId}, #{projectProfileId}, #{algorithmProblemId}, 'IN_PROGRESS', 'PROJECT', 0)
 """)
 int insertRequest(SimulationSessionEntity entity);
 @Select("SELECT * FROM simulation_session WHERE user_id=#{userId} AND client_request_id=#{clientRequestId} LIMIT 1")
 SimulationSessionEntity byRequest(@Param("userId") Long userId,@Param("clientRequestId") String clientRequestId);
 @Select("SELECT * FROM simulation_session WHERE id=#{id} FOR UPDATE") SimulationSessionEntity lock(@Param("id") Long id);
 @Update("""
 UPDATE simulation_session SET current_stage=#{nextStage}, version=version+1,
 status=CASE WHEN #{nextStage}='FINISHED' THEN 'FINISHED' ELSE status END,
 finished_at=CASE WHEN #{nextStage}='FINISHED' THEN CURRENT_TIMESTAMP(3) ELSE finished_at END
 WHERE id=#{id} AND user_id=#{userId} AND version=#{version} AND current_stage=#{currentStage}
 """)
 int advance(@Param("id") Long id,@Param("userId") Long userId,@Param("version") long version,
             @Param("currentStage") String currentStage,@Param("nextStage") String nextStage);
}
