package com.agent2026.interview.interviewsimulation.api;

import com.agent2026.interview.common.Result;
import com.agent2026.interview.identity.security.CurrentUserProvider;
import com.agent2026.interview.interviewsimulation.application.SimulationApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulations")
public class SimulationController {
    private final SimulationApplicationService application;
    private final CurrentUserProvider currentUser;

    public SimulationController(SimulationApplicationService application, CurrentUserProvider currentUser) {
        this.application = application;
        this.currentUser = currentUser;
    }

    @GetMapping("/options")
    public Result<SimulationOptionsResponse> options() {
        return Result.success(application.options(currentUser.requireUserId()));
    }

    @PostMapping
    public Result<SimulationResponse> create(@Valid @RequestBody CreateSimulationRequest request) {
        return Result.success(application.create(currentUser.requireUserId(), request));
    }

    @GetMapping("/{simulationId}")
    public Result<SimulationResponse> get(@PathVariable Long simulationId) {
        return Result.success(application.get(simulationId, currentUser.requireUserId()));
    }

    @PostMapping("/{simulationId}/answers")
    public Result<SimulationResponse> submit(@PathVariable Long simulationId,
                                             @Valid @RequestBody SubmitSimulationAnswerRequest request) {
        return Result.success(application.submit(simulationId, currentUser.requireUserId(), request));
    }

    @PostMapping("/{simulationId}/answers/retry-pending")
    public Result<SimulationResponse> retry(@PathVariable Long simulationId) {
        return Result.success(application.retry(simulationId, currentUser.requireUserId()));
    }

    @PostMapping("/{simulationId}/advance")
    public Result<SimulationResponse> advance(@PathVariable Long simulationId,
                                              @Valid @RequestBody SimulationVersionRequest request) {
        return Result.success(application.advance(simulationId, currentUser.requireUserId(),
                request.expectedVersion()));
    }

    @PostMapping("/{simulationId}/finish")
    public Result<SimulationResponse> finish(@PathVariable Long simulationId,
                                             @Valid @RequestBody SimulationVersionRequest request) {
        return Result.success(application.finish(simulationId, currentUser.requireUserId(),
                request.expectedVersion()));
    }

    @GetMapping("/{simulationId}/report")
    public Result<SimulationReportResponse> report(@PathVariable Long simulationId) {
        return Result.success(application.report(simulationId, currentUser.requireUserId()));
    }
}
