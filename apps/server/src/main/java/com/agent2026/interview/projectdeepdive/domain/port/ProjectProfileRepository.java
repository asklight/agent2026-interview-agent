package com.agent2026.interview.projectdeepdive.domain.port;

import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfile;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfileAnalysis;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfilePatch;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ProjectProfileRepository {

    ProjectProfile createDraft(String tokenHash, String sanitizedDescription);

    Optional<ProjectProfile> findById(Long profileId);

    List<ProjectClaim> findClaims(Long profileId);

    boolean beginAnalysis(Long profileId, long expectedVersion);

    boolean recoverStaleAnalysis(Long profileId, long expectedVersion, LocalDateTime staleBefore);

    boolean completeAnalysis(Long profileId, long expectedVersion, ProjectProfileAnalysis analysis);

    boolean markAnalysisFailed(Long profileId, long expectedVersion);

    boolean patch(Long profileId, long expectedVersion, ProjectProfilePatch patch);

    boolean confirm(Long profileId, long expectedVersion);
}
