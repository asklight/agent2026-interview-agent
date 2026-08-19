package com.agent2026.interview.trainingagent.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public enum AbilityDimension {
    KNOWLEDGE_JAVA("KNOWLEDGE.JAVA", "Java", "KNOWLEDGE"),
    KNOWLEDGE_MYSQL("KNOWLEDGE.MYSQL", "MySQL", "KNOWLEDGE"),
    KNOWLEDGE_REDIS("KNOWLEDGE.REDIS", "Redis", "KNOWLEDGE"),
    KNOWLEDGE_SPRING("KNOWLEDGE.SPRING", "Spring", "KNOWLEDGE"),
    KNOWLEDGE_NETWORK("KNOWLEDGE.NETWORK", "计算机网络", "KNOWLEDGE"),
    KNOWLEDGE_OS("KNOWLEDGE.OS", "操作系统", "KNOWLEDGE"),
    PROJECT_OWNERSHIP("PROJECT.OWNERSHIP", "个人贡献", "PROJECT_DEEP_DIVE"),
    PROJECT_AUTHENTICITY("PROJECT.AUTHENTICITY", "真实性", "PROJECT_DEEP_DIVE"),
    PROJECT_PRINCIPLE("PROJECT.PRINCIPLE", "技术原理", "PROJECT_DEEP_DIVE"),
    PROJECT_TRADEOFF("PROJECT.TRADEOFF", "方案取舍", "PROJECT_DEEP_DIVE"),
    ALGORITHM_CORRECTNESS("ALGORITHM.CORRECTNESS", "思路正确性", "ALGORITHM"),
    ALGORITHM_OPTIMIZATION("ALGORITHM.OPTIMIZATION", "优化推导", "ALGORITHM"),
    ALGORITHM_COMPLEXITY("ALGORITHM.COMPLEXITY", "复杂度分析", "ALGORITHM"),
    ALGORITHM_EDGE_CASE("ALGORITHM.EDGE_CASE", "边界意识", "ALGORITHM"),
    ALGORITHM_COMMUNICATION("ALGORITHM.COMMUNICATION", "算法表达结构", "ALGORITHM"),
    GENERAL_ANSWER_STRUCTURE("GENERAL.ANSWER_STRUCTURE", "表达完整性", "GENERAL"),
    GENERAL_EVIDENCE("GENERAL.EVIDENCE", "证据意识", "GENERAL");

    private static final Map<String, AbilityDimension> KNOWLEDGE_MODULES = Map.of(
            "JAVA", KNOWLEDGE_JAVA, "JAVA_CORE", KNOWLEDGE_JAVA,
            "MYSQL", KNOWLEDGE_MYSQL, "REDIS", KNOWLEDGE_REDIS,
            "SPRING", KNOWLEDGE_SPRING, "NETWORK", KNOWLEDGE_NETWORK,
            "OS", KNOWLEDGE_OS, "OPERATINGSYSTEM", KNOWLEDGE_OS,
            "COMPUTERNETWORK", KNOWLEDGE_NETWORK);

    private final String code;
    private final String label;
    private final String sourceType;

    AbilityDimension(String code, String label, String sourceType) {
        this.code = code;
        this.label = label;
        this.sourceType = sourceType;
    }

    public String code() { return code; }
    public String label() { return label; }
    public String sourceType() { return sourceType; }
    public boolean general() { return "GENERAL".equals(sourceType); }

    public boolean core() {
        return switch (this) {
            case KNOWLEDGE_JAVA, KNOWLEDGE_MYSQL,
                    PROJECT_OWNERSHIP, PROJECT_AUTHENTICITY, PROJECT_PRINCIPLE, PROJECT_TRADEOFF,
                    ALGORITHM_CORRECTNESS, ALGORITHM_COMPLEXITY -> true;
            default -> false;
        };
    }

    public static AbilityDimension fromCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equalsIgnoreCase(code)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown ability dimension: " + code));
    }

    public static AbilityDimension fromKnowledgeModule(String module) {
        if (module == null || module.isBlank()) {
            throw new IllegalArgumentException("knowledge module is missing");
        }
        String raw = module.trim().toUpperCase(Locale.ROOT);
        if (raw.contains("计算机网络") || raw.equals("网络")) return KNOWLEDGE_NETWORK;
        if (raw.contains("操作系统")) return KNOWLEDGE_OS;
        if (raw.contains("MYSQL") || raw.contains("数据库")) return KNOWLEDGE_MYSQL;
        if (raw.contains("REDIS")) return KNOWLEDGE_REDIS;
        if (raw.contains("SPRING")) return KNOWLEDGE_SPRING;
        if (raw.contains("JAVA")) return KNOWLEDGE_JAVA;
        String normalized = raw.replaceAll("[^A-Za-z]", "");
        AbilityDimension result = KNOWLEDGE_MODULES.get(normalized);
        if (result == null) throw new IllegalArgumentException("unknown knowledge module: " + module);
        return result;
    }

    public static AbilityDimension fromProjectDimension(String dimension) {
        if (dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("project dimension is missing");
        }
        return switch (dimension.replaceAll("[_-]", "").toUpperCase(Locale.ROOT)) {
            case "OWNERSHIP" -> PROJECT_OWNERSHIP;
            case "AUTHENTICITY" -> PROJECT_AUTHENTICITY;
            case "PRINCIPLE", "TECHNICALDEPTH", "ENGINEERINGAWARENESS" -> PROJECT_PRINCIPLE;
            case "TRADEOFF", "TRADEOFFREASONING" -> PROJECT_TRADEOFF;
            case "COMMUNICATION" -> GENERAL_ANSWER_STRUCTURE;
            default -> throw new IllegalArgumentException("unknown project dimension: " + dimension);
        };
    }

    public static AbilityDimension fromAlgorithmDimension(String dimension) {
        if (dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("algorithm dimension is missing");
        }
        return switch (dimension.replaceAll("[_-]", "").toLowerCase(Locale.ROOT)) {
            case "correctness", "baselinesolution" -> ALGORITHM_CORRECTNESS;
            case "optimization" -> ALGORITHM_OPTIMIZATION;
            case "complexity" -> ALGORITHM_COMPLEXITY;
            case "edgecases", "edgecase" -> ALGORITHM_EDGE_CASE;
            case "communication", "clarify", "followup" -> ALGORITHM_COMMUNICATION;
            default -> throw new IllegalArgumentException("unknown algorithm dimension: " + dimension);
        };
    }
}
