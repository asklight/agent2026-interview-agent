package com.agent2026.interview.service.impl;

import com.agent2026.interview.engine.QuestionSelector;
import com.agent2026.interview.entity.InterviewAnswer;
import com.agent2026.interview.entity.InterviewSession;
import com.agent2026.interview.entity.QuestionCard;
import com.agent2026.interview.evaluation.AnswerEvaluationResult;
import com.agent2026.interview.evaluation.AnswerEvaluator;
import com.agent2026.interview.followup.FollowUpDecider;
import com.agent2026.interview.followup.FollowUpDecision;
import com.agent2026.interview.followup.NextAction;
import com.agent2026.interview.mapper.InterviewAnswerMapper;
import com.agent2026.interview.mapper.InterviewSessionMapper;
import com.agent2026.interview.mapper.QuestionCardMapper;
import com.agent2026.interview.param.CreateInterviewSessionParam;
import com.agent2026.interview.param.SubmitAnswerParam;
import com.agent2026.interview.service.InterviewSessionService;
import com.agent2026.interview.service.InterviewReportService;
import com.agent2026.interview.vo.CurrentQuestionVO;
import com.agent2026.interview.vo.InterviewSessionVO;
import com.agent2026.interview.vo.SubmitAnswerVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InterviewSessionServiceImpl implements InterviewSessionService {

    private static final String MODE_JAVA_CORE = "JAVA_CORE";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_FINISHED = "FINISHED";
    private static final String QUESTION_TYPE_MAIN = "MAIN";
    private static final String QUESTION_TYPE_FOLLOW_UP = "FOLLOW_UP";
    private static final String QUESTION_TYPE_EVALUATED = "EVALUATED";
    private static final int DEFAULT_QUESTION_COUNT = 5;

    private final InterviewSessionMapper sessionMapper;
    private final InterviewAnswerMapper answerMapper;
    private final QuestionCardMapper questionCardMapper;
    private final QuestionSelector questionSelector;
    private final AnswerEvaluator answerEvaluator;
    private final FollowUpDecider followUpDecider;
    private final InterviewReportService interviewReportService;

    public InterviewSessionServiceImpl(InterviewSessionMapper sessionMapper,
                                       InterviewAnswerMapper answerMapper,
                                       QuestionCardMapper questionCardMapper,
                                       QuestionSelector questionSelector,
                                       AnswerEvaluator answerEvaluator,
                                       FollowUpDecider followUpDecider,
                                       InterviewReportService interviewReportService) {
        this.sessionMapper = sessionMapper;
        this.answerMapper = answerMapper;
        this.questionCardMapper = questionCardMapper;
        this.questionSelector = questionSelector;
        this.answerEvaluator = answerEvaluator;
        this.followUpDecider = followUpDecider;
        this.interviewReportService = interviewReportService;
    }

    @Override
    @Transactional
    public InterviewSessionVO create(CreateInterviewSessionParam param) {
        if (!StringUtils.hasText(param.getModule())) {
            throw new IllegalStateException("module cannot be blank for JAVA_CORE");
        }
        QuestionCard firstQuestion = questionSelector.selectFirst(param.getModule(), param.getDifficulty());

        InterviewSession session = new InterviewSession();
        session.setMode(MODE_JAVA_CORE);
        session.setModule(param.getModule());
        session.setDifficulty(param.getDifficulty());
        session.setQuestionCount(param.getQuestionCount() == null ? DEFAULT_QUESTION_COUNT : param.getQuestionCount());
        session.setCompletedQuestionCount(0);
        session.setCurrentQuestionId(firstQuestion.getId());
        session.setCurrentQuestionType(QUESTION_TYPE_MAIN);
        session.setCurrentFollowUpQuestion(null);
        session.setAskedQuestionIds(String.valueOf(firstQuestion.getId()));
        session.setStatus(STATUS_IN_PROGRESS);
        sessionMapper.insert(session);

        return toSessionVO(session, firstQuestion);
    }

    @Override
    public InterviewSessionVO get(Long sessionId) {
        InterviewSession session = requireSession(sessionId);
        QuestionCard question = session.getCurrentQuestionId() == null ? null : requireQuestion(session.getCurrentQuestionId());
        return toSessionVO(session, question);
    }

    @Override
    public CurrentQuestionVO currentQuestion(Long sessionId) {
        InterviewSession session = requireActiveSession(sessionId);
        return toQuestionVO(session, requireQuestion(session.getCurrentQuestionId()));
    }

    @Override
    @Transactional
    public SubmitAnswerVO submitAnswer(Long sessionId, SubmitAnswerParam param) {
        InterviewSession session = requireActiveSession(sessionId);
        QuestionCard question = requireQuestion(session.getCurrentQuestionId());
        String questionType = currentQuestionType(session);
        if (QUESTION_TYPE_EVALUATED.equals(questionType)) {
            throw new IllegalStateException("Current question has been evaluated, please move to next question or finish session");
        }

        String questionText = activeQuestionText(session, question);
        AnswerEvaluationResult evaluation = answerEvaluator.evaluate(question, questionText, param.getAnswerText(), questionType);
        InterviewAnswer answer = saveAnswer(session, question, questionText, param.getAnswerText(), evaluation);

        SubmitAnswerVO vo = baseSubmitAnswerVO(session, question, answer, evaluation);
        if (QUESTION_TYPE_MAIN.equals(questionType)) {
            handleMainAnswer(session, question, evaluation, vo);
        } else if (QUESTION_TYPE_FOLLOW_UP.equals(questionType)) {
            markCurrentQuestionCompleted(session);
            vo.setNextAction(nextActionAfterCompletion(session));
        } else {
            throw new IllegalStateException("Unsupported current question type: " + questionType);
        }
        vo.setCompletedQuestionCount(completedCount(session));
        vo.setQuestionCount(totalQuestionCount(session));
        return vo;
    }

    @Override
    @Transactional
    public InterviewSessionVO nextQuestion(Long sessionId) {
        InterviewSession session = requireActiveSession(sessionId);
        if (!QUESTION_TYPE_EVALUATED.equals(currentQuestionType(session))) {
            throw new IllegalStateException("Please submit the current answer before moving to next question");
        }
        if (completedCount(session) >= totalQuestionCount(session)) {
            finishSession(session);
            return toSessionVO(session, null);
        }

        Set<Long> askedQuestionIds = parseAskedQuestionIds(session.getAskedQuestionIds());
        QuestionCard nextQuestion = questionSelector.selectNext(session.getModule(), session.getDifficulty(), askedQuestionIds);
        if (nextQuestion == null) {
            finishSession(session);
            return toSessionVO(session, null);
        }

        askedQuestionIds.add(nextQuestion.getId());
        session.setCurrentQuestionId(nextQuestion.getId());
        session.setCurrentQuestionType(QUESTION_TYPE_MAIN);
        session.setCurrentFollowUpQuestion(null);
        session.setAskedQuestionIds(joinQuestionIds(askedQuestionIds));
        sessionMapper.updateById(session);
        return toSessionVO(session, nextQuestion);
    }

    @Override
    @Transactional
    public InterviewSessionVO finish(Long sessionId) {
        InterviewSession session = requireSession(sessionId);
        finishSession(session);
        return toSessionVO(session, null);
    }

    private void handleMainAnswer(InterviewSession session,
                                  QuestionCard question,
                                  AnswerEvaluationResult evaluation,
                                  SubmitAnswerVO vo) {
        FollowUpDecision decision = followUpDecider.decideAfterMainAnswer(question, evaluation);
        if (NextAction.ASK_FOLLOW_UP.equals(decision.getNextAction())) {
            session.setCurrentQuestionType(QUESTION_TYPE_FOLLOW_UP);
            session.setCurrentFollowUpQuestion(decision.getFollowUpQuestion());
            sessionMapper.updateById(session);

            vo.setNextAction(NextAction.ASK_FOLLOW_UP);
            vo.setFollowUpQuestion(decision.getFollowUpQuestion());
            vo.setCurrentQuestion(toQuestionVO(session, question));
            return;
        }

        markCurrentQuestionCompleted(session);
        vo.setNextAction(nextActionAfterCompletion(session));
    }

    private InterviewAnswer saveAnswer(InterviewSession session,
                                       QuestionCard question,
                                       String questionText,
                                       String answerText,
                                       AnswerEvaluationResult evaluation) {
        InterviewAnswer answer = new InterviewAnswer();
        answer.setSessionId(session.getId());
        answer.setQuestionCardId(question.getId());
        answer.setQuestionText(questionText);
        answer.setAnswerText(answerText);
        answer.setEvaluationText(evaluation.getEvaluationText());
        if (evaluation.getScore() != null) {
            answer.setScore(BigDecimal.valueOf(evaluation.getScore()));
        }
        answer.setTurnIndex(answerMapper.countBySessionId(session.getId()) + 1);
        answerMapper.insert(answer);
        return answer;
    }

    private SubmitAnswerVO baseSubmitAnswerVO(InterviewSession session,
                                              QuestionCard question,
                                              InterviewAnswer answer,
                                              AnswerEvaluationResult evaluation) {
        SubmitAnswerVO vo = new SubmitAnswerVO();
        vo.setAnswerId(answer.getId());
        vo.setSessionId(session.getId());
        vo.setQuestionId(question.getId());
        vo.setScore(evaluation.getScore());
        vo.setHitPoints(evaluation.getHitPoints());
        vo.setMissingPoints(evaluation.getMissingPoints());
        vo.setWeaknesses(evaluation.getWeaknesses());
        vo.setEvaluationText(evaluation.getEvaluationText());
        vo.setCompletedQuestionCount(completedCount(session));
        vo.setQuestionCount(totalQuestionCount(session));
        return vo;
    }

    private void markCurrentQuestionCompleted(InterviewSession session) {
        session.setCompletedQuestionCount(completedCount(session) + 1);
        session.setCurrentQuestionType(QUESTION_TYPE_EVALUATED);
        session.setCurrentFollowUpQuestion(null);
        if (completedCount(session) >= totalQuestionCount(session)) {
            session.setStatus(STATUS_FINISHED);
            session.setEndTime(LocalDateTime.now());
        }
        sessionMapper.updateById(session);
        if (STATUS_FINISHED.equals(session.getStatus())) {
            interviewReportService.generateIfAbsent(session.getId());
        }
    }

    private String nextActionAfterCompletion(InterviewSession session) {
        if (STATUS_FINISHED.equals(session.getStatus()) || completedCount(session) >= totalQuestionCount(session)) {
            return NextAction.FINISH_SESSION;
        }
        return NextAction.NEXT_QUESTION;
    }

    private void finishSession(InterviewSession session) {
        if (!STATUS_FINISHED.equals(session.getStatus())) {
            session.setStatus(STATUS_FINISHED);
            session.setEndTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
        interviewReportService.generateIfAbsent(session.getId());
    }

    private InterviewSession requireSession(Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalStateException("Interview session not found: " + sessionId);
        }
        return session;
    }

    private InterviewSession requireActiveSession(Long sessionId) {
        InterviewSession session = requireSession(sessionId);
        if (STATUS_FINISHED.equals(session.getStatus())) {
            throw new IllegalStateException("Interview session already finished: " + sessionId);
        }
        return session;
    }

    private QuestionCard requireQuestion(Long questionId) {
        QuestionCard question = questionCardMapper.selectById(questionId);
        if (question == null) {
            throw new IllegalStateException("Question card not found: " + questionId);
        }
        return question;
    }

    private String currentQuestionType(InterviewSession session) {
        return StringUtils.hasText(session.getCurrentQuestionType()) ? session.getCurrentQuestionType() : QUESTION_TYPE_MAIN;
    }

    private String activeQuestionText(InterviewSession session, QuestionCard question) {
        if (QUESTION_TYPE_FOLLOW_UP.equals(currentQuestionType(session))
                && StringUtils.hasText(session.getCurrentFollowUpQuestion())) {
            return session.getCurrentFollowUpQuestion();
        }
        return question.getMainQuestion();
    }

    private int completedCount(InterviewSession session) {
        return session.getCompletedQuestionCount() == null ? 0 : session.getCompletedQuestionCount();
    }

    private int totalQuestionCount(InterviewSession session) {
        return session.getQuestionCount() == null ? DEFAULT_QUESTION_COUNT : session.getQuestionCount();
    }

    private Set<Long> parseAskedQuestionIds(String askedQuestionIds) {
        if (!StringUtils.hasText(askedQuestionIds)) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(askedQuestionIds.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String joinQuestionIds(Set<Long> questionIds) {
        return questionIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private InterviewSessionVO toSessionVO(InterviewSession session, QuestionCard question) {
        InterviewSessionVO vo = new InterviewSessionVO();
        vo.setSessionId(session.getId());
        vo.setMode(session.getMode());
        vo.setModule(session.getModule());
        vo.setDifficulty(session.getDifficulty());
        vo.setQuestionCount(totalQuestionCount(session));
        vo.setCompletedQuestionCount(completedCount(session));
        vo.setStatus(session.getStatus());
        if (!STATUS_FINISHED.equals(session.getStatus()) && question != null) {
            vo.setCurrentQuestion(toQuestionVO(session, question));
        }
        return vo;
    }

    private CurrentQuestionVO toQuestionVO(InterviewSession session, QuestionCard question) {
        CurrentQuestionVO vo = new CurrentQuestionVO();
        vo.setQuestionId(question.getId());
        vo.setCardCode(question.getCardCode());
        vo.setModule(question.getModule());
        vo.setDifficulty(question.getDifficulty());
        vo.setQuestionType(currentQuestionType(session));
        vo.setQuestionText(activeQuestionText(session, question));
        vo.setMainQuestion(question.getMainQuestion());
        vo.setTags(question.getTags());
        vo.setCompletedQuestionCount(completedCount(session));
        vo.setQuestionCount(totalQuestionCount(session));
        return vo;
    }
}
