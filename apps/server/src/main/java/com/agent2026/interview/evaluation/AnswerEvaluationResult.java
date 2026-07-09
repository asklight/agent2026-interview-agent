package com.agent2026.interview.evaluation;

public class AnswerEvaluationResult {

    private final String evaluationText;
    private final boolean llmUsed;

    public AnswerEvaluationResult(String evaluationText, boolean llmUsed) {
        this.evaluationText = evaluationText;
        this.llmUsed = llmUsed;
    }

    public String getEvaluationText() {
        return evaluationText;
    }

    public boolean isLlmUsed() {
        return llmUsed;
    }
}
