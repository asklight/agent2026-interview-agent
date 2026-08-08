package com.agent2026.interview.identity.application;

import com.agent2026.interview.identity.security.CurrentUserProvider;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import com.agent2026.interview.shared.security.ResourceTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceOwnershipServiceTest {
    private ResourceOwnershipMapper mapper;
    private CurrentUserProvider currentUser;
    private ResourceTokenService tokens;
    private ResourceOwnershipService service;

    @BeforeEach
    void setUp() {
        mapper = mock(ResourceOwnershipMapper.class);
        currentUser = mock(CurrentUserProvider.class);
        tokens = mock(ResourceTokenService.class);
        service = new ResourceOwnershipService(mapper, currentUser, tokens);
    }

    @Test
    void authenticatedOwnerCanAccessWithoutLegacyResourceToken() {
        when(mapper.selectProjectOwner(10L)).thenReturn(7L);
        when(currentUser.currentUserId()).thenReturn(Optional.of(7L));

        assertDoesNotThrow(() -> service.requireProjectAccess(
                10L, "stored-hash", null, ErrorCode.PROJECT_PROFILE_ACCESS_DENIED));
    }

    @Test
    void anotherAuthenticatedUserCannotUseLeakedLegacyTokenForOwnedProfile() {
        when(mapper.selectProjectOwner(10L)).thenReturn(7L);
        when(currentUser.currentUserId()).thenReturn(Optional.of(8L));
        when(tokens.matches("legacy-token", "stored-hash")).thenReturn(true);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.requireProjectAccess(10L, "stored-hash", "legacy-token",
                        ErrorCode.PROJECT_PROFILE_ACCESS_DENIED));

        assertEquals(ErrorCode.PROJECT_PROFILE_ACCESS_DENIED, error.getErrorCode());
    }

    @Test
    void legacyUnownedProfileStillAcceptsItsResourceToken() {
        when(mapper.selectProjectOwner(10L)).thenReturn(null);
        when(tokens.matches("legacy-token", "stored-hash")).thenReturn(true);

        assertDoesNotThrow(() -> service.requireProjectAccess(
                10L, "stored-hash", "legacy-token", ErrorCode.PROJECT_PROFILE_ACCESS_DENIED));
    }
}
