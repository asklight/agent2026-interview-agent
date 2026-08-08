package com.agent2026.interview.algorithmpractice.infrastructure.llm;

import com.agent2026.interview.algorithmpractice.domain.AlgorithmEvaluation;
import com.agent2026.interview.algorithmpractice.domain.AlgorithmEvaluationContext;
import com.agent2026.interview.algorithmpractice.domain.AlgorithmTurnEvaluator;
import com.agent2026.interview.client.TjuLlmClient;
import com.agent2026.interview.common.LlmApiException;
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

@Component
public class TjuLlmAlgorithmTurnEvaluator implements AlgorithmTurnEvaluator {
    private static final List<String> DIMENSIONS = List.of(
            "correctness", "optimization", "complexity", "edgeCases", "communication");
    private final TjuLlmClient client;
    private final ObjectMapper objectMapper;

    public TjuLlmAlgorithmTurnEvaluator(TjuLlmClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public AlgorithmEvaluation evaluate(AlgorithmEvaluationContext context) {
        String raw = client.chat(prompt(context)).getContent();
        try {
            return parse(raw);
        } catch (RuntimeException first) {
            String repaired = client.chat("请只把下面内容修复为符合原约定的 JSON，不补充新的算法事实：\n" + raw)
                    .getContent();
            try {
                return parse(repaired);
            } catch (RuntimeException second) {
                throw new LlmApiException(50211, "算法口述评价格式无效，请安全重试", second);
            }
        }
    }

    private String prompt(AlgorithmEvaluationContext context) {
        return """
                你是一名严谨但自然的 Java 技术面试官。评价候选人在算法口述当前阶段的回答，只输出 JSON。
                JSON 必须包含：
                scores：固定包含 correctness、optimization、complexity、edgeCases、communication，值为 0-100 整数或 null；
                strengths：string[]；gaps：string[]；evidence：string[]；suggestedFollowUp：string。
                只评价当前回答实际出现的内容，未涉及的维度写 null。evidence 必须引用或紧贴候选人原话。
                suggestedFollowUp 必须是一条自然、单一、可回答的变体问题，不能泄露标准答案。
                题目：%s
                题意：%s
                约束：%s
                评价规则：%s
                当前阶段：%s
                最近对话：%s
                候选人回答：%s
                """.formatted(context.title(), context.statement(), context.constraints(), context.rubric(),
                context.stage(), context.recentConversation(), context.candidateAnswer());
    }

    private AlgorithmEvaluation parse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(stripFence(raw));
            if (!root.isObject()) throw new IllegalArgumentException("evaluation must be object");
            JsonNode scoreNode = root.path("scores");
            if (!scoreNode.isObject() || scoreNode.size() != DIMENSIONS.size()) {
                throw new IllegalArgumentException("scores must contain five fixed dimensions");
            }
            Map<String, Integer> scores = new LinkedHashMap<>();
            for (String dimension : DIMENSIONS) {
                if (!scoreNode.has(dimension)) throw new IllegalArgumentException("missing score: " + dimension);
                JsonNode value = scoreNode.get(dimension);
                if (value.isNull()) scores.put(dimension, null);
                else if (value.isIntegralNumber() && value.asInt() >= 0 && value.asInt() <= 100) {
                    scores.put(dimension, value.asInt());
                } else throw new IllegalArgumentException("invalid score: " + dimension);
            }
            String followUp = root.path("suggestedFollowUp").asText("").trim();
            if (followUp.isBlank()) throw new IllegalArgumentException("suggestedFollowUp is required");
            return new AlgorithmEvaluation(scores, strings(root, "strengths"), strings(root, "gaps"),
                    strings(root, "evidence"), followUp, sha256(raw), false);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid evaluation json", ex);
        }
    }

    private List<String> strings(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (!node.isArray()) throw new IllegalArgumentException(field + " must be array");
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            if (item.isTextual() && !item.asText().isBlank()) values.add(item.asText().trim());
        });
        return List.copyOf(values);
    }

    private String stripFence(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return text;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(64);
            for (byte item : digest) hash.append(String.format("%02x", item));
            return hash.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
