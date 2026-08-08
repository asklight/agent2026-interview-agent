package com.agent2026.interview.identity.domain;

import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class UsernameNormalizer {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_\\-\\u4e00-\\u9fa5]{3,32}$");

    public String display(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (!USERNAME.matcher(value).matches()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    "用户名需为 3-32 位中文、字母、数字、下划线或连字符");
        }
        return value;
    }

    public String normalize(String display) {
        return display.toLowerCase(Locale.ROOT);
    }
}
