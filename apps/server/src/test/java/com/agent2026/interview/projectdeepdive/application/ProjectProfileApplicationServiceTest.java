package com.agent2026.interview.projectdeepdive.application;

import com.agent2026.interview.projectdeepdive.domain.model.ProjectAnalysisStatus;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaimRiskLevel;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaimType;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfile;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfileAnalysis;
import com.agent2026.interview.projectdeepdive.domain.port.ProjectProfileAnalyzer;
import com.agent2026.interview.projectdeepdive.domain.port.ProjectProfileRepository;
import com.agent2026.interview.projectdeepdive.domain.service.ProjectDescriptionSanitizer;
import com.agent2026.interview.projectdeepdive.domain.service.ProjectProfileAnalysisValidator;
import com.agent2026.interview.projectdeepdive.domain.service.ProjectProfilePolicy;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import com.agent2026.interview.shared.security.IssuedResourceToken;
import com.agent2026.interview.shared.security.ResourceTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectProfileApplicationServiceTest {

    private ProjectProfileRepository repository;
    private ProjectProfileAnalyzer analyzer;
    private ResourceTokenService tokenService;
    private ProjectProfileApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProjectProfileRepository.class);
        analyzer = mock(ProjectProfileAnalyzer.class);
        tokenService = mock(ResourceTokenService.class);
        service = new ProjectProfileApplicationService(repository, analyzer,
                new ProjectDescriptionSanitizer(), new ProjectProfileAnalysisValidator(),
                new ProjectProfilePolicy(), tokenService);
    }

    @Test
    void createStoresOnlySanitizedDescriptionAndTokenHash() {
        String source = "订单平台联系人 13812345678，使用 Redis 优化缓存。";
        when(tokenService.issue()).thenReturn(new IssuedResourceToken("raw-token", "token-hash"));
        when(repository.createDraft(eq("token-hash"), any())).thenAnswer(invocation ->
                profile(ProjectAnalysisStatus.DRAFT, 0, invocation.getArgument(1)));

        CreatedProjectProfileResult result = service.create(source);

        assertThat(result.accessToken()).isEqualTo("raw-token");
        assertThat(result.projectProfile().profile().sanitizedDescription())
                .contains("[手机号已隐藏]")
                .doesNotContain("13812345678");
        verify(repository).createDraft("token-hash", "订单平台联系人 [手机号已隐藏]，使用 Redis 优化缓存。");
    }

    @Test
    void analyzeUsesTwoShortPersistenceTransitionsAroundExternalCall() {
        ProjectProfile draft = profile(ProjectAnalysisStatus.DRAFT, 0, source());
        ProjectProfile reviewed = profile(ProjectAnalysisStatus.REVIEW_REQUIRED, 2, source());
        ProjectProfileAnalysis analysis = analysis();
        when(repository.findById(1L)).thenReturn(Optional.of(draft), Optional.of(reviewed));
        when(tokenService.matches("token", "hash")).thenReturn(true);
        when(repository.beginAnalysis(1L, 0)).thenReturn(true);
        when(analyzer.analyze(source())).thenReturn(analysis);
        when(repository.completeAnalysis(1L, 1, analysis)).thenReturn(true);
        when(repository.findClaims(1L)).thenReturn(analysis.claims());

        ProjectProfileResult result = service.analyze(1L, "token");

        assertThat(result.profile().analysisStatus()).isEqualTo(ProjectAnalysisStatus.REVIEW_REQUIRED);
        verify(repository).beginAnalysis(1L, 0);
        verify(analyzer).analyze(source());
        verify(repository).completeAnalysis(1L, 1, analysis);
        verify(repository, never()).markAnalysisFailed(1L, 1L);
    }

    @Test
    void failedAnalysisMovesProfileToFailedState() {
        ProjectProfile draft = profile(ProjectAnalysisStatus.DRAFT, 0, source());
        when(repository.findById(1L)).thenReturn(Optional.of(draft));
        when(tokenService.matches("token", "hash")).thenReturn(true);
        when(repository.beginAnalysis(1L, 0)).thenReturn(true);
        when(analyzer.analyze(source())).thenThrow(new BusinessException(ErrorCode.LLM_RESPONSE_INVALID));

        assertThatThrownBy(() -> service.analyze(1L, "token"))
                .isInstanceOf(BusinessException.class);
        verify(repository).markAnalysisFailed(1L, 1L);
    }

    @Test
    void wrongResourceTokenCannotReadProfile() {
        when(repository.findById(1L)).thenReturn(Optional.of(profile(ProjectAnalysisStatus.DRAFT, 0, source())));
        when(tokenService.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.get(1L, "wrong"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROJECT_PROFILE_ACCESS_DENIED));
        verify(repository, never()).findClaims(1L);
    }

    @Test
    void readyProfileCannotReplaceClaimIdentity() {
        ProjectProfile ready = profile(ProjectAnalysisStatus.READY, 3, source());
        ProjectClaim existing = new ProjectClaim(10L, 1L, ProjectClaimType.RESPONSIBILITY,
                "负责缓存模块", "我负责缓存模块", List.of("Redis"), List.of("代码边界"),
                ProjectClaimRiskLevel.MEDIUM, true, null);
        ProjectClaim added = new ProjectClaim(null, 1L, ProjectClaimType.TECHNICAL_CHOICE,
                "选择 Redis", "使用 Redis", List.of("Redis"), List.of("选型依据"),
                ProjectClaimRiskLevel.MEDIUM, false, null);
        when(repository.findById(1L)).thenReturn(Optional.of(ready));
        when(repository.findClaims(1L)).thenReturn(List.of(existing));
        when(tokenService.matches("token", "hash")).thenReturn(true);
        PatchProjectProfileCommand command = new PatchProjectProfileCommand(3, null, null, null,
                null, null, null, null, List.of(added));

        assertThatThrownBy(() -> service.patch(1L, "token", command))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PARAM_INVALID));
        verify(repository, never()).patch(eq(1L), eq(3L), any());
    }

    @Test
    void staleAnalyzingLeaseIsRecoveredBeforeRetry() {
        ProjectProfile stale = new ProjectProfile(1L, "hash", source(), "订单平台", "摘要",
                List.of("Redis"), List.of("缓存模块"), List.of(), List.of(), List.of(),
                ProjectAnalysisStatus.ANALYZING, 1, LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().minusMinutes(5));
        ProjectProfile failed = profile(ProjectAnalysisStatus.FAILED, 2, source());
        ProjectProfile reviewed = profile(ProjectAnalysisStatus.REVIEW_REQUIRED, 4, source());
        when(repository.findById(1L)).thenReturn(Optional.of(stale), Optional.of(failed), Optional.of(reviewed));
        when(tokenService.matches("token", "hash")).thenReturn(true);
        when(repository.recoverStaleAnalysis(eq(1L), eq(1L), any())).thenReturn(true);
        when(repository.beginAnalysis(1L, 2)).thenReturn(true);
        when(analyzer.analyze(source())).thenReturn(analysis());
        when(repository.completeAnalysis(eq(1L), eq(3L), any())).thenReturn(true);
        when(repository.findClaims(1L)).thenReturn(analysis().claims());

        ProjectProfileResult result = service.analyze(1L, "token");

        assertThat(result.profile().analysisStatus()).isEqualTo(ProjectAnalysisStatus.REVIEW_REQUIRED);
        verify(repository).recoverStaleAnalysis(eq(1L), eq(1L), any());
        verify(repository).beginAnalysis(1L, 2);
    }

    private ProjectProfile profile(ProjectAnalysisStatus status, long version, String description) {
        return new ProjectProfile(1L, "hash", description, "订单平台", "摘要",
                List.of("Redis"), List.of("缓存模块"), List.of(), List.of(), List.of(),
                status, version, null, null);
    }

    private ProjectProfileAnalysis analysis() {
        ProjectClaim claim = new ProjectClaim(null, null, ProjectClaimType.RESPONSIBILITY,
                "负责缓存模块", "我负责缓存模块", List.of("Redis"), List.of("代码边界"),
                ProjectClaimRiskLevel.MEDIUM, false, null);
        return new ProjectProfileAnalysis("订单平台", "摘要", List.of("Redis"),
                List.of("缓存模块"), List.of(), List.of(), List.of(), List.of(claim));
    }

    private String source() {
        return "订单平台使用 Redis，我负责缓存模块。";
    }
}
