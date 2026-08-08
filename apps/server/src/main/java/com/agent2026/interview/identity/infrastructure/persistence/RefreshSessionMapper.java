package com.agent2026.interview.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface RefreshSessionMapper extends BaseMapper<RefreshSessionEntity> {
    @Select("SELECT * FROM auth_session WHERE jti=#{jti} FOR UPDATE")
    RefreshSessionEntity selectByJtiForUpdate(@Param("jti") String jti);

    @Update("""
            UPDATE auth_session
            SET revoked_at=#{usedAt}, replaced_by_jti=#{successorJti}, last_used_at=#{usedAt}
            WHERE id=#{id} AND revoked_at IS NULL
            """)
    int markRotated(@Param("id") Long id, @Param("successorJti") String successorJti,
                    @Param("usedAt") LocalDateTime usedAt);

    @Update("UPDATE auth_session SET revoked_at=#{revokedAt} WHERE id=#{id} AND revoked_at IS NULL")
    int revoke(@Param("id") Long id, @Param("revokedAt") LocalDateTime revokedAt);

    @Update("""
            UPDATE auth_session SET revoked_at=#{revokedAt}
            WHERE token_family_id=#{familyId} AND revoked_at IS NULL
            """)
    int revokeFamily(@Param("familyId") String tokenFamilyId,
                     @Param("revokedAt") LocalDateTime revokedAt);

    @Update("UPDATE auth_session SET revoked_at=#{revokedAt} WHERE user_id=#{userId} AND revoked_at IS NULL")
    int revokeAllForUser(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);
}
