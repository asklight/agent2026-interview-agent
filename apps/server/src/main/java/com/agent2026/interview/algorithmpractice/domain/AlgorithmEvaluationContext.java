package com.agent2026.interview.algorithmpractice.domain;

import java.util.List;

public record AlgorithmEvaluationContext(
        String title,
        String statement,
        List<String> constraints,
        List<String> rubric,
        AlgorithmStage stage,
        List<String> recentConversation,
        String candidateAnswer
) {
}
