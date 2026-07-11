package com.agent2026.interview.projectdeepdive.interview.integration;

import com.agent2026.interview.projectdeepdive.interview.domain.TurnEvaluationResult;

public interface TurnEvaluator {
    TurnEvaluationResult evaluate(TurnEvaluationContext context);
}
