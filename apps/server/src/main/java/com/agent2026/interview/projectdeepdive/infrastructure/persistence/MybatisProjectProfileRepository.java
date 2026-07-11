package com.agent2026.interview.projectdeepdive.infrastructure.persistence;

import com.agent2026.interview.projectdeepdive.domain.model.ProjectAnalysisStatus;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaimRiskLevel;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaimType;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfile;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfileAnalysis;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfilePatch;
import com.agent2026.interview.projectdeepdive.domain.port.ProjectProfileRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MybatisProjectProfileRepository implements ProjectProfileRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() { };

    private final ProjectProfileMapper profileMapper;
    private final ProjectClaimMapper claimMapper;
    private final ObjectMapper objectMapper;

    public MybatisProjectProfileRepository(ProjectProfileMapper profileMapper,
                                           ProjectClaimMapper claimMapper,
                                           ObjectMapper objectMapper) {
        this.profileMapper = profileMapper;
        this.claimMapper = claimMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ProjectProfile createDraft(String tokenHash, String sanitizedDescription) {
        ProjectProfileEntity entity = new ProjectProfileEntity();
        entity.setAccessTokenHash(tokenHash);
        entity.setSanitizedDescription(sanitizedDescription);
        entity.setAnalysisStatus(ProjectAnalysisStatus.DRAFT.name());
        entity.setVersion(0L);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(entity.getCreateTime());
        profileMapper.insert(entity);
        return toDomain(profileMapper.selectById(entity.getId()));
    }

    @Override
    public Optional<ProjectProfile> findById(Long profileId) {
        return Optional.ofNullable(profileMapper.selectById(profileId)).map(this::toDomain);
    }

    @Override
    public List<ProjectClaim> findClaims(Long profileId) {
        return claimMapper.selectList(new LambdaQueryWrapper<ProjectClaimEntity>()
                        .eq(ProjectClaimEntity::getProjectProfileId, profileId)
                        .orderByAsc(ProjectClaimEntity::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public boolean beginAnalysis(Long profileId, long expectedVersion) {
        return profileMapper.beginAnalysis(profileId, expectedVersion) == 1;
    }

    @Override
    @Transactional
    public boolean recoverStaleAnalysis(Long profileId, long expectedVersion, LocalDateTime staleBefore) {
        return profileMapper.recoverStaleAnalysis(profileId, expectedVersion, staleBefore) == 1;
    }

    @Override
    @Transactional
    public boolean completeAnalysis(Long profileId, long expectedVersion, ProjectProfileAnalysis analysis) {
        ProjectProfileEntity entity = analysisEntity(profileId, analysis);
        if (profileMapper.completeAnalysis(entity, expectedVersion) != 1) {
            return false;
        }
        replaceClaims(profileId, analysis.claims());
        return true;
    }

    @Override
    @Transactional
    public boolean markAnalysisFailed(Long profileId, long expectedVersion) {
        return profileMapper.markAnalysisFailed(profileId, expectedVersion) == 1;
    }

    @Override
    @Transactional
    public boolean patch(Long profileId, long expectedVersion, ProjectProfilePatch patch) {
        ProjectProfileEntity entity = patchEntity(profileId, patch);
        if (profileMapper.patch(entity, expectedVersion) != 1) {
            return false;
        }
        claimMapper.unconfirmByProfileId(profileId);
        if (patch.claims() != null) {
            if (!patch.claims().isEmpty() && patch.claims().stream().allMatch(claim -> claim.id() != null)) {
                updateClaimsInPlace(profileId, patch.claims());
            } else {
                replaceClaims(profileId, patch.claims());
            }
        }
        return true;
    }

    @Override
    @Transactional
    public boolean confirm(Long profileId, long expectedVersion) {
        if (profileMapper.confirm(profileId, expectedVersion) != 1) {
            return false;
        }
        claimMapper.confirmByProfileId(profileId);
        return true;
    }

    private void replaceClaims(Long profileId, List<ProjectClaim> claims) {
        claimMapper.deleteByProfileId(profileId);
        for (ProjectClaim claim : claims) {
            ProjectClaimEntity entity = new ProjectClaimEntity();
            entity.setProjectProfileId(profileId);
            entity.setClaimType(claim.claimType().name());
            entity.setStatement(claim.statement());
            entity.setSourceFragment(claim.sourceFragment());
            entity.setRelatedTechnologiesJson(writeList(claim.relatedTechnologies()));
            entity.setExpectedEvidenceJson(writeList(claim.expectedEvidence()));
            entity.setRiskLevel(claim.riskLevel().name());
            entity.setConfirmed(claim.confirmed());
            entity.setCreateTime(LocalDateTime.now());
            claimMapper.insert(entity);
        }
    }

    private void updateClaimsInPlace(Long profileId, List<ProjectClaim> claims) {
        for (ProjectClaim claim : claims) {
            ProjectClaimEntity entity = claimEntity(profileId, claim);
            entity.setId(claim.id());
            if (claimMapper.updateOwned(entity) != 1) {
                throw new IllegalStateException("项目声明不存在或不属于当前项目档案");
            }
        }
    }

    private ProjectClaimEntity claimEntity(Long profileId, ProjectClaim claim) {
        ProjectClaimEntity entity = new ProjectClaimEntity();
        entity.setProjectProfileId(profileId);
        entity.setClaimType(claim.claimType().name());
        entity.setStatement(claim.statement());
        entity.setSourceFragment(claim.sourceFragment());
        entity.setRelatedTechnologiesJson(writeList(claim.relatedTechnologies()));
        entity.setExpectedEvidenceJson(writeList(claim.expectedEvidence()));
        entity.setRiskLevel(claim.riskLevel().name());
        entity.setConfirmed(false);
        return entity;
    }

    private ProjectProfileEntity analysisEntity(Long profileId, ProjectProfileAnalysis analysis) {
        ProjectProfileEntity entity = new ProjectProfileEntity();
        entity.setId(profileId);
        entity.setProjectName(analysis.projectName());
        entity.setSummary(analysis.summary());
        entity.setTechStackJson(writeList(analysis.techStack()));
        entity.setResponsibilitiesJson(writeList(analysis.responsibilities()));
        entity.setMetricsJson(writeList(analysis.metrics()));
        entity.setArchitectureJson(writeList(analysis.architecture()));
        entity.setUncertaintiesJson(writeList(analysis.uncertainties()));
        return entity;
    }

    private ProjectProfileEntity patchEntity(Long profileId, ProjectProfilePatch patch) {
        ProjectProfileEntity entity = new ProjectProfileEntity();
        entity.setId(profileId);
        entity.setProjectName(patch.projectName());
        entity.setSummary(patch.summary());
        entity.setTechStackJson(writeList(patch.techStack()));
        entity.setResponsibilitiesJson(writeList(patch.responsibilities()));
        entity.setMetricsJson(writeList(patch.metrics()));
        entity.setArchitectureJson(writeList(patch.architecture()));
        entity.setUncertaintiesJson(writeList(patch.uncertainties()));
        return entity;
    }

    private ProjectProfile toDomain(ProjectProfileEntity entity) {
        return new ProjectProfile(
                entity.getId(),
                entity.getAccessTokenHash(),
                entity.getSanitizedDescription(),
                entity.getProjectName(),
                entity.getSummary(),
                readList(entity.getTechStackJson()),
                readList(entity.getResponsibilitiesJson()),
                readList(entity.getMetricsJson()),
                readList(entity.getArchitectureJson()),
                readList(entity.getUncertaintiesJson()),
                ProjectAnalysisStatus.valueOf(entity.getAnalysisStatus()),
                entity.getVersion() == null ? 0L : entity.getVersion(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }

    private ProjectClaim toDomain(ProjectClaimEntity entity) {
        return new ProjectClaim(
                entity.getId(),
                entity.getProjectProfileId(),
                ProjectClaimType.valueOf(entity.getClaimType()),
                entity.getStatement(),
                entity.getSourceFragment(),
                readList(entity.getRelatedTechnologiesJson()),
                readList(entity.getExpectedEvidenceJson()),
                ProjectClaimRiskLevel.valueOf(entity.getRiskLevel()),
                Boolean.TRUE.equals(entity.getConfirmed()),
                entity.getCreateTime()
        );
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("项目档案 JSON 序列化失败", ex);
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("项目档案 JSON 反序列化失败", ex);
        }
    }
}
