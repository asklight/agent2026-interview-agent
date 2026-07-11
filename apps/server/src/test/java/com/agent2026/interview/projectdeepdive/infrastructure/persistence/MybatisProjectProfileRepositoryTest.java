package com.agent2026.interview.projectdeepdive.infrastructure.persistence;

import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaimRiskLevel;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaimType;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfileAnalysis;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MybatisProjectProfileRepositoryTest {

    private ProjectProfileMapper profileMapper;
    private ProjectClaimMapper claimMapper;
    private MybatisProjectProfileRepository repository;

    @BeforeEach
    void setUp() {
        profileMapper = mock(ProjectProfileMapper.class);
        claimMapper = mock(ProjectClaimMapper.class);
        repository = new MybatisProjectProfileRepository(profileMapper, claimMapper, new ObjectMapper());
    }

    @Test
    void completeAnalysisReplacesClaimsOnlyAfterVersionedProfileUpdateSucceeds() {
        when(profileMapper.completeAnalysis(any(), eq(1L))).thenReturn(1);

        boolean completed = repository.completeAnalysis(1L, 1, analysis());

        assertThat(completed).isTrue();
        verify(claimMapper).deleteByProfileId(1L);
        verify(claimMapper).insert(any(ProjectClaimEntity.class));
    }

    @Test
    void failedVersionCheckNeverTouchesExistingClaims() {
        when(profileMapper.completeAnalysis(any(), eq(1L))).thenReturn(0);

        boolean completed = repository.completeAnalysis(1L, 1, analysis());

        assertThat(completed).isFalse();
        verify(claimMapper, never()).deleteByProfileId(1L);
        verify(claimMapper, never()).insert(any(ProjectClaimEntity.class));
    }

    private ProjectProfileAnalysis analysis() {
        ProjectClaim claim = new ProjectClaim(null, null, ProjectClaimType.RESPONSIBILITY,
                "负责缓存模块", "我负责缓存模块", List.of("Redis"), List.of("代码边界"),
                ProjectClaimRiskLevel.MEDIUM, false, null);
        return new ProjectProfileAnalysis("订单平台", "摘要", List.of("Redis"),
                List.of("缓存模块"), List.of(), List.of(), List.of(), List.of(claim));
    }
}
