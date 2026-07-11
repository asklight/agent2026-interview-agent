package com.agent2026.interview.projectdeepdive.application;

import com.agent2026.interview.common.LlmApiException;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectAnalysisStatus;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfile;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfileAnalysis;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfilePatch;
import com.agent2026.interview.projectdeepdive.domain.port.ProjectProfileAnalyzer;
import com.agent2026.interview.projectdeepdive.domain.port.ProjectProfileRepository;
import com.agent2026.interview.projectdeepdive.domain.service.ProjectDescriptionSanitizer;
import com.agent2026.interview.projectdeepdive.domain.service.ProjectProfileAnalysisValidator;
import com.agent2026.interview.projectdeepdive.domain.service.ProjectProfilePolicy;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import com.agent2026.interview.shared.security.IssuedResourceToken;
import com.agent2026.interview.shared.security.ResourceTokenService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
public class ProjectProfileApplicationService {

    private static final int MAX_DESCRIPTION_LENGTH = 20_000;
    private static final long ANALYSIS_LEASE_SECONDS = 120;

    private final ProjectProfileRepository repository;
    private final ProjectProfileAnalyzer analyzer;
    private final ProjectDescriptionSanitizer sanitizer;
    private final ProjectProfileAnalysisValidator analysisValidator;
    private final ProjectProfilePolicy policy;
    private final ResourceTokenService tokenService;

    public ProjectProfileApplicationService(ProjectProfileRepository repository,
                                            ProjectProfileAnalyzer analyzer,
                                            ProjectDescriptionSanitizer sanitizer,
                                            ProjectProfileAnalysisValidator analysisValidator,
                                            ProjectProfilePolicy policy,
                                            ResourceTokenService tokenService) {
        this.repository = repository;
        this.analyzer = analyzer;
        this.sanitizer = sanitizer;
        this.analysisValidator = analysisValidator;
        this.policy = policy;
        this.tokenService = tokenService;
    }

    public CreatedProjectProfileResult create(String description) {
        String sanitizedDescription = sanitizer.sanitize(description);
        if (sanitizedDescription.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "项目经历不能为空");
        }
        if (sanitizedDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    "项目经历不能超过 " + MAX_DESCRIPTION_LENGTH + " 个字符");
        }
        IssuedResourceToken token = tokenService.issue();
        ProjectProfile profile = repository.createDraft(token.tokenHash(), sanitizedDescription);
        return new CreatedProjectProfileResult(new ProjectProfileResult(profile, List.of()), token.rawToken());
    }

    public ProjectProfileResult get(Long profileId, String rawToken) {
        ProjectProfile profile = requireOwned(profileId, rawToken);
        return new ProjectProfileResult(profile, repository.findClaims(profileId));
    }

    public ProjectProfileResult analyze(Long profileId, String rawToken) {
        ProjectProfile profile = requireOwned(profileId, rawToken);
        if (profile.analysisStatus() == ProjectAnalysisStatus.ANALYZING
                && profile.updateTime() != null
                && repository.recoverStaleAnalysis(profileId, profile.version(),
                LocalDateTime.now().minusSeconds(ANALYSIS_LEASE_SECONDS))) {
            profile = requireOwned(profileId, rawToken);
        }
        policy.requireAnalyzable(profile);
        if (!repository.beginAnalysis(profileId, profile.version())) {
            throw new BusinessException(ErrorCode.PROJECT_PROFILE_VERSION_CONFLICT,
                    "项目档案已被其他请求修改，请刷新后重试");
        }

        ProjectProfileAnalysis analysis;
        try {
            analysis = analyzer.analyze(profile.sanitizedDescription());
            if (!repository.completeAnalysis(profileId, profile.version() + 1, analysis)) {
                repository.markAnalysisFailed(profileId, profile.version() + 1);
                throw new BusinessException(ErrorCode.PROJECT_PROFILE_VERSION_CONFLICT,
                        "项目分析结果保存时检测到并发修改，请重新分析");
            }
        } catch (LlmApiException ex) {
            repository.markAnalysisFailed(profileId, profile.version() + 1);
            throw new BusinessException(ErrorCode.PROJECT_PROFILE_ANALYSIS_FAILED, ex.getMessage());
        } catch (RuntimeException ex) {
            repository.markAnalysisFailed(profileId, profile.version() + 1);
            throw ex;
        }
        return get(profileId, rawToken);
    }

    public ProjectProfileResult patch(Long profileId, String rawToken, PatchProjectProfileCommand command) {
        ProjectProfile profile = requireOwned(profileId, rawToken);
        policy.requireEditable(profile);
        if (command.version() != profile.version()) {
            throw new BusinessException(ErrorCode.PROJECT_PROFILE_VERSION_CONFLICT,
                    "项目档案版本已变化，请刷新后重新修改");
        }

        List<ProjectClaim> currentClaims = command.claims() == null ? List.of() : repository.findClaims(profileId);
        List<ProjectClaim> claims = validatedPatchClaims(command.claims(), profile, currentClaims);
        ProjectProfilePatch patch = new ProjectProfilePatch(
                mergeRequired(command.projectName(), profile.projectName(), "项目名称", 255),
                mergeRequired(command.summary(), profile.summary(), "项目摘要", 4000),
                mergeList(command.techStack(), profile.techStack(), "技术栈"),
                mergeList(command.responsibilities(), profile.responsibilities(), "个人职责"),
                mergeList(command.metrics(), profile.metrics(), "项目指标"),
                mergeList(command.architecture(), profile.architecture(), "项目架构"),
                mergeList(command.uncertainties(), profile.uncertainties(), "不确定项"),
                claims
        );
        if (!repository.patch(profileId, profile.version(), patch)) {
            throw new BusinessException(ErrorCode.PROJECT_PROFILE_VERSION_CONFLICT,
                    "项目档案已被其他请求修改，请刷新后重试");
        }
        return get(profileId, rawToken);
    }

    public ProjectProfileResult confirm(Long profileId, String rawToken) {
        ProjectProfile profile = requireOwned(profileId, rawToken);
        List<ProjectClaim> claims = repository.findClaims(profileId);
        policy.requireConfirmable(profile, claims.size());
        if (!repository.confirm(profileId, profile.version())) {
            throw new BusinessException(ErrorCode.PROJECT_PROFILE_VERSION_CONFLICT,
                    "项目档案已被其他请求修改，请刷新后重试");
        }
        return get(profileId, rawToken);
    }

    private ProjectProfile requireOwned(Long profileId, String rawToken) {
        if (profileId == null || profileId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "项目档案 ID 不合法");
        }
        ProjectProfile profile = repository.findById(profileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_PROFILE_NOT_FOUND));
        if (rawToken == null || rawToken.isBlank()
                || !tokenService.matches(rawToken, profile.accessTokenHash())) {
            throw new BusinessException(ErrorCode.PROJECT_PROFILE_ACCESS_DENIED);
        }
        return profile;
    }

    private String mergeRequired(String requested, String current, String field, int maxLength) {
        if (requested == null) {
            return current;
        }
        String value = requested.trim();
        if (value.isBlank() || value.length() > maxLength) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, field + "不能为空且长度不能超过 " + maxLength);
        }
        return value;
    }

    private List<String> mergeList(List<String> requested, List<String> current, String field) {
        if (requested == null) {
            return current;
        }
        if (requested.size() > 50) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, field + "最多包含 50 项");
        }
        Set<String> values = new LinkedHashSet<>();
        for (String item : requested) {
            if (item == null || item.isBlank()) {
                continue;
            }
            String value = item.trim();
            if (value.length() > 1000) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, field + "单项内容过长");
            }
            values.add(value);
        }
        return List.copyOf(values);
    }

    private List<ProjectClaim> validatedPatchClaims(List<ProjectClaim> requested,
                                                    ProjectProfile profile,
                                                    List<ProjectClaim> currentClaims) {
        if (requested == null) {
            return null;
        }
        Set<Long> currentIds = currentClaims.stream().map(ProjectClaim::id).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> requestedIds = requested.stream().map(ProjectClaim::id).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (requested.size() != currentClaims.size() || !requestedIds.equals(currentIds)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    "只能修正现有项目声明，不能新增、删除或替换声明标识");
        }
        Map<Long, ProjectClaim> currentById = currentClaims.stream()
                .collect(Collectors.toMap(ProjectClaim::id, Function.identity()));
        return requested.stream()
                .map(claim -> {
                    ProjectClaim current = currentById.get(claim.id());
                    ProjectClaim merged = new ProjectClaim(claim.id(), profile.id(), claim.claimType(),
                            claim.statement(), claim.sourceFragment(), claim.relatedTechnologies(),
                            current.expectedEvidence(), current.riskLevel(), false, current.createTime());
                    return analysisValidator.validateUserClaim(merged, profile.sanitizedDescription());
                })
                .toList();
    }
}
