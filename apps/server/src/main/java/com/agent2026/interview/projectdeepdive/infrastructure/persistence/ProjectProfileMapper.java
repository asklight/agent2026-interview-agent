package com.agent2026.interview.projectdeepdive.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProjectProfileMapper extends BaseMapper<ProjectProfileEntity> {

    @Update("""
            UPDATE project_profile
            SET analysis_status = 'ANALYZING', version = version + 1
            WHERE id = #{profileId}
              AND version = #{expectedVersion}
              AND analysis_status IN ('DRAFT', 'FAILED')
            """)
    int beginAnalysis(@Param("profileId") Long profileId, @Param("expectedVersion") long expectedVersion);

    @Update("""
            UPDATE project_profile
            SET analysis_status = 'FAILED', version = version + 1
            WHERE id = #{profileId}
              AND version = #{expectedVersion}
              AND analysis_status = 'ANALYZING'
              AND update_time < #{staleBefore}
            """)
    int recoverStaleAnalysis(@Param("profileId") Long profileId,
                             @Param("expectedVersion") long expectedVersion,
                             @Param("staleBefore") java.time.LocalDateTime staleBefore);

    @Update("""
            UPDATE project_profile
            SET project_name = #{entity.projectName},
                summary = #{entity.summary},
                tech_stack_json = #{entity.techStackJson},
                responsibilities_json = #{entity.responsibilitiesJson},
                metrics_json = #{entity.metricsJson},
                architecture_json = #{entity.architectureJson},
                uncertainties_json = #{entity.uncertaintiesJson},
                analysis_status = 'REVIEW_REQUIRED',
                version = version + 1
            WHERE id = #{entity.id}
              AND version = #{expectedVersion}
              AND analysis_status = 'ANALYZING'
            """)
    int completeAnalysis(@Param("entity") ProjectProfileEntity entity,
                         @Param("expectedVersion") long expectedVersion);

    @Update("""
            UPDATE project_profile
            SET analysis_status = 'FAILED', version = version + 1
            WHERE id = #{profileId}
              AND version = #{expectedVersion}
              AND analysis_status = 'ANALYZING'
            """)
    int markAnalysisFailed(@Param("profileId") Long profileId,
                           @Param("expectedVersion") long expectedVersion);

    @Update("""
            UPDATE project_profile
            SET project_name = #{entity.projectName},
                summary = #{entity.summary},
                tech_stack_json = #{entity.techStackJson},
                responsibilities_json = #{entity.responsibilitiesJson},
                metrics_json = #{entity.metricsJson},
                architecture_json = #{entity.architectureJson},
                uncertainties_json = #{entity.uncertaintiesJson},
                analysis_status = 'REVIEW_REQUIRED',
                version = version + 1
            WHERE id = #{entity.id}
              AND version = #{expectedVersion}
              AND analysis_status IN ('REVIEW_REQUIRED', 'READY')
            """)
    int patch(@Param("entity") ProjectProfileEntity entity,
              @Param("expectedVersion") long expectedVersion);

    @Update("""
            UPDATE project_profile
            SET analysis_status = 'READY', version = version + 1
            WHERE id = #{profileId}
              AND version = #{expectedVersion}
              AND analysis_status = 'REVIEW_REQUIRED'
            """)
    int confirm(@Param("profileId") Long profileId, @Param("expectedVersion") long expectedVersion);
}
