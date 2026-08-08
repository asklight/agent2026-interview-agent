package com.agent2026.interview.algorithmpractice.api;

import com.agent2026.interview.algorithmpractice.application.AlgorithmApplicationService;
import com.agent2026.interview.common.Result;
import com.agent2026.interview.identity.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AlgorithmPracticeController {
    private final AlgorithmApplicationService application;
    private final CurrentUserProvider currentUser;

    public AlgorithmPracticeController(AlgorithmApplicationService application, CurrentUserProvider currentUser) {
        this.application = application;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/algorithm-problems")
    public Result<List<AlgorithmProblemResponse>> problems(
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String tag) {
        currentUser.requireUserId();
        return Result.success(application.listProblems(difficulty, tag));
    }

    @PostMapping("/api/algorithm-sessions")
    public Result<AlgorithmSessionResponse> create(@Valid @RequestBody CreateAlgorithmSessionRequest request) {
        return Result.success(application.create(currentUser.requireUserId(), request.problemId()));
    }

    @GetMapping("/api/algorithm-sessions/{sessionId}")
    public Result<AlgorithmSessionResponse> get(@PathVariable Long sessionId) {
        return Result.success(application.get(currentUser.requireUserId(), sessionId));
    }

    @PostMapping("/api/algorithm-sessions/{sessionId}/turns")
    public Result<AlgorithmSessionResponse> submit(@PathVariable Long sessionId,
                                                   @Valid @RequestBody SubmitAlgorithmTurnRequest request) {
        return Result.success(application.submit(currentUser.requireUserId(), sessionId, request));
    }

    @PostMapping("/api/algorithm-sessions/{sessionId}/turns/retry-pending")
    public Result<AlgorithmSessionResponse> retry(@PathVariable Long sessionId) {
        return Result.success(application.retryPending(currentUser.requireUserId(), sessionId));
    }

    @PostMapping("/api/algorithm-sessions/{sessionId}/finish")
    public Result<AlgorithmSessionResponse> finish(@PathVariable Long sessionId) {
        return Result.success(application.finish(currentUser.requireUserId(), sessionId));
    }

    @GetMapping("/api/algorithm-sessions/{sessionId}/report")
    public Result<AlgorithmReportResponse> report(@PathVariable Long sessionId) {
        return Result.success(application.report(currentUser.requireUserId(), sessionId));
    }
}
