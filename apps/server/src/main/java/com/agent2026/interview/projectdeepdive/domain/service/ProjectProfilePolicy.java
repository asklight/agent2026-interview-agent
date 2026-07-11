package com.agent2026.interview.projectdeepdive.domain.service;

import com.agent2026.interview.projectdeepdive.domain.model.ProjectAnalysisStatus;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfile;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class ProjectProfilePolicy {

    public void requireAnalyzable(ProjectProfile profile) {
        if (profile.analysisStatus() != ProjectAnalysisStatus.DRAFT
                && profile.analysisStatus() != ProjectAnalysisStatus.FAILED) {
            throw new BusinessException(ErrorCode.PROJECT_PROFILE_STATE_CONFLICT,
                    "当前状态 " + profile.analysisStatus() + " 不允许开始分析");
        }
    }

    public void requireEditable(ProjectProfile profile) {
        if (profile.analysisStatus() != ProjectAnalysisStatus.REVIEW_REQUIRED
                && profile.analysisStatus() != ProjectAnalysisStatus.READY) {
            throw new BusinessException(ErrorCode.PROJECT_PROFILE_STATE_CONFLICT,
                    "当前状态 " + profile.analysisStatus() + " 不允许修正提取结果");
        }
    }

    public void requireConfirmable(ProjectProfile profile, int claimCount) {
        if (profile.analysisStatus() != ProjectAnalysisStatus.REVIEW_REQUIRED) {
            throw new BusinessException(ErrorCode.PROJECT_PROFILE_STATE_CONFLICT,
                    "只有待确认的项目档案可以确认");
        }
        if (isBlank(profile.projectName()) || isBlank(profile.summary()) || claimCount == 0) {
            throw new BusinessException(ErrorCode.PROJECT_PROFILE_NOT_READY,
                    "项目名称、项目摘要和至少一条项目声明确认后才能开始面试");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
