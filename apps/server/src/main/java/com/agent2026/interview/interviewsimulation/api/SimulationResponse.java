package com.agent2026.interview.interviewsimulation.api;
import java.util.List;

public record SimulationResponse(Long simulationId,String status,String currentStage,long version,
                                 List<StageResponse> stages,Object stageData) {
 public record StageResponse(String stageType,int sequence,String status,Long businessSessionId) {}
}
