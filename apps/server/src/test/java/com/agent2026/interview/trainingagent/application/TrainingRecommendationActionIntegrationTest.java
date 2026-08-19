package com.agent2026.interview.trainingagent.application;

import com.agent2026.interview.trainingagent.domain.AbilityDimension;
import com.agent2026.interview.trainingagent.domain.AbilityEvidence;
import com.agent2026.interview.trainingagent.domain.AbilitySnapshot;
import com.agent2026.interview.trainingagent.domain.AbilityState;
import com.agent2026.interview.trainingagent.domain.EvidencePolarity;
import com.agent2026.interview.trainingagent.domain.TrainingHistorySignal;
import com.agent2026.interview.trainingagent.domain.TrainingRecommendation;
import com.agent2026.interview.trainingagent.domain.TrainingRecommendationPolicy;
import com.agent2026.interview.shared.training.CompletedTrainingReport;
import com.agent2026.interview.trainingagent.infrastructure.source.TrainingEvidenceExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingRecommendationActionIntegrationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 19, 10, 0);

    private final TrainingEvidenceExtractor extractor = new TrainingEvidenceExtractor(new ObjectMapper());
    private final TrainingRecommendationPolicy policy = new TrainingRecommendationPolicy();

    @Test
    void projectProfileIdFlowsFromSourceReportContextIntoRecommendationAction() {
        CompletedTrainingReport source = new CompletedTrainingReport(
                "PROJECT_DEEP_DIVE", 7L, 41L, 51L, 1, null, null, null, 23L,
                "{\"rounds\":[{\"dimension\":\"PRINCIPLE\",\"missingPoints\":[\"explain failure recovery\"]}]}",
                null, null, null, NOW.minusMinutes(5));
        List<AbilityEvidence> evidence = extractor.extract(source, NOW).stream()
                .filter(item -> item.polarity() == EvidencePolarity.GAP)
                .toList();

        TrainingRecommendation recommendation = policy.recommend(
                List.of(needsWork(AbilityDimension.PROJECT_PRINCIPLE)), evidence,
                new TrainingHistorySignal(List.of()), NOW);

        assertThat(evidence).isNotEmpty().allSatisfy(item ->
                assertThat(item.metadata()).containsEntry("projectProfileId", "23"));
        assertThat(recommendation.primary().trainingType()).isEqualTo("PROJECT_DEEP_DIVE");
        assertThat(recommendation.primary().action()).containsEntry("profileId", 23L);
    }

    @Test
    void algorithmDifficultyAndTagFlowFromProblemContextIntoRecommendationAction() {
        CompletedTrainingReport source = new CompletedTrainingReport(
                "ALGORITHM", 7L, 61L, 71L, 1, null, "HARD", "dynamic-programming, graph", null,
                "{\"rounds\":[{\"stage\":\"COMPLEXITY\",\"gaps\":[\"space complexity omitted\"]}]}",
                null, null, null, NOW.minusMinutes(5));
        List<AbilityEvidence> evidence = extractor.extract(source, NOW).stream()
                .filter(item -> item.polarity() == EvidencePolarity.GAP)
                .toList();

        TrainingRecommendation recommendation = policy.recommend(
                List.of(needsWork(AbilityDimension.ALGORITHM_COMPLEXITY)), evidence,
                new TrainingHistorySignal(List.of()), NOW);

        assertThat(evidence).isNotEmpty().allSatisfy(item -> assertThat(item.metadata())
                .containsEntry("difficulty", "HARD")
                .containsEntry("tags", "dynamic-programming, graph"));
        assertThat(recommendation.primary().trainingType()).isEqualTo("ALGORITHM");
        assertThat(recommendation.primary().action())
                .containsEntry("difficulty", "hard")
                .containsEntry("tag", "dynamic-programming");
    }

    private AbilitySnapshot needsWork(AbilityDimension dimension) {
        return new AbilitySnapshot(dimension, AbilityState.NEEDS_WORK,
                -4, 0.7, 0, 2, 0, 2, NOW.minusMinutes(5));
    }
}
