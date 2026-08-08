package com.agent2026.interview.interviewsimulation.api;
import java.time.LocalDateTime;
import java.util.List;

public record SimulationReportResponse(int schemaVersion,Long simulationId,String completionStatus,
                                       List<StageReport> stages,List<String> recommendations,LocalDateTime generatedAt) {
 public record StageReport(String stageType,String status,Object report) {}
}
