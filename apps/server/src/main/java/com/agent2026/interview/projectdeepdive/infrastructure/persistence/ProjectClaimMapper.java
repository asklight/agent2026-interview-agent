package com.agent2026.interview.projectdeepdive.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProjectClaimMapper extends BaseMapper<ProjectClaimEntity> {

    @Delete("DELETE FROM project_claim WHERE project_profile_id = #{profileId}")
    int deleteByProfileId(@Param("profileId") Long profileId);

    @Update("UPDATE project_claim SET confirmed = 1 WHERE project_profile_id = #{profileId}")
    int confirmByProfileId(@Param("profileId") Long profileId);

    @Update("UPDATE project_claim SET confirmed = 0 WHERE project_profile_id = #{profileId}")
    int unconfirmByProfileId(@Param("profileId") Long profileId);

    @Update("""
            UPDATE project_claim
            SET claim_type = #{entity.claimType},
                statement = #{entity.statement},
                source_fragment = #{entity.sourceFragment},
                related_technologies_json = #{entity.relatedTechnologiesJson},
                expected_evidence_json = #{entity.expectedEvidenceJson},
                risk_level = #{entity.riskLevel},
                confirmed = 0
            WHERE id = #{entity.id} AND project_profile_id = #{entity.projectProfileId}
            """)
    int updateOwned(@Param("entity") ProjectClaimEntity entity);
}
