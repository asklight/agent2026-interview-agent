package com.agent2026.interview.interviewsimulation.persistence;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SimulationAnswerReceiptMapper {
    @Insert("""
            INSERT IGNORE INTO simulation_answer_receipt(simulation_session_id, client_turn_id, stage_type)
            VALUES(#{simulationId}, #{clientTurnId}, #{stageType})
            """)
    int claim(@Param("simulationId") Long simulationId, @Param("clientTurnId") String clientTurnId,
              @Param("stageType") String stageType);

    @Delete("DELETE FROM simulation_answer_receipt WHERE simulation_session_id=#{simulationId} AND client_turn_id=#{clientTurnId}")
    int release(@Param("simulationId") Long simulationId, @Param("clientTurnId") String clientTurnId);
}
