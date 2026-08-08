package com.agent2026.interview.algorithmpractice.domain;

public enum AlgorithmStage {
    CLARIFY,
    BASELINE_SOLUTION,
    OPTIMIZATION,
    COMPLEXITY,
    EDGE_CASE,
    FOLLOW_UP,
    FINISHED;

    public AlgorithmStage next() {
        return switch (this) {
            case CLARIFY -> BASELINE_SOLUTION;
            case BASELINE_SOLUTION -> OPTIMIZATION;
            case OPTIMIZATION -> COMPLEXITY;
            case COMPLEXITY -> EDGE_CASE;
            case EDGE_CASE -> FOLLOW_UP;
            case FOLLOW_UP, FINISHED -> FINISHED;
        };
    }

    public String interviewerPrompt(String suggestedFollowUp) {
        return switch (this) {
            case CLARIFY -> "先不用急着给最终答案。请复述题意，并说明需要确认的输入约束和边界。";
            case BASELINE_SOLUTION -> "请先给出一个直接可行的基础方案，说明核心数据结构和执行步骤。";
            case OPTIMIZATION -> "这个方案还可以怎样优化？请说明优化依据，以及为什么它能降低开销。";
            case COMPLEXITY -> "请分析当前最优方案的时间复杂度和空间复杂度，并解释最坏情况。";
            case EDGE_CASE -> "请列出最容易遗漏的边界条件，并说明你的方案如何处理它们。";
            case FOLLOW_UP -> suggestedFollowUp == null || suggestedFollowUp.isBlank()
                    ? "如果输入规模扩大或约束变化，你会怎样调整方案？"
                    : suggestedFollowUp.trim();
            case FINISHED -> "好的，这道题的口述到这里结束。你可以查看完整复盘。";
        };
    }
}
