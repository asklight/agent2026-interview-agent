package com.agent2026.interview.trainingagent.infrastructure.source;

import com.agent2026.interview.trainingagent.domain.AbilityDimension;
import com.agent2026.interview.trainingagent.domain.AbilityEvidence;
import com.agent2026.interview.trainingagent.domain.EvidencePolarity;
import com.agent2026.interview.shared.training.CompletedTrainingReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TrainingEvidenceExtractor {
    private final ObjectMapper objectMapper;

    public TrainingEvidenceExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<AbilityEvidence> extract(CompletedTrainingReport source, LocalDateTime now) {
        List<AbilityEvidence> result = new ArrayList<>();
        LocalDateTime observedAt = source.observedAt() == null ? now : source.observedAt();
        JsonNode root = readTree(source.reportJson());
        if (root == null) {
            if ("KNOWLEDGE".equals(source.sourceType())) {
                extractLegacy(source, observedAt, result);
                if (result.isEmpty()) {
                    throw rejected("REPORT_EVIDENCE_EMPTY", "legacy report contains no supported evidence");
                }
                return distinct(result);
            }
            throw rejected("REPORT_JSON_INVALID", "structured report JSON is missing");
        }
        if ("ALGORITHM".equals(source.sourceType())) {
            extractDimensions(root.path("dimensions"), source, observedAt, result, true);
            extractConclusionArray(root.path("strengths"), root.path("rounds"), source, observedAt, result,
                    EvidencePolarity.STRENGTH, 3, true);
            extractConclusionArray(root.path("gaps"), root.path("rounds"), source, observedAt, result,
                    EvidencePolarity.GAP, 3, true);
            extractAlgorithmRounds(root.path("rounds"), source, observedAt, result);
        } else if ("PROJECT_DEEP_DIVE".equals(source.sourceType())) {
            extractDimensions(root.path("dimensions"), source, observedAt, result, false);
            extractConclusionArray(root.path("strengths"), root.path("rounds"), source, observedAt, result,
                    EvidencePolarity.STRENGTH, 3, false);
            extractConclusionArray(root.path("risks"), root.path("rounds"), source, observedAt, result,
                    EvidencePolarity.RISK, 4, false);
            extractConclusionArray(root.path("weaknesses"), root.path("rounds"), source, observedAt, result,
                    EvidencePolarity.GAP, 3, false);
            extractConclusionArray(root.path("recommendations"), root.path("rounds"), source, observedAt, result,
                    EvidencePolarity.GAP, 2, false);
            extractProjectRounds(root.path("rounds"), source, observedAt, result);
        } else {
            extractLegacy(source, observedAt, result);
        }
        if (result.isEmpty()) {
            throw rejected("REPORT_EVIDENCE_EMPTY", "structured report contains no supported evidence");
        }
        return distinct(result);
    }

    private void extractAlgorithmRounds(JsonNode rounds, CompletedTrainingReport source, LocalDateTime observedAt,
                                        List<AbilityEvidence> result) {
        if (!rounds.isArray()) return;
        for (JsonNode round : rounds) {
            AbilityDimension dimension = algorithmDimension(text(round, "stage"));
            Long turnId = longValue(round, "candidateTurnId");
            Long evaluationId = longValue(round, "evaluationId");
            extractRoundTexts(round.path("strengths"), source, observedAt, result, dimension,
                    EvidencePolarity.STRENGTH, 3, turnId, evaluationId, "round-strength");
            extractRoundTexts(round.path("gaps"), source, observedAt, result, dimension,
                    EvidencePolarity.GAP, 3, turnId, evaluationId, "round-gap");
            extractRoundTexts(round.path("evidence"), source, observedAt, result, dimension,
                    EvidencePolarity.STRENGTH, 2, turnId, evaluationId, "round-evidence");
            if (dimension == AbilityDimension.ALGORITHM_COMMUNICATION) {
                extractRoundTexts(round.path("strengths"), source, observedAt, result,
                        AbilityDimension.GENERAL_ANSWER_STRUCTURE, EvidencePolarity.STRENGTH, 2,
                        turnId, evaluationId, "answer-structure");
                extractRoundTexts(round.path("gaps"), source, observedAt, result,
                        AbilityDimension.GENERAL_ANSWER_STRUCTURE, EvidencePolarity.GAP, 2,
                        turnId, evaluationId, "answer-structure");
            }
        }
    }

    private void extractProjectRounds(JsonNode rounds, CompletedTrainingReport source, LocalDateTime observedAt,
                                      List<AbilityEvidence> result) {
        if (!rounds.isArray()) return;
        for (JsonNode round : rounds) {
            AbilityDimension dimension = projectDimension(text(round, "dimension"));
            Long turnId = longValue(round, "candidateTurnId");
            Long evaluationId = longValue(round, "evaluationId");
            Map<String, String> metadata = roundMetadata(round);
            extractRoundTexts(round.path("hitPoints"), source, observedAt, result, dimension,
                    EvidencePolarity.STRENGTH, 3, turnId, evaluationId, "round-hit", metadata);
            extractRoundTexts(round.path("missingPoints"), source, observedAt, result, dimension,
                    EvidencePolarity.GAP, 3, turnId, evaluationId, "round-missing", metadata);
            extractRoundTexts(round.path("riskFlags"), source, observedAt, result, dimension,
                    EvidencePolarity.RISK, 4, turnId, evaluationId, "round-risk", metadata);
            if (dimension == AbilityDimension.PROJECT_AUTHENTICITY) {
                extractRoundTexts(round.path("hitPoints"), source, observedAt, result,
                        AbilityDimension.GENERAL_EVIDENCE, EvidencePolarity.STRENGTH, 2,
                        turnId, evaluationId, "evidence-awareness-hit", metadata);
                extractRoundTexts(round.path("missingPoints"), source, observedAt, result,
                        AbilityDimension.GENERAL_EVIDENCE, EvidencePolarity.GAP, 2,
                        turnId, evaluationId, "evidence-awareness-missing", metadata);
                extractRoundTexts(round.path("riskFlags"), source, observedAt, result,
                        AbilityDimension.GENERAL_EVIDENCE, EvidencePolarity.RISK, 4,
                        turnId, evaluationId, "evidence-awareness-risk", metadata);
            }
        }
    }

    private void extractRoundTexts(JsonNode values, CompletedTrainingReport source, LocalDateTime observedAt,
                                   List<AbilityEvidence> result, AbilityDimension dimension,
                                   EvidencePolarity polarity, int severity, Long turnId, Long evaluationId,
                                   String category) {
        extractRoundTexts(values, source, observedAt, result, dimension, polarity, severity, turnId,
                evaluationId, category, Map.of());
    }

    private void extractRoundTexts(JsonNode values, CompletedTrainingReport source, LocalDateTime observedAt,
                                   List<AbilityEvidence> result, AbilityDimension dimension,
                                   EvidencePolarity polarity, int severity, Long turnId, Long evaluationId,
                                   String category, Map<String, String> baseMetadata) {
        if (!values.isArray()) return;
        for (JsonNode value : values) {
            String text = value.isTextual() ? value.asText() : text(value, "text");
            if (text == null || text.isBlank()) continue;
            Map<String, String> metadata = new java.util.LinkedHashMap<>(baseMetadata);
            metadata.put("category", category);
            add(result, source, dimension, polarity, severity, polarity == EvidencePolarity.RISK ? 0.82 : 0.75,
                    text, turnId, evaluationId, metadata, observedAt);
        }
    }

    private void extractDimensions(JsonNode dimensions, CompletedTrainingReport source, LocalDateTime observedAt,
                                   List<AbilityEvidence> result, boolean algorithm) {
        if (!dimensions.isArray()) return;
        for (JsonNode node : dimensions) {
            Integer score = integer(node.get("score"));
            if (score == null) continue;
            String name = text(node, "dimension");
            AbilityDimension dimension = algorithm ? algorithmDimension(name) : projectDimension(name);
            EvidencePolarity polarity = score >= 80 ? EvidencePolarity.STRENGTH
                    : score < 60 ? EvidencePolarity.GAP : EvidencePolarity.STRENGTH;
            int severity = score >= 80 ? 2 : score < 60 ? 3 : 1;
            String summary = score >= 80 ? "表现稳定" : score < 60 ? "表现需要补强" : "表现仍在发展";
            add(result, source, dimension, polarity, severity, 0.72,
                    "维度“" + dimension.label() + "”" + summary + "。", null, null,
                    Map.of("category", "dimension", "score", String.valueOf(score)), observedAt);
        }
    }

    private void extractConclusionArray(JsonNode values, JsonNode rounds, CompletedTrainingReport source,
                                        LocalDateTime observedAt, List<AbilityEvidence> result,
                                        EvidencePolarity polarity, int severity, boolean algorithm) {
        if (!values.isArray()) return;
        for (JsonNode node : values) {
            String text = node.isTextual() ? node.asText() : text(node, "text");
            if (text == null || text.isBlank()) continue;
            if (!node.isObject()) continue;
            String dimensionName = text(node, "dimension");
            AbilityDimension dimension;
            if (dimensionName != null && !dimensionName.isBlank()) {
                dimension = algorithm ? algorithmDimension(dimensionName) : projectDimension(dimensionName);
            } else {
                dimension = resolveReferencedDimension(node, rounds, algorithm);
                if (dimension == null) continue;
            }
            Map<String, String> metadata = roundMetadata(node);
            metadata = new java.util.LinkedHashMap<>(metadata);
            metadata.put("category", polarity.name().toLowerCase());
            add(result, source, dimension, polarity, severity, polarity == EvidencePolarity.RISK ? 0.82 : 0.75,
                    text, longValue(node, "candidateTurnId"), longValue(node, "evaluationId"),
                    metadata, observedAt);
        }
    }

    private void extractLegacy(CompletedTrainingReport source, LocalDateTime observedAt, List<AbilityEvidence> result) {
        AbilityDimension dimension = knowledgeDimension(source.module());
        split(source.strengths()).forEach(value -> add(result, source, dimension, EvidencePolarity.STRENGTH,
                3, 0.68, value, null, null, Map.of("category", "strength"), observedAt));
        split(source.weaknesses()).forEach(value -> add(result, source, dimension, EvidencePolarity.GAP,
                3, 0.68, value, null, null, Map.of("category", "weakness"), observedAt));
        split(source.recommendations()).forEach(value -> add(result, source, dimension, EvidencePolarity.GAP,
                2, 0.65, value, null, null, Map.of("category", "recommendation"), observedAt));
    }

    private void add(List<AbilityEvidence> result, CompletedTrainingReport source, AbilityDimension dimension,
                     EvidencePolarity polarity, int severity, double confidence, String rawText,
                     Long sourceTurnId, Long sourceEvaluationId, Map<String, String> metadata,
                     LocalDateTime observedAt) {
        String text = truncate(rawText);
        if (text.isBlank()) return;
        String key = sha256(source.sourceType() + ":" + source.sourceSessionId() + ":"
                + source.sourceReportVersion() + ":" + dimension.code() + ":" + polarity + ":" + text);
        Map<String, String> sourceMetadata = new java.util.LinkedHashMap<>(metadata);
        putMetadata(sourceMetadata, "module", source.module());
        putMetadata(sourceMetadata, "difficulty", source.difficulty());
        putMetadata(sourceMetadata, "tags", source.tags());
        if (source.projectProfileId() != null) {
            sourceMetadata.putIfAbsent("projectProfileId", String.valueOf(source.projectProfileId()));
        }
        result.add(new AbilityEvidence(null, source.userId(), source.sourceType(), source.sourceSessionId(),
                source.sourceReportId(), source.sourceReportVersion(), key, dimension, polarity,
                Math.max(1, Math.min(5, severity)), Math.max(0.1, Math.min(1.0, confidence)), text,
                sourceTurnId, sourceEvaluationId, sourceMetadata, observedAt));
    }

    private void putMetadata(Map<String, String> metadata, String key, String value) {
        if (value != null && !value.isBlank()) metadata.putIfAbsent(key, value.trim());
    }

    private JsonNode readTree(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node == null || !node.isObject()) throw rejected("REPORT_JSON_INVALID", "report JSON must be an object");
            return node;
        } catch (EvidenceRejectedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw rejected("REPORT_JSON_INVALID", "report JSON is invalid");
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
        return normalized.length() > 240 ? normalized.substring(0, 240) : normalized;
    }

    private AbilityDimension resolveReferencedDimension(JsonNode conclusion, JsonNode rounds, boolean algorithm) {
        if (!rounds.isArray()) return null;
        Long turnId = longValue(conclusion, "candidateTurnId");
        Long evaluationId = longValue(conclusion, "evaluationId");
        Long claimId = longValue(conclusion, "claimId");
        for (JsonNode round : rounds) {
            if (sameReference(turnId, longValue(round, "candidateTurnId"))
                    || sameReference(evaluationId, longValue(round, "evaluationId"))
                    || sameReference(claimId, longValue(round, "claimId"))) {
                return algorithm ? algorithmDimension(text(round, "stage"))
                        : projectDimension(text(round, "dimension"));
            }
        }
        return null;
    }

    private boolean sameReference(Long expected, Long actual) {
        return expected != null && expected.equals(actual);
    }

    private Map<String, String> roundMetadata(JsonNode node) {
        Long claimId = longValue(node, "claimId");
        return claimId == null ? Map.of() : Map.of("claimId", String.valueOf(claimId));
    }

    private AbilityDimension knowledgeDimension(String value) {
        try {
            return AbilityDimension.fromKnowledgeModule(value);
        } catch (IllegalArgumentException ex) {
            throw rejected("UNKNOWN_DIMENSION", ex.getMessage());
        }
    }

    private AbilityDimension projectDimension(String value) {
        try {
            return AbilityDimension.fromProjectDimension(value);
        } catch (IllegalArgumentException ex) {
            throw rejected("UNKNOWN_DIMENSION", ex.getMessage());
        }
    }

    private AbilityDimension algorithmDimension(String value) {
        try {
            return AbilityDimension.fromAlgorithmDimension(value);
        } catch (IllegalArgumentException ex) {
            throw rejected("UNKNOWN_DIMENSION", ex.getMessage());
        }
    }

    private EvidenceRejectedException rejected(String code, String message) {
        return new EvidenceRejectedException(code, message);
    }

    private List<AbilityEvidence> distinct(List<AbilityEvidence> evidence) {
        Map<String, AbilityEvidence> byKey = new LinkedHashMap<>();
        evidence.forEach(item -> byKey.putIfAbsent(item.evidenceKey(), item));
        return List.copyOf(byKey.values());
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
