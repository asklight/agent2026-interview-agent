package com.agent2026.interview.service;

import com.agent2026.interview.param.CreateInterviewSessionParam;
import com.agent2026.interview.param.SubmitAnswerParam;
import com.agent2026.interview.vo.CurrentQuestionVO;
import com.agent2026.interview.vo.InterviewSessionVO;
import com.agent2026.interview.vo.SubmitAnswerVO;

public interface InterviewSessionService {

    InterviewSessionVO create(CreateInterviewSessionParam param);
    InterviewSessionVO create(CreateInterviewSessionParam param, Long userId);

    InterviewSessionVO get(Long sessionId);
    InterviewSessionVO get(Long sessionId, Long userId);

    CurrentQuestionVO currentQuestion(Long sessionId);
    CurrentQuestionVO currentQuestion(Long sessionId, Long userId);

    SubmitAnswerVO submitAnswer(Long sessionId, SubmitAnswerParam param);
    SubmitAnswerVO submitAnswer(Long sessionId, SubmitAnswerParam param, Long userId);

    InterviewSessionVO nextQuestion(Long sessionId);
    InterviewSessionVO nextQuestion(Long sessionId, Long userId);

    InterviewSessionVO finish(Long sessionId);
    InterviewSessionVO finish(Long sessionId, Long userId);
}
