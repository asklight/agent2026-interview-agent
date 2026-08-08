package com.agent2026.interview.identity.domain;

import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    public void validate(String password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "密码长度需为 8-72 位");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "密码必须同时包含字母和数字");
        }
    }
}
