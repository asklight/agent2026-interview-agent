package com.agent2026.interview.projectdeepdive.domain.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ProjectDescriptionSanitizer {

    private static final Pattern EMAIL = Pattern.compile(
            "(?i)(?<![a-z0-9._%+-])[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}(?![a-z0-9.-])");
    private static final Pattern MOBILE = Pattern.compile("(?<!\\d)(?:\\+?86[- ]?)?1[3-9]\\d{9}(?!\\d)");
    private static final Pattern LANDLINE = Pattern.compile("(?<!\\d)0\\d{2,3}[- ]?\\d{7,8}(?!\\d)");
    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)\\d{17}[\\dXx](?!\\d)");
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\p{Cc}&&[^\\r\\n\\t]]");
    private static final Pattern EXCESSIVE_BLANK_LINES = Pattern.compile("(?:\\R[ \\t]*){3,}");

    public String sanitize(String description) {
        if (description == null) {
            return "";
        }
        String sanitized = CONTROL_CHARACTERS.matcher(description).replaceAll("");
        sanitized = EMAIL.matcher(sanitized).replaceAll("[邮箱已隐藏]");
        sanitized = MOBILE.matcher(sanitized).replaceAll("[手机号已隐藏]");
        sanitized = LANDLINE.matcher(sanitized).replaceAll("[电话号码已隐藏]");
        sanitized = ID_CARD.matcher(sanitized).replaceAll("[身份证号已隐藏]");
        sanitized = EXCESSIVE_BLANK_LINES.matcher(sanitized).replaceAll(System.lineSeparator() + System.lineSeparator());
        return sanitized.trim();
    }
}
