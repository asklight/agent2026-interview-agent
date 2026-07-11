package com.agent2026.interview.projectdeepdive.domain.service;

import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfileAnalysis;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ProjectProfileAnalysisValidator {

    private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?%?");
    private static final Pattern METRIC_TOKEN = Pattern.compile("P95|P99|QPS|TPS|ms|毫秒|秒|吞吐|延迟|耗时|%", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESPONSIBILITY_CUE = Pattern.compile("负责|主导|承担|参与|实现|设计|开发|维护");
    private static final Pattern METRIC_CUE = Pattern.compile("P(?:95|99)|QPS|TPS|吞吐|延迟|耗时|提升|降低|减少|增长|%", Pattern.CASE_INSENSITIVE);
    private static final int MAX_LIST_SIZE = 50;

    public ProjectProfileAnalysis validate(ProjectProfileAnalysis analysis, String sourceDescription) {
        if (analysis == null) {
            invalid("模型没有返回项目分析结果");
        }
        String projectName = required(analysis.projectName(), "projectName", 255);
        String summary = required(analysis.summary(), "summary", 4000);
        List<String> techStack = cleanList(analysis.techStack(), "techStack", 100);
        List<String> responsibilities = cleanList(analysis.responsibilities(), "responsibilities", 1000);
        List<String> metrics = cleanList(analysis.metrics(), "metrics", 1000);
        List<String> architecture = cleanList(analysis.architecture(), "architecture", 1000);
        List<String> uncertainties = cleanList(analysis.uncertainties(), "uncertainties", 1000);

        if (RESPONSIBILITY_CUE.matcher(sourceDescription).find() && responsibilities.isEmpty()) {
            invalid("原文包含个人职责，但 responsibilities 为空");
        }
        if (METRIC_CUE.matcher(sourceDescription).find() && metrics.isEmpty()) {
            invalid("原文包含量化指标或效果描述，但 metrics 为空");
        }

        for (String technology : techStack) {
            if (!containsNormalized(sourceDescription, technology)) {
                invalid("技术栈“" + technology + "”无法在项目原文中找到依据");
            }
        }
        for (String responsibility : responsibilities) {
            if (!containsNormalized(sourceDescription, responsibility)) {
                invalid("职责“" + responsibility + "”无法在项目原文中找到依据");
            }
        }
        for (String architectureFact : architecture) {
            if (!containsNormalized(sourceDescription, architectureFact)) {
                invalid("架构事实“" + architectureFact + "”无法在项目原文中找到依据");
            }
        }
        for (String metric : metrics) {
            requireMetricEvidence(metric, sourceDescription);
        }
        if (analysis.claims() == null || analysis.claims().isEmpty()) {
            invalid("至少需要提取一条可验证的项目声明");
        }
        if (analysis.claims().size() > 30) {
            invalid("项目声明数量不能超过 30 条");
        }
        List<ProjectClaim> claims = analysis.claims().stream()
                .map(claim -> validateClaim(claim, sourceDescription))
                .toList();
        return new ProjectProfileAnalysis(projectName, summary, techStack, responsibilities,
                metrics, architecture, uncertainties, claims);
    }

    public ProjectClaim validateUserClaim(ProjectClaim claim, String sourceDescription) {
        try {
            return validateClaim(claim, sourceDescription);
        } catch (BusinessException ex) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    ex.getMessage() == null ? "项目声明内容不合法" : ex.getMessage());
        }
    }

    private ProjectClaim validateClaim(ProjectClaim claim, String sourceDescription) {
        if (claim == null || claim.claimType() == null || claim.riskLevel() == null) {
            invalid("项目声明类型和风险等级不能为空");
        }
        String statement = required(claim.statement(), "claim.statement", 2000);
        String sourceFragment = required(claim.sourceFragment(), "claim.sourceFragment", 2000);
        if (normalize(sourceFragment).length() < 4 || !containsNormalized(sourceDescription, sourceFragment)) {
            invalid("项目声明的 sourceFragment 无法在项目原文中找到");
        }
        List<String> technologies = cleanList(claim.relatedTechnologies(), "relatedTechnologies", 100);
        List<String> expectedEvidence = cleanList(claim.expectedEvidence(), "expectedEvidence", 500);
        if (expectedEvidence.isEmpty()) {
            invalid("每条项目声明至少需要一个预期证据");
        }
        for (String technology : technologies) {
            if (!containsNormalized(sourceDescription, technology)) {
                invalid("项目声明关联了原文不存在的技术“" + technology + "”");
            }
        }
        requireEvidenceTokens(statement, sourceDescription, "项目声明");
        return new ProjectClaim(claim.id(), claim.projectProfileId(), claim.claimType(), statement,
                sourceFragment, technologies, expectedEvidence, claim.riskLevel(), claim.confirmed(), claim.createTime());
    }

    private void requireMetricEvidence(String metric, String sourceDescription) {
        if (containsNormalized(sourceDescription, metric)) {
            return;
        }
        requireEvidenceTokens(metric, sourceDescription, "指标");
        Set<String> sourceNumbers = new LinkedHashSet<>();
        Matcher sourceMatcher = NUMBER.matcher(sourceDescription);
        while (sourceMatcher.find()) {
            sourceNumbers.add(sourceMatcher.group());
        }
        Matcher matcher = NUMBER.matcher(metric);
        boolean foundNumber = false;
        while (matcher.find()) {
            foundNumber = true;
            if (!sourceNumbers.contains(matcher.group())) {
                invalid("指标“" + metric + "”包含原文不存在的数值");
            }
        }
        if (!foundNumber) {
            invalid("指标“" + metric + "”无法在项目原文中找到依据");
        }
    }

    private void requireEvidenceTokens(String fact, String sourceDescription, String field) {
        Set<String> sourceNumbers = new LinkedHashSet<>();
        Matcher sourceNumberMatcher = NUMBER.matcher(sourceDescription);
        while (sourceNumberMatcher.find()) {
            sourceNumbers.add(sourceNumberMatcher.group().toLowerCase(Locale.ROOT));
        }
        Matcher factNumberMatcher = NUMBER.matcher(fact);
        while (factNumberMatcher.find()) {
            String token = factNumberMatcher.group().toLowerCase(Locale.ROOT);
            if (!sourceNumbers.contains(token)) {
                invalid(field + "包含原文不存在的数值“" + token + "”");
            }
        }

        String normalizedSource = normalize(sourceDescription);
        Matcher markerMatcher = METRIC_TOKEN.matcher(fact);
        while (markerMatcher.find()) {
            String marker = normalize(markerMatcher.group());
            if (!normalizedSource.contains(marker)) {
                invalid(field + "包含原文不存在的指标或单位“" + markerMatcher.group() + "”");
            }
        }
    }

    private List<String> cleanList(List<String> values, String field, int itemMaxLength) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        if (values.size() > MAX_LIST_SIZE) {
            invalid(field + " 数量不能超过 " + MAX_LIST_SIZE);
        }
        Set<String> cleaned = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String item = value.trim();
            if (item.length() > itemMaxLength) {
                invalid(field + " 单项内容过长");
            }
            cleaned.add(item);
        }
        return List.copyOf(cleaned);
    }

    private String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            invalid(field + " 不能为空");
        }
        String cleaned = value.trim();
        if (cleaned.length() > maxLength) {
            invalid(field + " 内容过长");
        }
        return cleaned;
    }

    private boolean containsNormalized(String source, String fragment) {
        return normalize(source).contains(normalize(fragment));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}%]+", "");
    }

    private void invalid(String detail) {
        throw new BusinessException(ErrorCode.LLM_RESPONSE_INVALID, detail);
    }
}
