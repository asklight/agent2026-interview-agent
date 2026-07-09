package com.agent2026.interview.followup;

public class FollowUpDecision {

    private final String nextAction;
    private final String followUpQuestion;

    public FollowUpDecision(String nextAction, String followUpQuestion) {
        this.nextAction = nextAction;
        this.followUpQuestion = followUpQuestion;
    }

    public String getNextAction() {
        return nextAction;
    }

    public String getFollowUpQuestion() {
        return followUpQuestion;
    }
}
