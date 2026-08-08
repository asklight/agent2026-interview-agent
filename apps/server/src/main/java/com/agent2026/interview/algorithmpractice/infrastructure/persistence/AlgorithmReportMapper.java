package com.agent2026.interview.algorithmpractice.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AlgorithmReportMapper extends BaseMapper<AlgorithmReportEntity> {
    @Select("SELECT * FROM algorithm_report WHERE session_id=#{sessionId} LIMIT 1")
    AlgorithmReportEntity selectBySession(@Param("sessionId") Long sessionId);

    @Insert("""
            INSERT IGNORE INTO algorithm_report(session_id, report_json, schema_version, generated_at)
            VALUES(#{sessionId}, #{reportJson}, #{schemaVersion}, #{generatedAt})
            """)
    int insertIgnore(AlgorithmReportEntity report);
}
