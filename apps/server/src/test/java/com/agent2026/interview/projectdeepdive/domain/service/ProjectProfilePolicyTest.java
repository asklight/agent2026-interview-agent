package com.agent2026.interview.projectdeepdive.domain.service;

import com.agent2026.interview.projectdeepdive.domain.model.ProjectAnalysisStatus;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfile;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectProfilePolicyTest {

    private final ProjectProfilePolicy policy = new ProjectProfilePolicy();

    @Test
    void draftAndFailedProfilesCanBeAnalyzed() {
        policy.requireAnalyzable(profile(ProjectAnalysisStatus.DRAFT));
        policy.requireAnalyzable(profile(ProjectAnalysisStatus.FAILED));
    }

    @Test
    void analyzingProfileCannotStartAnotherAnalysis() {
        assertThatThrownBy(() -> policy.requireAnalyzable(profile(ProjectAnalysisStatus.ANALYZING)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> org.assertj.core.api.Assertions.assertThat(ex.getErrorCode())
                                .isEqualTo(ErrorCode.PROJECT_PROFILE_STATE_CONFLICT));
    }

    @Test
    void onlyReviewRequiredProfileCanBeConfirmed() {
        policy.requireConfirmable(profile(ProjectAnalysisStatus.REVIEW_REQUIRED), 1);

        assertThatThrownBy(() -> policy.requireConfirmable(profile(ProjectAnalysisStatus.READY), 1))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void confirmationRequiresCoreFieldsAndAtLeastOneClaim() {
        assertThatThrownBy(() -> policy.requireConfirmable(profile(ProjectAnalysisStatus.REVIEW_REQUIRED), 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> org.assertj.core.api.Assertions.assertThat(ex.getErrorCode())
                                .isEqualTo(ErrorCode.PROJECT_PROFILE_NOT_READY));
    }

    private ProjectProfile profile(ProjectAnalysisStatus status) {
        return new ProjectProfile(1L, "hash", "原文", "订单平台", "摘要",
                List.of("Redis"), List.of("缓存模块"), List.of(), List.of(), List.of(),
                status, 0, null, null);
    }
}
