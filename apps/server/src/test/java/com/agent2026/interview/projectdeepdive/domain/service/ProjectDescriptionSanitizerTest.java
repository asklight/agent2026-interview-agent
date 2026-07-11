package com.agent2026.interview.projectdeepdive.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectDescriptionSanitizerTest {

    private final ProjectDescriptionSanitizer sanitizer = new ProjectDescriptionSanitizer();

    @Test
    void sanitizesCommonPersonalIdentifiersWithoutRemovingProjectMetrics() {
        String source = "联系人 +86 13812345678，座机 022-12345678，邮箱 dev@example.com，身份证 12010119900101123X。接口 P95 为 120ms。";

        String sanitized = sanitizer.sanitize(source);

        assertThat(sanitized)
                .doesNotContain("13812345678", "022-12345678", "dev@example.com", "12010119900101123X")
                .contains("[手机号已隐藏]", "[电话号码已隐藏]", "[邮箱已隐藏]", "[身份证号已隐藏]", "P95 为 120ms");
    }
}
