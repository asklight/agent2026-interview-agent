package com.agent2026.interview.controller;

import com.agent2026.interview.common.Result;
import com.agent2026.interview.param.CreateInterviewSessionParam;
import com.agent2026.interview.param.SubmitAnswerParam;
import com.agent2026.interview.service.InterviewSessionService;
import com.agent2026.interview.service.InterviewReportService;
import com.agent2026.interview.vo.CurrentQuestionVO;
import com.agent2026.interview.vo.InterviewSessionVO;
import com.agent2026.interview.vo.SubmitAnswerVO;
import com.agent2026.interview.vo.InterviewReportVO;
import com.agent2026.interview.projectdeepdive.interview.api.ProjectInterviewSessionResponse;
import com.agent2026.interview.projectdeepdive.interview.api.SubmitProjectTurnRequest;
import com.agent2026.interview.projectdeepdive.interview.application.DeepDiveInterviewApplicationService;
import com.agent2026.interview.projectdeepdive.report.api.ProjectInterviewReportResponse;
import com.agent2026.interview.identity.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/interview-sessions")
public class InterviewSessionController {

    private final InterviewSessionService interviewSessionService;
    private final InterviewReportService interviewReportService;
    private final DeepDiveInterviewApplicationService deepDiveInterviewService;
    private final CurrentUserProvider currentUser;

    public InterviewSessionController(InterviewSessionService interviewSessionService, InterviewReportService interviewReportService,
                                      DeepDiveInterviewApplicationService deepDiveInterviewService,
                                      CurrentUserProvider currentUser) {
        this.interviewSessionService = interviewSessionService;
        this.interviewReportService = interviewReportService;
        this.deepDiveInterviewService = deepDiveInterviewService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public Result<?> create(@Valid @RequestBody CreateInterviewSessionParam param,
                            @RequestHeader(value = "X-Resource-Token", required = false) String token) {
        Long userId = currentUser.requireUserId();
        if ("PROJECT_DEEP_DIVE".equalsIgnoreCase(param.getMode())) {
            return Result.success(deepDiveInterviewService.create(param, token));
        }
        return Result.success(interviewSessionService.create(param, userId));
    }

    @GetMapping("/{sessionId}/turns")
    public Result<ProjectInterviewSessionResponse> turns(@PathVariable Long sessionId,
            @RequestHeader(value = "X-Resource-Token", required = false) String token) {
        return Result.success(deepDiveInterviewService.getTurns(sessionId, token));
    }

    @PostMapping("/{sessionId}/turns")
    public Result<ProjectInterviewSessionResponse> submitTurn(@PathVariable Long sessionId,
            @RequestHeader(value = "X-Resource-Token", required = false) String token,
            @Valid @RequestBody SubmitProjectTurnRequest request) {
        return Result.success(deepDiveInterviewService.submit(sessionId, token, request));
    }

    @PostMapping("/{sessionId}/turns/retry-pending")
    public Result<ProjectInterviewSessionResponse> retryPendingTurn(@PathVariable Long sessionId,
            @RequestHeader(value = "X-Resource-Token", required = false) String token) {
        return Result.success(deepDiveInterviewService.retryPending(sessionId, token));
    }

    @GetMapping("/{sessionId}")
    public Result<?> get(@PathVariable Long sessionId,
                         @RequestHeader(value = "X-Resource-Token", required = false) String token) {
        if (deepDiveInterviewService.supports(sessionId)) {
            return Result.success(deepDiveInterviewService.getTurns(sessionId, token));
        }
        return Result.success(interviewSessionService.get(sessionId, currentUser.requireUserId()));
    }

    @GetMapping("/{sessionId}/current-question")
    public Result<CurrentQuestionVO> currentQuestion(@PathVariable Long sessionId,
            @RequestHeader(value = "X-Resource-Token", required = false) String token) {
        if (deepDiveInterviewService.supports(sessionId)) deepDiveInterviewService.rejectLegacyEndpoint(sessionId, token);
        return Result.success(interviewSessionService.currentQuestion(sessionId, currentUser.requireUserId()));
    }

    @PostMapping("/{sessionId}/answers")
    public Result<SubmitAnswerVO> submitAnswer(@PathVariable Long sessionId,
                                               @RequestHeader(value = "X-Resource-Token", required = false) String token,
                                               @Valid @RequestBody SubmitAnswerParam param) {
        if (deepDiveInterviewService.supports(sessionId)) deepDiveInterviewService.rejectLegacyEndpoint(sessionId, token);
        return Result.success(interviewSessionService.submitAnswer(sessionId, param, currentUser.requireUserId()));
    }

    @PostMapping("/{sessionId}/next-question")
    public Result<InterviewSessionVO> nextQuestion(@PathVariable Long sessionId,
            @RequestHeader(value = "X-Resource-Token", required = false) String token) {
        if (deepDiveInterviewService.supports(sessionId)) deepDiveInterviewService.rejectLegacyEndpoint(sessionId, token);
        return Result.success(interviewSessionService.nextQuestion(sessionId, currentUser.requireUserId()));
    }

    @PostMapping("/{sessionId}/finish")
    public Result<?> finish(@PathVariable Long sessionId,
                            @RequestHeader(value = "X-Resource-Token", required = false) String token) {
        if (deepDiveInterviewService.supports(sessionId)) {
            return Result.success(deepDiveInterviewService.finish(sessionId, token));
        }
        return Result.success(interviewSessionService.finish(sessionId, currentUser.requireUserId()));
    }

    @GetMapping("/{sessionId}/report")
    public Result<?> report(@PathVariable Long sessionId,
            @RequestHeader(value = "X-Resource-Token", required = false) String token) {
        if (deepDiveInterviewService.supports(sessionId)) {
            return Result.success(deepDiveInterviewService.getReport(sessionId, token));
        }
        interviewSessionService.get(sessionId, currentUser.requireUserId());
        return Result.success(interviewReportService.get(sessionId));
    }
}
