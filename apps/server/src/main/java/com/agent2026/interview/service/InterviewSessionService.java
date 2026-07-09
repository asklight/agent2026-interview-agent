package com.agent2026.interview.service;

import com.agent2026.interview.param.CreateInterviewSessionParam;
import com.agent2026.interview.param.SubmitAnswerParam;
import com.agent2026.interview.vo.CurrentQuestionVO;
import com.agent2026.interview.vo.InterviewSessionVO;
import com.agent2026.interview.vo.SubmitAnswerVO;

public interface InterviewSessionService {

    InterviewSessionVO create(CreateInterviewSessionParam param);

    InterviewSessionVO get(Long sessionId);

    CurrentQuestionVO currentQuestion(Long sessionId);

    SubmitAnswerVO submitAnswer(Long sessionId, SubmitAnswerParam param);

    InterviewSessionVO nextQuestion(Long sessionId);

    InterviewSessionVO finish(Long sessionId);
}
