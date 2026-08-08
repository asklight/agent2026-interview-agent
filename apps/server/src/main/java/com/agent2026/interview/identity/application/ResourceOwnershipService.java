package com.agent2026.interview.identity.application;

import com.agent2026.interview.identity.security.CurrentUserProvider;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import com.agent2026.interview.shared.security.ResourceTokenService;
import org.springframework.stereotype.Service;

@Service
public class ResourceOwnershipService {
    private final ResourceOwnershipMapper mapper;
    private final CurrentUserProvider currentUser;
    private final ResourceTokenService resourceTokens;

    public ResourceOwnershipService(ResourceOwnershipMapper mapper, CurrentUserProvider currentUser,
                                    ResourceTokenService resourceTokens) {
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.resourceTokens = resourceTokens;
    }

    public void attachProjectToCurrentUser(Long profileId) {
        mapper.attachProject(profileId, currentUser.requireUserId());
    }

    public void attachInterviewToCurrentUser(Long sessionId) {
        mapper.attachInterview(sessionId, currentUser.requireUserId());
    }

    public void requireProjectAccess(Long profileId, String accessTokenHash, String rawToken,
                                     ErrorCode deniedCode) {
        Long ownerId = mapper.selectProjectOwner(profileId);
        if (ownerId != null) {
            if (!currentUser.currentUserId().filter(ownerId::equals).isPresent()) {
                throw new BusinessException(deniedCode);
            }
            return;
        }
        if (rawToken == null || rawToken.isBlank() || !resourceTokens.matches(rawToken, accessTokenHash)) {
            throw new BusinessException(deniedCode);
        }
    }
}
