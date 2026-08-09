package com.agent2026.interview.trainingagent.api;

import com.agent2026.interview.common.Result;
import com.agent2026.interview.identity.security.CurrentUserProvider;
import com.agent2026.interview.trainingagent.application.TrainingAgentDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrainingAgentController {
    private final TrainingAgentDashboardService dashboard;
    private final CurrentUserProvider currentUser;

    public TrainingAgentController(TrainingAgentDashboardService dashboard, CurrentUserProvider currentUser) {
        this.dashboard = dashboard;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/training-agent/dashboard")
    public Result<TrainingAgentDashboardResponse> dashboard() {
        return Result.success(dashboard.dashboard(currentUser.requireUserId()));
    }
}
