package com.agent2026.interview.projectdeepdive.api;

import com.agent2026.interview.projectdeepdive.application.ProjectProfileResult;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectAnalysisStatus;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectProfileResponseTest {

    @Test
    void detailResponseNeverExposesStoredTokenHashOrRawToken() throws Exception {
        ProjectProfile profile = new ProjectProfile(1L, "secret-token-hash", "脱敏原文", "订单平台", "摘要",
                List.of("Redis"), List.of(), List.of(), List.of(), List.of(),
                ProjectAnalysisStatus.REVIEW_REQUIRED, 2, null, null);

        String json = new ObjectMapper().writeValueAsString(
                ProjectProfileResponse.from(new ProjectProfileResult(profile, List.of())));

        assertThat(json)
                .doesNotContain("secret-token-hash", "accessToken", "accessTokenHash",
                        "expectedEvidence", "riskLevel")
                .contains("\"profileId\":1", "\"analysisStatus\":\"REVIEW_REQUIRED\"");
    }
}
