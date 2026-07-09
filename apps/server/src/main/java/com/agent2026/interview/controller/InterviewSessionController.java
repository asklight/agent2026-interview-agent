package com.agent2026.interview.controller;

import com.agent2026.interview.common.Result;
import com.agent2026.interview.param.CreateInterviewSessionParam;
import com.agent2026.interview.param.SubmitAnswerParam;
import com.agent2026.interview.service.InterviewSessionService;
import com.agent2026.interview.vo.CurrentQuestionVO;
import com.agent2026.interview.vo.InterviewSessionVO;
import com.agent2026.interview.vo.SubmitAnswerVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interview-sessions")
public class InterviewSessionController {

    private final InterviewSessionService interviewSessionService;

    public InterviewSessionController(InterviewSessionService interviewSessionService) {
        this.interviewSessionService = interviewSessionService;
    }

    @PostMapping
    public Result<InterviewSessionVO> create(@Valid @RequestBody CreateInterviewSessionParam param) {
        return Result.success(interviewSessionService.create(param));
    }

    @GetMapping("/{sessionId}")
    public Result<InterviewSessionVO> get(@PathVariable Long sessionId) {
        return Result.success(interviewSessionService.get(sessionId));
    }

    @GetMapping("/{sessionId}/current-question")
    public Result<CurrentQuestionVO> currentQuestion(@PathVariable Long sessionId) {
        return Result.success(interviewSessionService.currentQuestion(sessionId));
    }

    @PostMapping("/{sessionId}/answers")
    public Result<SubmitAnswerVO> submitAnswer(@PathVariable Long sessionId,
                                               @Valid @RequestBody SubmitAnswerParam param) {
        return Result.success(interviewSessionService.submitAnswer(sessionId, param));
    }

    @PostMapping("/{sessionId}/next-question")
    public Result<InterviewSessionVO> nextQuestion(@PathVariable Long sessionId) {
        return Result.success(interviewSessionService.nextQuestion(sessionId));
    }

    @PostMapping("/{sessionId}/finish")
    public Result<InterviewSessionVO> finish(@PathVariable Long sessionId) {
        return Result.success(interviewSessionService.finish(sessionId));
    }
}
