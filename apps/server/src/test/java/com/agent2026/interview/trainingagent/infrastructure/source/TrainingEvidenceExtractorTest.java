package com.agent2026.interview.trainingagent.infrastructure.source;

import com.agent2026.interview.trainingagent.domain.AbilityDimension;
import com.agent2026.interview.trainingagent.domain.AbilityEvidence;
import com.agent2026.interview.trainingagent.domain.EvidencePolarity;
import com.agent2026.interview.shared.training.CompletedTrainingReport;
import com.agent2026.interview.projectdeepdive.report.api.ProjectInterviewReportResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class TrainingEvidenceExtractorTest {
    private static final LocalDateTime OBSERVED_AT = LocalDateTime.of(2026, 8, 10, 9, 30);
    private static final LocalDateTime EXTRACTED_AT = LocalDateTime.of(2026, 8, 10, 10, 0);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TrainingEvidenceExtractor extractor = new TrainingEvidenceExtractor(objectMapper);

    @ParameterizedTest
    @MethodSource("knowledgeModules")
    void mapsLegacyKnowledgeEvidenceToItsDeclaredModule(String module, AbilityDimension expected) {
        CompletedTrainingReport source = source("KNOWLEDGE", module, null,
                "命中点：概念说明准确", "遗漏点：没有说明适用边界", "薄弱点：补充原理和使用场景");

        List<AbilityEvidence> evidence = extractor.extract(source, EXTRACTED_AT);

        assertThat(evidence).hasSize(3).allMatch(item -> item.dimension() == expected);
        assertThat(evidence).extracting(AbilityEvidence::polarity)
                .containsExactlyInAnyOrder(EvidencePolarity.STRENGTH, EvidencePolarity.GAP, EvidencePolarity.GAP);
        assertThat(evidence).extracting(item -> item.metadata().get("category"))
                .containsExactlyInAnyOrder("strength", "weakness", "recommendation");
    }

    private static Stream<Arguments> knowledgeModules() {
        return Stream.of(
                Arguments.of("Java", AbilityDimension.KNOWLEDGE_JAVA),
                Arguments.of("MySQL", AbilityDimension.KNOWLEDGE_MYSQL),
                Arguments.of("Redis", AbilityDimension.KNOWLEDGE_REDIS),
                Arguments.of("Spring", AbilityDimension.KNOWLEDGE_SPRING),
                Arguments.of("计算机网络", AbilityDimension.KNOWLEDGE_NETWORK),
                Arguments.of("操作系统", AbilityDimension.KNOWLEDGE_OS));
    }

    @Test
    void mapsProjectDimensionsRisksAndSourceReferencesWithoutGuessing() throws Exception {
        List<Map<String, Object>> rounds = List.of(
                projectRound("OWNERSHIP", 501L, 101L, 201L, "职责边界清楚", "缺少协作细节", List.of()),
                projectRound("AUTHENTICITY", 502L, 102L, 202L, "数据口径明确", "缺少原始依据", List.of("描述前后矛盾")),
                projectRound("PRINCIPLE", 503L, 103L, 203L, "原理解释准确", "缺少故障路径", List.of()),
                projectRound("TRADEOFF", 504L, 104L, 204L, "说明了方案取舍", "没有比较备选方案", List.of()));
        Map<String, Object> report = Map.of(
                "dimensions", List.of(
                        dimension("ownership", 82),
                        dimension("authenticity", 45),
                        dimension("technicalDepth", 73),
                        dimension("tradeoffReasoning", 58),
                        dimension("communication", 76)),
                // Project conclusions do not carry a dimension. The adapter must resolve it from the round reference.
                "risks", List.of(Map.of("text", "描述前后矛盾", "claimId", 502L,
                        "candidateTurnId", 102L, "evaluationId", 202L)),
                "rounds", rounds);
        CompletedTrainingReport source = source("PROJECT_DEEP_DIVE", null,
                objectMapper.writeValueAsString(report), null, null, null);

        List<AbilityEvidence> evidence = extractor.extract(source, EXTRACTED_AT);
        List<AbilityEvidence> roundEvidence = evidence.stream()
                .filter(item -> item.sourceTurnId() != null)
                .toList();

        assertThat(roundEvidence).extracting(
                        AbilityEvidence::sourceTurnId,
                        AbilityEvidence::sourceEvaluationId,
                        AbilityEvidence::dimension)
                .contains(
                        tuple(101L, 201L, AbilityDimension.PROJECT_OWNERSHIP),
                        tuple(102L, 202L, AbilityDimension.PROJECT_AUTHENTICITY),
                        tuple(103L, 203L, AbilityDimension.PROJECT_PRINCIPLE),
                        tuple(104L, 204L, AbilityDimension.PROJECT_TRADEOFF));
        assertThat(evidence).anySatisfy(item -> {
            assertThat(item.dimension()).isEqualTo(AbilityDimension.GENERAL_ANSWER_STRUCTURE);
            assertThat(item.metadata()).containsEntry("category", "dimension");
        });

        List<AbilityEvidence> risks = evidence.stream()
                .filter(item -> item.polarity() == EvidencePolarity.RISK)
                .filter(item -> item.dimension() == AbilityDimension.PROJECT_AUTHENTICITY)
                .toList();
        assertThat(risks).hasSize(1).allSatisfy(item -> {
            assertThat(item.dimension()).isEqualTo(AbilityDimension.PROJECT_AUTHENTICITY);
            assertThat(item.sourceTurnId()).isEqualTo(102L);
            assertThat(item.sourceEvaluationId()).isEqualTo(202L);
            assertThat(item.metadata()).containsEntry("claimId", "502");
        });
    }

    @Test
    void realProjectRoundReviewDerivesGeneralEvidenceOnlyFromAuthenticityFacts() throws Exception {
        ProjectInterviewReportResponse.RoundReview authenticity = new ProjectInterviewReportResponse.RoundReview(
                1, "AUTHENTICITY", "完整回答不得进入证据",
                List.of("给出了可核验时间点"), List.of("缺少监控截图来源"), List.of("指标口径前后不一致"),
                502L, 102L, 202L);
        ProjectInterviewReportResponse.RoundReview ownership = new ProjectInterviewReportResponse.RoundReview(
                2, "OWNERSHIP", "完整回答同样不得进入证据",
                List.of("职责边界清楚"), List.of("缺少协作过程"), List.of("职责描述含糊"),
                503L, 103L, 203L);
        String reportJson = objectMapper.writeValueAsString(Map.of("rounds", List.of(authenticity, ownership)));

        List<AbilityEvidence> evidence = extractor.extract(
                source("PROJECT_DEEP_DIVE", null, reportJson, null, null, null), EXTRACTED_AT);

        List<AbilityEvidence> generalEvidence = evidence.stream()
                .filter(item -> item.dimension() == AbilityDimension.GENERAL_EVIDENCE)
                .toList();
        assertThat(generalEvidence).extracting(AbilityEvidence::text, AbilityEvidence::polarity)
                .containsExactlyInAnyOrder(
                        tuple("给出了可核验时间点", EvidencePolarity.STRENGTH),
                        tuple("缺少监控截图来源", EvidencePolarity.GAP),
                        tuple("指标口径前后不一致", EvidencePolarity.RISK));
        assertThat(generalEvidence).allSatisfy(item -> {
            assertThat(item.sourceTurnId()).isEqualTo(102L);
            assertThat(item.sourceEvaluationId()).isEqualTo(202L);
            assertThat(item.metadata()).containsEntry("claimId", "502");
            assertThat(item.text()).doesNotContain("完整回答");
        });
        assertThat(generalEvidence).noneMatch(item -> item.sourceTurnId().equals(103L));
    }

    @Test
    void mapsSixAlgorithmStagesToFiveAbilityDimensions() throws Exception {
        List<Map<String, Object>> rounds = List.of(
                algorithmRound("CLARIFY", 101L, 201L),
                algorithmRound("BASELINE_SOLUTION", 102L, 202L),
                algorithmRound("OPTIMIZATION", 103L, 203L),
                algorithmRound("COMPLEXITY", 104L, 204L),
                algorithmRound("EDGE_CASE", 105L, 205L),
                algorithmRound("FOLLOW_UP", 106L, 206L));
        String reportJson = objectMapper.writeValueAsString(Map.of("rounds", rounds));

        List<AbilityEvidence> evidence = extractor.extract(
                source("ALGORITHM", null, reportJson, null, null, null), EXTRACTED_AT);

        assertThat(evidence).filteredOn(item -> "ALGORITHM".equals(item.dimension().sourceType())).extracting(
                        AbilityEvidence::sourceTurnId,
                        AbilityEvidence::sourceEvaluationId,
                        AbilityEvidence::dimension)
                .containsExactlyInAnyOrder(
                        tuple(101L, 201L, AbilityDimension.ALGORITHM_COMMUNICATION),
                        tuple(102L, 202L, AbilityDimension.ALGORITHM_CORRECTNESS),
                        tuple(103L, 203L, AbilityDimension.ALGORITHM_OPTIMIZATION),
                        tuple(104L, 204L, AbilityDimension.ALGORITHM_COMPLEXITY),
                        tuple(105L, 205L, AbilityDimension.ALGORITHM_EDGE_CASE),
                        tuple(106L, 206L, AbilityDimension.ALGORITHM_COMMUNICATION));
    }

    @ParameterizedTest
    @MethodSource("rejectedSources")
    void rejectsDamagedReportsAndUnknownDimensions(CompletedTrainingReport source, String expectedCode) {
        assertThatThrownBy(() -> extractor.extract(source, EXTRACTED_AT))
                .isInstanceOfSatisfying(EvidenceRejectedException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(expectedCode));
    }

    private static Stream<Arguments> rejectedSources() {
        return Stream.of(
                Arguments.of(source("ALGORITHM", null, "{not-json",
                        "不得回退的旧强项", null, null), "REPORT_JSON_INVALID"),
                Arguments.of(source("KNOWLEDGE", "Java", null,
                        null, null, null), "REPORT_EVIDENCE_EMPTY"),
                Arguments.of(source("KNOWLEDGE", "GraphQL", null,
                        "未知模块不能默认归到 Java", null, null), "UNKNOWN_DIMENSION"),
                Arguments.of(source("PROJECT_DEEP_DIVE", null,
                        "{\"rounds\":[{\"dimension\":\"MAGIC\",\"hitPoints\":[\"不应生成\"]}]}",
                        null, null, null), "UNKNOWN_DIMENSION"),
                Arguments.of(source("ALGORITHM", null,
                        "{\"dimensions\":[{\"dimension\":\"surprise\",\"score\":20}]}",
                        null, null, null), "UNKNOWN_DIMENSION"));
    }

    @Test
    void storesOnlyShortEvidenceSummariesAndNeverCopiesCandidateAnswer() throws Exception {
        String candidateAnswer = "PRIVATE_FULL_ANSWER_" + "a".repeat(1200);
        String longGap = "GAP_SUMMARY_" + "b".repeat(1200);
        String reportJson = objectMapper.writeValueAsString(Map.of("rounds", List.of(Map.of(
                "stage", "BASELINE_SOLUTION",
                "candidateAnswer", candidateAnswer,
                "gaps", List.of(longGap),
                "candidateTurnId", 301L,
                "evaluationId", 401L))));

        List<AbilityEvidence> evidence = extractor.extract(
                source("ALGORITHM", null, reportJson, null, null, null), EXTRACTED_AT);

        assertThat(evidence).hasSize(1);
        assertThat(evidence.get(0).text())
                .hasSizeLessThanOrEqualTo(240)
                .doesNotContain("PRIVATE_FULL_ANSWER_");
        assertThat(evidence).noneMatch(item -> item.metadata().values().stream()
                .anyMatch(value -> value.contains("PRIVATE_FULL_ANSWER_")));
    }

    @Test
    void producesStableEvidenceKeysForTheSameReportVersion() {
        CompletedTrainingReport source = source("KNOWLEDGE", "Java", null,
                " JVM   内存区域说明准确 ", "没有说明 GC 边界", null);

        List<String> firstKeys = extractor.extract(source, EXTRACTED_AT).stream()
                .map(AbilityEvidence::evidenceKey)
                .toList();
        List<String> retryKeys = extractor.extract(source, EXTRACTED_AT.plusDays(7)).stream()
                .map(AbilityEvidence::evidenceKey)
                .toList();

        assertThat(firstKeys).isNotEmpty().containsExactlyElementsOf(retryKeys);
        assertThat(firstKeys).allMatch(key -> key.matches("[0-9a-f]{64}"));
        assertThat(Set.copyOf(firstKeys)).hasSameSizeAs(firstKeys);
    }

    private static Map<String, Object> projectRound(String dimension, Long claimId, Long turnId,
                                                     Long evaluationId, String hit, String missing,
                                                     List<String> risks) {
        return Map.of(
                "dimension", dimension,
                "claimId", claimId,
                "candidateAnswer", "完整回答不得进入证据",
                "hitPoints", List.of(hit),
                "missingPoints", List.of(missing),
                "riskFlags", risks,
                "candidateTurnId", turnId,
                "evaluationId", evaluationId);
    }

    private static Map<String, Object> algorithmRound(String stage, Long turnId, Long evaluationId) {
        return Map.of(
                "stage", stage,
                "candidateAnswer", "完整回答不得进入证据",
                "gaps", List.of(stage + " 待改进"),
                "candidateTurnId", turnId,
                "evaluationId", evaluationId);
    }

    private static Map<String, Object> dimension(String dimension, int score) {
        return Map.of("dimension", dimension, "status", "ASSESSED", "score", score);
    }

    private static CompletedTrainingReport source(String sourceType, String module, String reportJson,
                                                  String strengths, String weaknesses, String recommendations) {
        return new CompletedTrainingReport(sourceType, 7L, 11L, 13L, 1, module,
                "mixed", "", 23L, reportJson, strengths, weaknesses, recommendations, OBSERVED_AT);
    }
}
