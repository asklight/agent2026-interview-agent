package com.agent2026.interview.projectdeepdive.infrastructure.llm;

import com.agent2026.interview.client.TjuLlmClient;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaim;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaimRiskLevel;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectClaimType;
import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfileAnalysis;
import com.agent2026.interview.projectdeepdive.domain.port.ProjectProfileAnalyzer;
import com.agent2026.interview.projectdeepdive.domain.service.ProjectProfileAnalysisValidator;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import com.agent2026.interview.vo.LlmTestVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TjuLlmProjectProfileAnalyzer implements ProjectProfileAnalyzer {

    private final TjuLlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final ProjectProfileAnalysisValidator validator;

    public TjuLlmProjectProfileAnalyzer(TjuLlmClient llmClient,
                                        ObjectMapper objectMapper,
                                        ProjectProfileAnalysisValidator validator) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    public ProjectProfileAnalysis analyze(String sanitizedDescription) {
        if (!llmClient.isConfigured()) {
            throw new BusinessException(ErrorCode.PROJECT_PROFILE_ANALYSIS_FAILED,
                    "学校 tju-llm 尚未配置，暂时无法分析项目经历");
        }
        LlmTestVO response = llmClient.chat(buildPrompt(sanitizedDescription));
        try {
            return validator.validate(parse(response.getContent()), sanitizedDescription);
        } catch (BusinessException | IllegalArgumentException ex) {
            LlmTestVO repaired = llmClient.chat(buildRepairPrompt(sanitizedDescription, response.getContent()));
            try {
                return validator.validate(parse(repaired.getContent()), sanitizedDescription);
            } catch (RuntimeException repairFailure) {
                throw new BusinessException(ErrorCode.LLM_RESPONSE_INVALID,
                        "模型两次返回的项目分析结果都不符合约定结构");
            }
        }
    }

    private ProjectProfileAnalysis parse(String content) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(content));
            List<ProjectClaim> claims = new ArrayList<>();
            JsonNode claimNodes = root.path("claims");
            if (claimNodes.isArray()) {
                for (JsonNode node : claimNodes) {
                    claims.add(new ProjectClaim(
                            null,
                            null,
                            enumValue(ProjectClaimType.class, text(node, "claimType"), "claimType"),
                            text(node, "statement"),
                            text(node, "sourceFragment"),
                            strings(node.path("relatedTechnologies")),
                            strings(node.path("expectedEvidence")),
                            enumValue(ProjectClaimRiskLevel.class, text(node, "riskLevel"), "riskLevel"),
                            false,
                            null
                    ));
                }
            }
            return new ProjectProfileAnalysis(
                    text(root, "projectName"),
                    text(root, "summary"),
                    strings(root.path("techStack")),
                    strings(root.path("responsibilities")),
                    strings(root.path("metrics")),
                    strings(root.path("architecture")),
                    strings(root.path("uncertainties")),
                    claims
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.LLM_RESPONSE_INVALID, "项目分析 JSON 解析失败");
        }
    }

    private String extractJson(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.LLM_RESPONSE_INVALID, "模型返回内容为空");
        }
        String trimmed = content.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException(ErrorCode.LLM_RESPONSE_INVALID, "模型返回内容不包含 JSON 对象");
        }
        return trimmed.substring(start, end + 1);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual()) {
            throw new BusinessException(ErrorCode.LLM_RESPONSE_INVALID, field + " 必须是字符串");
        }
        return value.asText();
    }

    private List<String> strings(JsonNode node) {
        if (!node.isArray()) {
            throw new BusinessException(ErrorCode.LLM_RESPONSE_INVALID, "列表字段必须是数组");
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isTextual()) {
                throw new BusinessException(ErrorCode.LLM_RESPONSE_INVALID, "列表字段只能包含字符串");
            }
            values.add(item.asText());
        }
        return values;
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.LLM_RESPONSE_INVALID, field + " 枚举值不合法");
        }
    }

    private String buildPrompt(String description) {
        return """
                你是严谨的 Java 后端项目面试档案分析器。请只根据用户提供的项目原文提取事实，不能补充、猜测或编造原文不存在的技术、职责和指标。

                只输出一个 JSON 对象，不要输出 Markdown 或解释。结构必须严格为：
                {
                  "projectName": "项目名称；原文未明确时使用能概括项目的中性名称",
                  "summary": "只基于原文的一到三句摘要",
                  "techStack": ["原文明确出现的技术"],
                  "responsibilities": ["候选人明确承担的职责"],
                  "metrics": ["原文明确给出的指标或业务结果；没有则为空数组"],
                  "architecture": ["原文明确描述的架构或关键链路"],
                  "uncertainties": ["需要用户确认、原文证据不足或表述含糊的事项"],
                  "claims": [{
                    "claimType": "RESPONSIBILITY | TECHNICAL_CHOICE | PERFORMANCE_IMPROVEMENT | ARCHITECTURE_DESIGN | INCIDENT_HANDLING | BUSINESS_RESULT",
                    "statement": "需要在面试中验证的项目声明",
                    "sourceFragment": "必须逐字摘自项目原文的短片段",
                    "relatedTechnologies": ["相关技术"],
                    "expectedEvidence": ["面试时应继续验证的证据"],
                    "riskLevel": "LOW | MEDIUM | HIGH"
                  }]
                }

                约束：
                1. techStack 中每一项都必须能在原文找到。
                2. metrics 中的所有数值必须原样来自原文。
                3. 每条 claim 的 sourceFragment 必须是原文连续片段，不能改写。
                4. 至少提取一条 claim；没有量化指标时不要制造指标。

                【项目原文】
                %s
                """.formatted(description);
    }

    private String buildRepairPrompt(String description, String invalidContent) {
        return buildPrompt(description) + """

                上一次输出没有通过结构或事实依据校验。请依据上面的同一份项目原文重新生成完整 JSON。
                不要沿用无依据内容，也不要输出解释。

                【上一次无效输出，仅用于定位格式问题，不可作为事实来源】
                %s
                """.formatted(invalidContent.length() > 10000 ? invalidContent.substring(0, 10000) : invalidContent);
    }
}
