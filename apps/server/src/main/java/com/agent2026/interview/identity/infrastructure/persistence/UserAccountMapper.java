package com.agent2026.interview.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccountEntity> {
    @Select("SELECT * FROM app_user WHERE normalized_username=#{username} LIMIT 1")
    UserAccountEntity selectByNormalizedUsername(@Param("username") String normalizedUsername);
}
