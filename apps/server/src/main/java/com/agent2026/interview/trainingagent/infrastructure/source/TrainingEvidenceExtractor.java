package com.agent2026.interview.trainingagent.infrastructure.source;

import com.agent2026.interview.trainingagent.domain.AbilityDimension;
import com.agent2026.interview.trainingagent.domain.AbilityEvidence;
import com.agent2026.interview.trainingagent.domain.EvidencePolarity;
import com.agent2026.interview.trainingagent.infrastructure.persistence.TrainingAgentSourceRow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class TrainingEvidenceExtractor {
    private final ObjectMapper objectMapper;

    public TrainingEvidenceExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<AbilityEvidence> extract(TrainingAgentSourceRow source, LocalDateTime now) {
        List<AbilityEvidence> result = new ArrayList<>();
        LocalDateTime observedAt = source.observedAt() == null ? now : source.observedAt();
        JsonNode root = readTree(source.reportJson());
        if (root == null) {
            extractLegacy(source, observedAt, result);
            return List.copyOf(result);
        }
        if ("ALGORITHM".equals(source.sourceType())) {
            extractDimensions(root.path("dimensions"), source, observedAt, result, true);
            extractConclusionArray(root.path("strengths"), source, observedAt, result,
                    EvidencePolarity.STRENGTH, 3, "ALGORITHM", true);
            extractConclusionArray(root.path("gaps"), source, observedAt, result,
                    EvidencePolarity.GAP, 3, "ALGORITHM", true);
            extractTextArray(root.path("recommendations"), source, observedAt, result,
                    EvidencePolarity.GAP, 2, AbilityDimension.ALGORITHM_COMMUNICATION, "recommendation");
            extractAlgorithmRounds(root.path("rounds"), source, observedAt, result);
        } else if ("PROJECT_DEEP_DIVE".equals(source.sourceType())) {
            extractDimensions(root.path("dimensions"), source, observedAt, result, false);
            extractConclusionArray(root.path("strengths"), source, observedAt, result,
                    EvidencePolarity.STRENGTH, 3, "PROJECT_DEEP_DIVE", false);
            extractConclusionArray(root.path("risks"), source, observedAt, result,
                    EvidencePolarity.RISK, 4, "PROJECT_DEEP_DIVE", false);
            extractConclusionArray(root.path("weaknesses"), source, observedAt, result,
                    EvidencePolarity.GAP, 3, "PROJECT_DEEP_DIVE", false);
            extractConclusionArray(root.path("recommendations"), source, observedAt, result,
                    EvidencePolarity.GAP, 2, "PROJECT_DEEP_DIVE", false);
            extractProjectRounds(root.path("rounds"), source, observedAt, result);
        } else {
            extractLegacy(source, observedAt, result);
        }
        if (result.isEmpty()) extractLegacy(source, observedAt, result);
        return List.copyOf(result);
    }

    private void extractAlgorithmRounds(JsonNode rounds, TrainingAgentSourceRow source, LocalDateTime observedAt,
                                        List<AbilityEvidence> result) {
        if (!rounds.isArray()) return;
        for (JsonNode round : rounds) {
            AbilityDimension dimension = AbilityDimension.fromAlgorithmDimension(text(round, "stage"));
            Long turnId = longValue(round, "candidateTurnId");
            Long evaluationId = longValue(round, "evaluationId");
            extractRoundTexts(round.path("strengths"), source, observedAt, result, dimension,
                    EvidencePolarity.STRENGTH, 3, turnId, evaluationId, "round-strength");
            extractRoundTexts(round.path("gaps"), source, observedAt, result, dimension,
                    EvidencePolarity.GAP, 3, turnId, evaluationId, "round-gap");
            extractRoundTexts(round.path("evidence"), source, observedAt, result, dimension,
                    EvidencePolarity.STRENGTH, 2, turnId, evaluationId, "round-evidence");
        }
    }

    private void extractProjectRounds(JsonNode rounds, TrainingAgentSourceRow source, LocalDateTime observedAt,
                                      List<AbilityEvidence> result) {
        if (!rounds.isArray()) return;
        for (JsonNode round : rounds) {
            AbilityDimension dimension = AbilityDimension.fromProjectDimension(text(round, "dimension"));
            Long turnId = longValue(round, "candidateTurnId");
            Long evaluationId = longValue(round, "evaluationId");
            extractRoundTexts(round.path("hitPoints"), source, observedAt, result, dimension,
                    EvidencePolarity.STRENGTH, 3, turnId, evaluationId, "round-hit");
            extractRoundTexts(round.path("missingPoints"), source, observedAt, result, dimension,
                    EvidencePolarity.GAP, 3, turnId, evaluationId, "round-missing");
            extractRoundTexts(round.path("riskFlags"), source, observedAt, result, dimension,
                    EvidencePolarity.RISK, 4, turnId, evaluationId, "round-risk");
        }
    }

    private void extractRoundTexts(JsonNode values, TrainingAgentSourceRow source, LocalDateTime observedAt,
                                   List<AbilityEvidence> result, AbilityDimension dimension,
                                   EvidencePolarity polarity, int severity, Long turnId, Long evaluationId,
                                   String category) {
        if (!values.isArray()) return;
        for (JsonNode value : values) {
            String text = value.isTextual() ? value.asText() : text(value, "text");
            if (text == null || text.isBlank()) continue;
            add(result, source, dimension, polarity, severity, polarity == EvidencePolarity.RISK ? 0.82 : 0.75,
                    text, turnId, evaluationId, Map.of("category", category), observedAt);
        }
    }

    private void extractDimensions(JsonNode dimensions, TrainingAgentSourceRow source, LocalDateTime observedAt,
                                   List<AbilityEvidence> result, boolean algorithm) {
        if (!dimensions.isArray()) return;
        for (JsonNode node : dimensions) {
            Integer score = integer(node.get("score"));
            if (score == null) continue;
            String name = text(node, "dimension");
            AbilityDimension dimension = algorithm ? AbilityDimension.fromAlgorithmDimension(name)
                    : AbilityDimension.fromProjectDimension(name);
            EvidencePolarity polarity = score >= 80 ? EvidencePolarity.STRENGTH
                    : score < 60 ? EvidencePolarity.GAP : EvidencePolarity.STRENGTH;
            int severity = score >= 80 ? 2 : score < 60 ? 3 : 1;
            add(result, source, dimension, polarity, severity, 0.72,
                    "维度“" + dimension.label() + "”报告分数为 " + score + "。", null, null,
                    Map.of("category", "dimension", "score", String.valueOf(score)), observedAt);
        }
    }

    private void extractConclusionArray(JsonNode values, TrainingAgentSourceRow source, LocalDateTime observedAt,
                                        List<AbilityEvidence> result, EvidencePolarity polarity, int severity,
                                        String sourceType, boolean algorithm) {
        if (!values.isArray()) return;
        for (JsonNode node : values) {
            String text = node.isTextual() ? node.asText() : text(node, "text");
            if (text == null || text.isBlank()) continue;
            String dimensionName = node.isObject() ? text(node, "dimension") : null;
            AbilityDimension dimension = algorithm ? AbilityDimension.fromAlgorithmDimension(dimensionName)
                    : AbilityDimension.fromProjectDimension(dimensionName);
            add(result, source, dimension, polarity, severity, polarity == EvidencePolarity.RISK ? 0.82 : 0.75,
                    text, longValue(node, "candidateTurnId"), longValue(node, "evaluationId"),
                    Map.of("category", polarity.name().toLowerCase()), observedAt);
        }
    }

    private void extractTextArray(JsonNode values, TrainingAgentSourceRow source, LocalDateTime observedAt,
                                  List<AbilityEvidence> result, EvidencePolarity polarity, int severity,
                                  AbilityDimension dimension, String category) {
        if (!values.isArray()) return;
        for (JsonNode node : values) {
            String value = node.isTextual() ? node.asText() : text(node, "text");
            if (value == null || value.isBlank()) continue;
            add(result, source, dimension, polarity, severity, 0.7, value, null, null,
                    Map.of("category", category), observedAt);
        }
    }

    private void extractLegacy(TrainingAgentSourceRow source, LocalDateTime observedAt, List<AbilityEvidence> result) {
        AbilityDimension dimension = AbilityDimension.fromKnowledgeModule(source.module());
        split(source.strengths()).forEach(value -> add(result, source, dimension, EvidencePolarity.STRENGTH,
                3, 0.68, value, null, null, Map.of("category", "strength"), observedAt));
        split(source.weaknesses()).forEach(value -> add(result, source, dimension, EvidencePolarity.GAP,
                3, 0.68, value, null, null, Map.of("category", "weakness"), observedAt));
        split(source.recommendations()).forEach(value -> add(result, source, dimension, EvidencePolarity.GAP,
                2, 0.65, value, null, null, Map.of("category", "recommendation"), observedAt));
    }

    private void add(List<AbilityEvidence> result, TrainingAgentSourceRow source, AbilityDimension dimension,
                     EvidencePolarity polarity, int severity, double confidence, String rawText,
                     Long sourceTurnId, Long sourceEvaluationId, Map<String, String> metadata,
                     LocalDateTime observedAt) {
        String text = truncate(rawText);
        if (text.isBlank()) return;
        String key = sha256(source.sourceType() + ":" + source.sourceSessionId() + ":"
                + source.sourceReportVersion() + ":" + dimension.code() + ":" + polarity + ":" + text);
        result.add(new AbilityEvidence(null, source.userId(), source.sourceType(), source.sourceSessionId(),
                source.sourceReportId(), source.sourceReportVersion(), key, dimension, polarity,
                Math.max(1, Math.min(5, severity)), Math.max(0.1, Math.min(1.0, confidence)), text,
                sourceTurnId, sourceEvaluationId, metadata, observedAt));
    }

    private JsonNode readTree(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(json);
            return node != null && node.isObject() ? node : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        for (String item : value.split("[\\r\\n;；]+")) if (!item.isBlank()) result.add(item.trim());
        return result;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Integer integer(JsonNode node) {
        return node == null || node.isNull() || !node.isNumber() ? null : node.asInt();
    }

    private Long longValue(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() || !value.canConvertToLong() ? null : value.asLong();
    }

    private String truncate(String value) {
        if (value == null) return "";
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > 800 ? normalized.substring(0, 800) : normalized;
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte item : bytes) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
