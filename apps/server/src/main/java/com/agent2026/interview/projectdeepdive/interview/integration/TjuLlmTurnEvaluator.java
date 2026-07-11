package com.agent2026.interview.projectdeepdive.interview.integration;

import com.agent2026.interview.client.TjuLlmClient;
import com.agent2026.interview.common.LlmApiException;
import com.agent2026.interview.projectdeepdive.interview.domain.TurnEvaluationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TjuLlmTurnEvaluator implements TurnEvaluator {
    private static final Set<String> DECISIONS = Set.of("FOLLOW_UP", "SWITCH_DIMENSION", "SWITCH_CLAIM", "WRAP_UP", "FINISH");
    private static final List<String> SCORE_DIMENSIONS = List.of("authenticity", "ownership", "technicalDepth",
            "tradeoffReasoning", "engineeringAwareness", "communication");
    private static final List<String> ACCUSATORY_TERMS = List.of("造假", "欺骗", "撒谎", "说谎", "虚假经历");
    private final TjuLlmClient client;
    private final ObjectMapper objectMapper;

    public TjuLlmTurnEvaluator(TjuLlmClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public TurnEvaluationResult evaluate(TurnEvaluationContext context) {
        String raw = client.chat(prompt(context)).getContent();
        try {
            return parse(raw);
        } catch (RuntimeException first) {
            String repaired = client.chat("请只修复下面内容为符合约定的 JSON，不增加事实：\n" + raw).getContent();
            try {
                return parse(repaired);
            } catch (RuntimeException second) {
                throw new LlmApiException(50211, "项目面试评价格式无效，请重试", second);
            }
        }
    }

    private String prompt(TurnEvaluationContext context) {
        StringBuilder history = new StringBuilder();
        context.recentTurns().stream().skip(Math.max(0, context.recentTurns().size() - 6))
                .forEach(turn -> history.append(turn.role()).append(':').append(turn.content()).append('\n'));
        return """
                你是严谨的项目经历面试官。评价候选人回答并生成下一问，只输出 JSON。
                禁止直接断言造假；风险只能描述证据不足、前后不一致或需要进一步验证。
                JSON 字段：scores、hitPoints(string[])、missingPoints(string[])、
                weaknesses(string[])、evidence(string[])、riskFlags(string[])、decision、suggestedFollowUp。
                scores 必须完整包含 authenticity、ownership、technicalDepth、tradeoffReasoning、engineeringAwareness、communication；
                每个值只能是 0-100 整数或 null，未评估必须写 null，不允许使用其他评分字段。
                decision 只能是 FOLLOW_UP/SWITCH_DIMENSION/SWITCH_CLAIM/WRAP_UP/FINISH。
                suggestedFollowUp 必须是自然、单一且可回答的问题。
                项目摘要：%s
                当前目标：%s
                最近对话：%s
                候选人回答：%s
                补充知识：%s
                """.formatted(context.projectSummary(), context.probe().objective(), history,
                context.candidateAnswer(), context.retrievedKnowledge());
    }

    private TurnEvaluationResult parse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(stripFence(raw));
            if (!root.isObject()) throw new IllegalArgumentException("evaluation must be object");
            String decision = requiredText(root, "decision");
            if (!DECISIONS.contains(decision)) throw new IllegalArgumentException("invalid decision");
            String followUp = requiredText(root, "suggestedFollowUp");
            Map<String, Integer> scores = new LinkedHashMap<>();
            JsonNode scoreNode = root.path("scores");
            if (!scoreNode.isObject() || scoreNode.size() != SCORE_DIMENSIONS.size())
                throw new IllegalArgumentException("scores must contain fixed six dimensions");
            for (String dimension : SCORE_DIMENSIONS) {
                if (!scoreNode.has(dimension)) throw new IllegalArgumentException("missing score dimension: " + dimension);
                JsonNode value = scoreNode.get(dimension);
                if (value.isNull()) scores.put(dimension, null);
                else if (value.isIntegralNumber() && value.asInt() >= 0 && value.asInt() <= 100)
                    scores.put(dimension, value.asInt());
                else throw new IllegalArgumentException("score out of range: " + dimension);
            }
            List<String> riskFlags = strings(root, "riskFlags");
            if (riskFlags.stream().anyMatch(this::containsAccusation))
                throw new IllegalArgumentException("risk flag contains accusatory conclusion");
            return new TurnEvaluationResult(scores, strings(root, "hitPoints"), strings(root, "missingPoints"),
                    strings(root, "weaknesses"), strings(root, "evidence"), riskFlags,
                    decision, followUp, sha256(raw), false);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid json", ex);
        }
    }

    private String requiredText(JsonNode root, String field) {
        String value = root.path(field).asText("").trim();
        if (value.isEmpty()) throw new IllegalArgumentException(field + " is required");
        return value;
    }

    private List<String> strings(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (!node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        node.forEach(item -> { if (item.isTextual() && !item.asText().isBlank()) values.add(item.asText().trim()); });
        return List.copyOf(values);
    }

    private String stripFence(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) text = text.substring(firstNewline + 1, lastFence).trim();
        }
        return text;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte b : digest) result.append(String.format("%02x", b));
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private boolean containsAccusation(String value) {
        return ACCUSATORY_TERMS.stream().anyMatch(value::contains);
    }
}
