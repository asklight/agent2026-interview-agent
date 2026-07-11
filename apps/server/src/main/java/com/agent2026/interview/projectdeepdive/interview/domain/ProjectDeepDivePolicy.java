package com.agent2026.interview.projectdeepdive.interview.domain;

import org.springframework.stereotype.Component;

@Component
public class ProjectDeepDivePolicy {

    public String decide(String suggestedDecision, int followUpCount, int maxFollowUps,
                         int currentProbeIndex, int probeCount) {
        if (currentProbeIndex >= probeCount - 1 && !"FOLLOW_UP".equals(suggestedDecision)) {
            return "WRAP_UP";
        }
        if ("FOLLOW_UP".equals(suggestedDecision) && followUpCount < maxFollowUps) {
            return "FOLLOW_UP";
        }
        return currentProbeIndex >= probeCount - 1 ? "WRAP_UP" : "SWITCH_DIMENSION";
    }
}
