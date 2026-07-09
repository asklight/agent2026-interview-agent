package com.agent2026.interview.evaluation;

import java.util.List;

public class AnswerEvaluationResult {

    private final String evaluationText;
    private final String rawModelContent;
    private final Integer score;
    private final List<String> hitPoints;
    private final List<String> missingPoints;
    private final List<String> weaknesses;
    private final String nextAction;
    private final String followUpQuestion;
    private final boolean llmUsed;
    private final boolean structured;

    public AnswerEvaluationResult(String evaluationText,
                                  String rawModelContent,
                                  Integer score,
                                  List<String> hitPoints,
                                  List<String> missingPoints,
                                  List<String> weaknesses,
                                  String nextAction,
                                  String followUpQuestion,
                                  boolean llmUsed,
                                  boolean structured) {
        this.evaluationText = evaluationText;
        this.rawModelContent = rawModelContent;
        this.score = score;
        this.hitPoints = hitPoints == null ? List.of() : List.copyOf(hitPoints);
        this.missingPoints = missingPoints == null ? List.of() : List.copyOf(missingPoints);
        this.weaknesses = weaknesses == null ? List.of() : List.copyOf(weaknesses);
        this.nextAction = nextAction;
        this.followUpQuestion = followUpQuestion;
        this.llmUsed = llmUsed;
        this.structured = structured;
    }

    public static AnswerEvaluationResult unstructured(String evaluationText, boolean llmUsed) {
        return new AnswerEvaluationResult(
                evaluationText,
                evaluationText,
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                llmUsed,
                false
        );
    }

    public String getEvaluationText() {
        return evaluationText;
    }

    public String getRawModelContent() {
        return rawModelContent;
    }

    public Integer getScore() {
        return score;
    }

    public List<String> getHitPoints() {
        return hitPoints;
    }

    public List<String> getMissingPoints() {
        return missingPoints;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public String getNextAction() {
        return nextAction;
    }

    public String getFollowUpQuestion() {
        return followUpQuestion;
    }

    public boolean isLlmUsed() {
        return llmUsed;
    }

    public boolean isStructured() {
        return structured;
    }
}
