package com.agent2026.interview.identity.infrastructure.persistence;

import com.agent2026.interview.identity.domain.UserAccount;
import com.agent2026.interview.identity.domain.UserStatus;
import com.agent2026.interview.identity.domain.port.UserAccountRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MybatisUserAccountRepository implements UserAccountRepository {
    private final UserAccountMapper mapper;

    public MybatisUserAccountRepository(UserAccountMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<UserAccount> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByNormalizedUsername(String normalizedUsername) {
        return Optional.ofNullable(mapper.selectByNormalizedUsername(normalizedUsername)).map(this::toDomain);
    }

    @Override
    public UserAccount create(String username, String normalizedUsername, String passwordHash) {
        UserAccountEntity entity = new UserAccountEntity();
        entity.setUsername(username);
        entity.setNormalizedUsername(normalizedUsername);
        entity.setPasswordHash(passwordHash);
        entity.setStatus(UserStatus.ACTIVE.name());
        mapper.insert(entity);
        return toDomain(mapper.selectById(entity.getId()));
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        return new UserAccount(entity.getId(), entity.getUsername(), entity.getNormalizedUsername(),
                entity.getPasswordHash(), UserStatus.valueOf(entity.getStatus()),
                entity.getCreateTime(), entity.getUpdateTime());
    }
}
