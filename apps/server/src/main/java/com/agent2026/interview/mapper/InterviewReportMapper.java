package com.agent2026.interview.mapper;

import com.agent2026.interview.entity.InterviewReport;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InterviewReportMapper extends BaseMapper<InterviewReport> {
    @Insert("""
        INSERT IGNORE INTO interview_report
        (session_id, mode, generation_status, total_score, answered_count, report_json, schema_version, generated_at)
        VALUES (#{report.sessionId}, #{report.mode}, #{report.generationStatus}, #{report.totalScore},
                #{report.answeredCount}, #{report.reportJson}, #{report.schemaVersion}, #{report.generatedAt})
        """)
    int insertIgnoreProjectReport(@Param("report") InterviewReport report);
}
